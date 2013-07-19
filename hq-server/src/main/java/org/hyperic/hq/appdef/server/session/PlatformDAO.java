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
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformDAO
    extends HibernateDAO<Platform> {

    private final VirtualDAO virtualDAO;

    @Autowired
    public PlatformDAO(SessionFactory f, VirtualDAO virtualDAO) {
        super(Platform.class, f);
        this.virtualDAO = virtualDAO;
    }

    public Platform findById(Integer id) {
        return super.findById(id);
    }

    public Platform get(Integer id) {
        return super.get(id);
    }

    @Override
    public void save(Platform entity) {
        super.save(entity);
        getSession().flush();
    }

    public void remove(Ip ip) {
        getSession().delete(ip);
    }

    /**
     * A method to update a platform based on a PlatformValue object Ideally,
     * this should be done via the xdoclet generated setPlatformValue method,
     * however, since this method is generated incorrectly, and doesnt support
     * CMR's reliably, I'm rolling my own here. IMPORTANT: due to a bug in the
     * value objects, this method expects any IP's you wish to save (even
     * existing ones) to be inside the "addedIpValues" collection. This means
     * you should removeAllIpValues(), then add them individually. This is a
     * workaround until the xdoclet stuff is made to work.
     * 
     * Legacy code from entity bean. All this logic should move close to the
     * modification source. Should pass the pojo directly instead of using
     * Platform Value object.
     * 
     * @param existing - a platform value object.
     */
    public void updatePlatform(Platform platform, PlatformValue existing) {
        // retrieve current list of ips
        Collection curips = platform.getIps();
        if (curips == null) {
            curips = new ArrayList();
            platform.setIps(curips);
        }

        // first remove any which were in the removedIp collection
        for (Object element : existing.getRemovedIpValues()) {
            IpValue aIp = (IpValue) element;
            if (aIp.idHasBeenSet()) {
                removeAIp(curips, aIp);
            }
        }
        Collection ips = existing.getAddedIpValues();
        // now get any ips which were in the ipValues array
        for (int i = 0; i < existing.getIpValues().length; i++) {
            IpValue aIp = existing.getIpValues()[i];
            if (!(ips.contains(aIp))) {
                ips.add(aIp);
            }
        }
        for (Iterator i = ips.iterator(); i.hasNext();) {
            IpValue aIp = (IpValue) i.next();
            if (aIp.idHasBeenSet()) {

                updateAIp(curips, aIp);
            } else {
                // looks like its a new one
                Ip nip = new Ip();
                nip.setIpValue(aIp);
                nip.setPlatform(platform);
                curips.add(nip);
            }
        }
        // finally update the platform
        platform.setPlatformValue(existing);

        // if there is a agent
        if (existing.getAgent() != null) {
            // get the agent token and set the agent to the platform
            platform.setAgent(existing.getAgent());
        }
        save(platform);
        // it is a good idea to
        // flush the Session here
        getSession().flush();
    }

    private void removeAIp(Collection coll, IpValue ipv) {
        for (Iterator i = coll.iterator(); i.hasNext();) {
            Ip ip = (Ip) i.next();
            if (ip.getId().equals(ipv.getId())) {
                i.remove();
                remove(ip);
                return;
            }
        }
    }

    private void updateAIp(Collection coll, IpValue ipv) {
        for (Iterator i = coll.iterator(); i.hasNext();) {
            Ip ip = (Ip) i.next();
            if (ip.getId().equals(ipv.getId())) {
                ip.setIpValue(ipv);
                return;
            }
        }
    }

    public Platform findByFQDN(String fqdn) {
        String sql = "from Platform where lower(fqdn)=?";
        return (Platform) getSession().createQuery(sql).setString(0, fqdn.toLowerCase())
            .uniqueResult();
    }

    public Collection findByNameOrFQDN(String name, String fqdn) {
        String sql = "from Platform where resource.sortName=? or lower(fqdn)=?";
        return getSession().createQuery(sql).setString(0, name.toUpperCase()).setString(1,
            fqdn.toLowerCase()).list();
    }

    public Collection<Platform> findAll_orderName(boolean asc) {
        return createCriteria().createAlias("resource", "r").addOrder(
            asc ? Order.asc("r.sortName") : Order.desc("r.sortName")).setCacheable(true)
            .setCacheRegion("Platform.findAll_orderName").list();
    }

    public Collection<Platform> findAll_orderCTime(boolean asc) {
        return createCriteria().addOrder(
            asc ? Order.asc("creationTime") : Order.desc("creationTime")).list();
    }

    public Collection<Platform> findByCTime(long ctime) {
        return createCriteria().add(Restrictions.gt("creationTime", new Long(ctime))).addOrder(
            Order.desc("creationTime")).list();
    }

    public Platform findByResourceId(int id) {
        String sql = "from Platform where resource.id=?";
        return (Platform) getSession().createQuery(sql).setInteger(0, id).uniqueResult();
    }

    public Platform findByName(String name) {
        String sql = "from Platform where resource.name=?";
        return (Platform) getSession().createQuery(sql).setString(0, name).uniqueResult();
    }

    public Platform findBySortName(String name) {
        String sql = "from Platform where resource.sortName=?";
        return (Platform) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public List<Platform> findByTypeAndRegEx(Integer pType, String regex) {
        HQDialect dialect = getHQDialect();
        String fqdnEx = dialect.getRegExSQL("p.fqdn", ":regex", true, false);
        String nameEx = dialect.getRegExSQL("rez.sort_name", ":regex", true, false);
        String sql = "select {p.*} from EAM_PLATFORM p" +
                     " JOIN EAM_RESOURCE rez on p.resource_id = rez.id" +
                     " WHERE p.platform_type_id = :id" + " AND (" + fqdnEx + " OR " + nameEx + ")";
        return getSession().createSQLQuery(sql).addEntity("p", Platform.class).setInteger("id",
            pType.intValue()).setString("regex", regex).list();
    }

    // TODO: G (this and other methods in this class)
    public List<Platform> findParentByNetworkRelation(List platformTypeIds, String platformName,
                                                      Boolean hasChildren) {
        String nameEx = null;
        StringBuffer sql = new StringBuffer("select {p.*} from EAM_PLATFORM p ");

        sql.append("join EAM_RESOURCE r on p.resource_id = r.id ");

        StringBuffer whereClause = new StringBuffer();

        if (hasChildren != null) {
            whereClause.append((hasChildren.booleanValue() ? "" : "not")).append(
                " exists (select id from EAM_RESOURCE_EDGE e ").append(
                " where p.resource_id = e.from_id ").append(" and e.rel_id = ").append(
                AuthzConstants.RELATION_NETWORK_ID).append(" and e.distance = 0) ");
        }

        if ((platformTypeIds != null) && !platformTypeIds.isEmpty()) {
            whereClause.append((whereClause.length() > 0) ? " and" : "").append(
                " p.platform_type_id in (:ids) ");
        }

        if ((platformName != null) && (platformName.trim().length() > 0)) {
            HQDialect dialect = getHQDialect();
            nameEx = dialect.getRegExSQL("r.sort_name", ":regex", true, false);

            whereClause.append((whereClause.length() > 0) ? " and" : "").append(" (")
                .append(nameEx).append(") ");
        }

        if (whereClause.length() > 0) {
            sql.append("where ").append(whereClause);
        }

        sql.append(" order by r.sort_name ");

        Query query = getSession().createSQLQuery(sql.toString()).addEntity("p", Platform.class);

        if ((platformTypeIds != null) && !platformTypeIds.isEmpty()) {
            query.setParameterList("ids", platformTypeIds, new IntegerType());
        }

        if (nameEx != null) {
            query.setString("regex", platformName);
        }

        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Platform> findByNoNetworkRelation(List<Integer> platformTypeIds, String platformName) {
        String nameEx = null;
        String sql = "select {p.*} from EAM_PLATFORM p " +
                     "join EAM_RESOURCE r on p.resource_id = r.id " +
                     "where p.platform_type_id in (:ids) " + "and not exists (" +
                     " select from_id from EAM_RESOURCE_EDGE e " + " where e.rel_id = " +
                     AuthzConstants.RELATION_NETWORK_ID + " and e.to_id = p.resource_id ) ";

        if ((platformName != null) && (platformName.trim().length() > 0)) {
            HQDialect dialect = getHQDialect();
            nameEx = dialect.getRegExSQL("r.sort_name", ":regex", true, false);
            String fqdnEx = dialect.getRegExSQL("p.fqdn", ":regex", true, false);

            sql += " and (" + fqdnEx + " or " + nameEx + ") ";
        }

        Query query = getSession().createSQLQuery(sql).addEntity("p", Platform.class)
            .setParameterList("ids", platformTypeIds, new IntegerType());

        if (nameEx != null) {
            query.setString("regex", platformName);
        }

        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Platform> findByType(Integer pid) {
        String sql = "select distinct p from Platform p " + "where p.platformType.id=?";
        return getSession().createQuery(sql).setInteger(0, pid.intValue()).list();
    }

    @SuppressWarnings("unchecked")
    public List<Platform> findByServers(Integer[] ids) {
        return createCriteria().createAlias("resource", "r").createAlias(
            "serversBag", "s").add(Restrictions.in("s.id", ids)).addOrder(Order.asc("r.sortName"))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    public Platform findByServiceId(Integer id) {
        String sql = "select distinct p from Platform p " + " join p.serversBag s "
                     + " join s.services sv " + "where " + " sv.id = ?";
        return (Platform) getSession().createQuery(sql).setInteger(0, id.intValue()).uniqueResult();
    }

    public Platform findByCertDN(String dn, String type) {
        String sql = "from Platform p where p.certdn = ? and " + "p.platformType.name = ?";
        return (Platform) getSession().createQuery(sql).setString(0, dn).setString(1, type)
            .uniqueResult();
    }

    public Collection findByApplication(Application app) {
        String sql = "select distinct p from Platform p " + " join p.serversBag s "
                     + " join s.services sv " + " join sv.appServices asv "
                     + "where asv.appication.id = ?";
        return getSession().createQuery(sql).setInteger(0, app.getId().intValue()).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Platform> findByAgent(Agent agt) {
        String sql = "from Platform where agent.id=?";
        return getSession()
            .createQuery(sql)
            .setCacheable(true)
            .setCacheRegion("Platform.findByAgent")
            .setInteger(0, agt.getId()
            .intValue()).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Platform> findByAgentToken(String token) {
        String sql = "select p from Platform p join fetch p.agent a " + "where a.agentToken=?";
        return getSession().createQuery(sql).setString(0, token).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Platform> findByIpAddr(String addr) {
        // here we do not to use 'fetch' join as fetch joins have a
        // side effect of also initializing the collection with the
        // result ips retrieved from the query. That is the ips collection
        // in the 'fetch' join will have only one entry in this instance.
        // That entry being the the row from eam_ip table with eam_ip.address
        // to the 'addr' passed to this method.
        String sql = "select distinct p from Platform p " + "join p.ips ip where ip.address=?";
        return getSession().createQuery(sql).setString(0, addr).list();
    }
    
    @SuppressWarnings("unchecked")
    public Collection<Platform> findByMacAddr(String macAddress) {
        if (macAddress == null) {
            return Collections.EMPTY_LIST;
        }
        // Both the VM and the Guest will have IP entries with 
        // the given MAC address.
        String hql = "select distinct p from Platform p " +
                     "join p.ips ip where upper(ip.macAddress)=?";
         
        return getSession().createQuery(hql)
                    .setString(0, macAddress.toUpperCase())
                    .list();
    }


    public Resource findVirtualByInstanceId(Integer id) {

        return virtualDAO.findVirtualByInstanceId(id, AuthzConstants.platformResType);
    }

    public Collection<Platform> findVirtualByProcessId(Integer id) {

        Collection resources = virtualDAO
            .findVirtualByProcessId(id, AuthzConstants.platformResType);
        List platforms = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            platforms.add(findById(virt.getId()));
        }
        return platforms;
    }

    public Collection<Platform> findVirtualByPhysicalId(Integer id) {

        Collection resources = virtualDAO
            .findVirtualByPysicalId(id, AuthzConstants.platformResType);
        List platforms = new ArrayList();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Virtual virt = (Virtual) it.next();
            platforms.add(findById(virt.getId()));
        }
        return platforms;
    }

    public List<Object[]> getPlatformTypeCounts() {
        String sql = "select t.name, count(*) from PlatformType t, "
                     + "Platform p where p.platformType = t " + "group by t.name order by t.name";

        return getSession().createQuery(sql).list();
    }

    public Number getPlatformCount() {
        return (Number) getSession().createQuery("select count(*) from Platform").uniqueResult();
    }

    public Number getCpuCount() {
        Number rslt = (Number) getSession().createQuery("select sum(p.cpuCount) from Platform p")
            .uniqueResult();
        return (rslt == null) ? new Integer(0) : rslt;
    }

    public Collection<Platform> findDeletedPlatforms() {
        String hql = "from Platform where resource.resourceType = null";
        return createQuery(hql).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<Platform> getOrphanedPlatforms() {
        final String hql = "from Platform where resource is null";
        return getSession().createQuery(hql).list();
    }
}
