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

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceDAO
    extends HibernateDAO<Service> {
    private ConfigResponseDAO configResponseDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private VirtualDAO virtualDAO;

    @Autowired
    public ServiceDAO(SessionFactory f, ConfigResponseDAO configResponseDAO,
                      ServiceTypeDAO serviceTypeDAO, VirtualDAO virtualDAO) {
        super(Service.class, f);
        this.configResponseDAO = configResponseDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.virtualDAO = virtualDAO;
    }

    public Service findById(Integer id) {
        return (Service) super.findById(id);
    }

    public Service get(Integer id) {
        return (Service) super.get(id);
    }

    public void save(Service entity) {
        super.save(entity);
    }

    public void remove(Service entity) {
        super.remove(entity);
    }

    /**
     * NOTE: this method automatically sets the autoinventoryIdentifier = name
     */
    public Service create(ServiceType type, Server server, String name, String desc,
                          String modifiedBy, String location, String owner, Service parent) {
        ConfigResponseDB configResponse = configResponseDAO.create();

        Service s = new Service();
        s.setName(name);
        s.setAutoinventoryIdentifier(name);
        s.setAutodiscoveryZombie(false);
        s.setServiceRt(false);
        s.setEndUserRt(false);
        s.setDescription(desc);
        s.setModifiedBy(modifiedBy);
        s.setLocation(location);
        s.setParentService(parent);
        s.setServiceType(type);
        s.setServer(server);
        s.setConfigResponse(configResponse);
        save(s);

        server.addService(s);

        return s;
    }

    public Service create(ServiceValue sv, Server parent) {
        ConfigResponseDB configResponse = configResponseDAO.create();

        Service s = new Service();
        s.setName(sv.getName());
        s.setAutodiscoveryZombie(false);
        s.setServiceRt(false);
        s.setEndUserRt(false);
        s.setDescription(sv.getDescription());
        s.setModifiedBy(sv.getModifiedBy());
        s.setLocation(sv.getLocation());
        s.setParentId(sv.getParentId());

        if (sv.getServiceType() != null) {
            Integer stId = sv.getServiceType().getId();
            ServiceType st = serviceTypeDAO.findById(stId);
            s.setServiceType(st);
        }

        s.setServer(parent);
        s.setConfigResponse(configResponse);
        save(s);
        return s;
    }

    public Service createService(Server s, ServiceValue sv) throws ValidationException {
        // validate the service
        s.validateNewService(sv);
        // get the Service home
        return create(sv, s);
    }

    public Collection findByParent(Integer parentId) {
        String sql = "from Service where parentService.id=?";
        return createQuery(sql).setInteger(0, parentId.intValue()).list();
    }

    public Collection<Service> findByParentAndType(Integer parentId, Integer typeId) {
        String sql = "from Service where parentService.id=? and serviceType.id=?";
        return createQuery(sql).setInteger(0, parentId.intValue()).setInteger(1, typeId.intValue())
            .list();
    }

    /**
     * @param serviceIds - {@link List} of {@link Integer}
     * @return {@link Collection} of {@link ServiceType}
     */
    Collection<ServiceType> getServiceTypes(final List serviceIds, final boolean asc) {
        final String hql = new StringBuilder().append("SELECT distinct s.serviceType").append(
            " FROM Service s").append(" WHERE s.id in (:svcs)").toString();
        final HQDialect dialect = getHQDialect();
        // can't go over 1000 due to the hibernate bug
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        final int max = dialect.getMaxExpressions();
        final int maxExprs = (max == -1 || max > 1000) ? 1000 : max;
        // need a Set to quickly ensure ServiceTypes are unique btwn queries
        final Set set = new HashSet();
        for (int i = 0; i < serviceIds.size(); i += maxExprs) {
            final int last = Math.min(i + maxExprs, serviceIds.size());
            final List sublist = serviceIds.subList(i, last);
            final List list = getSession().createQuery(hql).setParameterList("svcs", sublist,
                new IntegerType()).list();
            // ServiceType hashCode is by name
            set.addAll(list);
        }
        final List rtn = new ArrayList(set);
        Collections.sort(rtn, new AppdefNameComparator(asc));
        return rtn;
    }

    public Collection findAll_orderName(boolean asc) {
        return getSession().createQuery(
            "from Service order by resource.sortName " + (asc ? "asc" : "desc")).setCacheable(true)
            .setCacheRegion("Service.findAll_orderName").list();
    }

    public Collection findAll_orderCtime(boolean asc) {
        return createQuery("from Service order by creationTime " + (asc ? "asc" : "desc")).list();
    }

    public Collection<Service> findByType(Integer st, boolean asc) {
        String sql = "from Service where serviceType.id=? " + "order by resource.sortName " +
                     (asc ? "asc" : "desc");
        return createQuery(sql).setInteger(0, st.intValue()).list();
    }

    public List findByName(String name) {
        String sql = "from Service where resource.sortName=?";
        return createQuery(sql).setString(0, name.toUpperCase()).list();
    }

    public Service findByName(Platform platform, String serviceName) {
        String sql = "select v from Service v join v.server s "
                     + "where s.platform = :platform and " + "v.resource.sortName = :name";

        return (Service) getSession().createQuery(sql).setParameter("platform", platform)
            .setParameter("name", serviceName.toUpperCase()).uniqueResult();
    }

    public Service findByName(Server server, String serviceName) {
        String sql = "select v from Service v "
                     + "where v.server = :server and v.resource.sortName = :name";

        return (Service) createQuery(sql).setParameter("server", server).setParameter("name",
            serviceName.toUpperCase()).uniqueResult();
    }

    /**
     * @return {@link List} of {@link Service}
     */
    public List<Service> getByAIID(Server server, String aiid) {
        final String sql = new StringBuilder().append("select s from Service s").append(
            " WHERE s.server = :server").append(" AND s.autoinventoryIdentifier = :aiid")
            .toString();
        return createQuery(sql).setParameter("server", server).setParameter("aiid", aiid).list();
    }

    public Collection<Service> findByPlatform_orderName(Integer id, boolean asc) {
        String sql = "select sv from Service sv " + " join fetch sv.server s " +
                     " join fetch s.platform p " + "where p.id=?" +
                     "order by s.resource.sortName " + (asc ? "asc" : "desc");
        return createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public Collection<Service> findByPlatform_orderType(Integer id, boolean asc) {
        String sql = "select sv from Service sv " + " join fetch sv.server s " +
                     " join fetch s.serverType st " + " join fetch s.platform p " + "where p.id=?" +
                     "order by st.sortName " + (asc ? "asc" : "desc") + ", s.sortName";
        return createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public List<Service> findPlatformServicesByType(Platform p, ServiceType st) {
        String sql = "select v from Service v " + " join v.server s " + " join s.platform p "
                     + " where " + "     p = :platform " + " and v.serviceType = :serviceType "
                     + " order by v.resource.sortName";

        return createQuery(sql).setParameter("platform", p).setParameter("serviceType", st).list();
    }

    public Collection<Service> findPlatformServices_orderName(Integer platId, boolean asc) {
        String sql = "select sv from Service sv " + " join fetch sv.server s " +
                     " join fetch s.serverType st " + " join fetch s.platform p " +
                     "where p.id=? " + " and st.virtual=? " + "order by sv.resource.sortName " +
                     (asc ? "asc" : "desc");
        return createQuery(sql).setInteger(0, platId.intValue()).setBoolean(1, true)
            .setCacheRegion("Service.findPlatformServices").setCacheable(true).list();
    }

    public List<Service> findByServer_orderName(Integer id) {
        String sql = "from Service where server.id=? order by resource.sortName";
        return createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public List<Service> findByServer_orderType(Integer id) {
        String sql = "select s from Service s " + " join fetch s.serviceType st "
                     + "where s.server.id=? " + "order by st.sortName";
        return createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public List<Service> findByServerAndType_orderName(Integer id, Integer tid) {
        String sql = "from Service where server.id=? and serviceType.id=? "
                     + "order by resource.sortName";
        return createQuery(sql).setInteger(0, id.intValue()).setInteger(1, tid.intValue())
            .setCacheable(true).setCacheRegion("Service.findByServerAndType").list();
    }

    public Service findByApplication(Integer appId) {
        String sql = "select s from Service s " + " join fetch s.appServices a "
                     + "where a.application.id=? ";
        return (Service) createQuery(sql).setInteger(0, appId.intValue()).uniqueResult();
    }

    public Collection findAllClusterUnassigned_orderName(boolean asc) {
        String sql = "from Service where serviceCluster is null " + "order by resource.sortName " +
                     (asc ? "asc" : "desc");
        return createQuery(sql).list();
    }

    public Collection<Service> findAllClusterAppUnassigned_orderName(boolean asc) {
        String sql = "from Service where serviceCluster is null and " + "appServices.size=0 " +
                     "order by resource.sortName " + (asc ? "asc" : "desc");
        return createQuery(sql).list();
    }

    public Resource findVirtualByInstanceId(Integer id) {

        return virtualDAO.findVirtualByInstanceId(id, AuthzConstants.serviceResType);
    }

    public Collection<Service> findVirtualByProcessId(Integer id) {

        Collection resources = virtualDAO.findVirtualByProcessId(id, AuthzConstants.serviceResType);
        List services = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            services.add(findById(virt.getId()));
        }
        return services;
    }

    public Collection<Service> findVirtualByPysicalId(Integer id) {

        Collection resources = virtualDAO.findVirtualByPysicalId(id, AuthzConstants.serviceResType);
        List services = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            services.add(findById(virt.getId()));
        }
        return services;
    }

    public List<Object[]> getServiceTypeCounts() {
        String sql = "select t.name, count(*) from ServiceType t, "
                     + "Service s where s.serviceType = t " + "group by t.name order by t.name";

        return createQuery(sql).list();
    }

    public Number getServiceCount() {
        return (Number) createQuery("select count(*) from Service").uniqueResult();
    }

    public Collection<Service> findDeletedServices() {
        String hql = "from Service where resource.resourceType = null";
        return createQuery(hql).list();
    }

    public Service findByResource(Resource res) {
        return (Service) createCriteria().add(Restrictions.eq("resource", res)).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<Service> getOrphanedServices() {
        final String hql = "from Service where resource is null";
        return createQuery(hql).list();
    }
}
