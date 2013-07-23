/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ServerDAO
    extends HibernateDAO<Server> {
    private ConfigResponseDAO configResponseDAO;
    private ServerTypeDAO serverTypeDAO;
    private VirtualDAO virtualDAO;

    @Autowired
    public ServerDAO(SessionFactory f, ConfigResponseDAO configResponseDAO,
                     ServerTypeDAO serverTypeDAO, VirtualDAO virtualDAO) {
        super(Server.class, f);
        this.configResponseDAO = configResponseDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.virtualDAO = virtualDAO;
    }

    public Server findById(Integer id) {
        return (Server) super.findById(id);
    }

    public Server get(Integer id) {
        return (Server) super.get(id);
    }

    public void save(Server entity) {
        super.save(entity);
    }

    public void remove(Server entity) {
        super.remove(entity);
    }

    public void create(Server s) {
        save(s);
    }

    public Server create(ServerValue sv, Platform p) {
        ConfigResponseDB configResponse = configResponseDAO.create();

        Server s = new Server();
        s.setName(sv.getName());
        s.setDescription(sv.getDescription());
        s.setInstallPath(sv.getInstallPath());
        String aiid = sv.getAutoinventoryIdentifier();
        if (aiid != null) {
            s.setAutoinventoryIdentifier(sv.getAutoinventoryIdentifier());
        } else {
            // Server was created by hand, use a generated AIID. (This matches
            // the behaviour in 2.7 and prior)
            aiid = sv.getInstallPath() + "_" + System.currentTimeMillis() + "_" + sv.getName();
            s.setAutoinventoryIdentifier(aiid);
        }

        s.setServicesAutomanaged(sv.getServicesAutomanaged());
        s.setRuntimeAutodiscovery(sv.getRuntimeAutodiscovery());
        s.setWasAutodiscovered(sv.getWasAutodiscovered());
        s.setAutodiscoveryZombie(false);
        s.setLocation(sv.getLocation());
        s.setModifiedBy(sv.getModifiedBy());
        s.setConfigResponse(configResponse);
        s.setPlatform(p);

        Integer stid = sv.getServerType().getId();
        ServerType st = serverTypeDAO.findById(stid);
        s.setServerType(st);
        save(s);
        return s;
    }

    Server findServerByAIID(Platform platform, String autoinventoryID) {
        String sql = "from Server where autoinventoryIdentifier = :aiid"
                     + " and platform = :platform";
        return (Server) getSession().createQuery(sql).setEntity("platform", platform).setString(
            "aiid", autoinventoryID).uniqueResult();
    }

    public Collection findAll_orderName(boolean asc) {
        String sql = "from Server s join fetch s.serverType st " + "where st.virtual=false " +
                     "order by s.resource.sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setCacheable(true).setCacheRegion(
            "Server.findAll_orderName").list();
    }

    /**
     * @param serverIds - {@link List} of {@link Integer}
     * @return {@link Collection} of {@link ServerType}
     */
    Collection<ServerType> getServerTypes(final List<Integer> serverIds, final boolean asc) {
        final String hql = new StringBuilder().append("SELECT distinct s.serverType").append(
            " FROM Server s").append(" WHERE s.id in (:svrs)").toString();
        final HQDialect dialect = getHQDialect();
        // can't go over 1000 due to the hibernate bug
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        final int max = dialect.getMaxExpressions();
        final int maxExprs = (max == -1 || max > 1000) ? 1000 : max;
        // need a Set to quickly ensure ServiceTypes are unique btwn queries
        final Set set = new HashSet();
        for (int i = 0; i < serverIds.size(); i += maxExprs) {
            final int last = Math.min(i + maxExprs, serverIds.size());
            final List<Integer> sublist = serverIds.subList(i, last);
            final List list = getSession().createQuery(hql).setParameterList("svrs", sublist,
                new IntegerType()).list();
            // ServerType hashCode is by name
            set.addAll(list);
        }
        final List rtn = new ArrayList(set);
        Collections.sort(rtn, new AppdefNameComparator(asc));
        return rtn;
    }

    public Collection<Server> findByType(Integer sTypeId) {
        String sql = "from Server where serverType.id=?";
        return getSession().createQuery(sql).setInteger(0, sTypeId.intValue()).list();
    }

    public List<Server> findByPlatform_orderName(Integer id) {
        String sql = "from Server where platform.id=? " + "order by resource.sortName";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public List<Server> findByPlatform_orderName(Integer id, Boolean virtual) {
        String sql = "from Server where platform.id=? and " + "serverType.virtual=? "
                     + "order by resource.sortName";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).setBoolean(1,
            virtual.booleanValue()).setCacheable(true).setCacheRegion(
            "Server.findByPlatform_orderName").list();
    }

    public List<Server> findByPlatformAndType_orderName(Integer id, Integer tid) {
        String sql = "from Server where platform.id=? and " + "serverType.id=? "
                     + "order by resource.sortName";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).setInteger(1,
            tid.intValue()).list();
    }

    public List<Server> findByPlatformAndType_orderName(Integer id, Integer tid, Boolean isVirtual) {
        String sql = "select s from Server s join s.serverType st " + "where s.platform.id=? and "
                     + "st.id=? and " + "st.virtual=? " + "order by s.resource.sortName";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).setInteger(1,
            tid.intValue()).setBoolean(2, isVirtual.booleanValue()).setCacheable(true)
            .setCacheRegion("Server.findByPlatformAndType_orderName").list();
    }

    public List findByServices(Integer[] ids) {
        return createCriteria().createAlias("services", "s").add(Expression.in("s.id", ids))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    public Server findByName(Platform plat, String name) {
        String sql = "select s from Server s "
                     + "where s.platform = :plat and s.resource.sortName=:name";

        return (Server) getSession().createQuery(sql).setParameter("plat", plat).setParameter(
            "name", name.toUpperCase()).uniqueResult();
    }

    public List findByName(String name) {
        String sql = "from Server where resource.sortName=?";
        return getSession().createQuery(sql).setString(0, name.toUpperCase()).list();
    }

    public Resource findVirtualByInstanceId(Integer id) {

        return virtualDAO.findVirtualByInstanceId(id, AuthzConstants.serverResType);
    }

    public Collection<Server> findVirtualByProcessId(Integer id) {

        Collection resources = virtualDAO.findVirtualByProcessId(id, AuthzConstants.serverResType);
        List servers = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            servers.add(findById(virt.getId()));
        }
        return servers;
    }

    public Collection<Server> findVirtualByPysicalId(Integer id) {

        Collection resources = virtualDAO.findVirtualByPysicalId(id, AuthzConstants.serverResType);
        List servers = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            servers.add(findById(virt.getId()));
        }
        return servers;
    }

    public List<Object[]> getServerTypeCounts() {
        String sql = "select t.name, count(*) from ServerType t, "
                     + "Server s where s.serverType = t " + "group by t.name order by t.name";

        return getSession().createQuery(sql).list();
    }

    public Number getServerCount() {
        String sql = "select count(*) from Server s join s.serverType st "
                     + "where st.virtual=false";
        return (Number) getSession().createQuery(sql).uniqueResult();
    }

    public Collection<Server> findDeletedServers() {
        String hql = "from Server where resource.resourceType = null";
        return createQuery(hql).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Server> getOrphanedServers() {
        String hql = "from Server where resource is null";
        return createQuery(hql).list();
    }

    public Collection<Server> getRemovableServers() {
        String hql = "select s from Server s " + 
                        "join s.resource r " +
                        "join r.prototype p " + 
                        "where p.removable = true";
        return createQuery(hql).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Server> getServers(Collection<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyList();
        }
        String hql = "select s from Server s where s.resource in (:resources)";
        Query q = createQuery(hql).setParameterList("resources",resources);
        return q.list();
    }
}
