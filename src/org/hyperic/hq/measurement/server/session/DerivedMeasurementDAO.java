/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.dao.HibernateDAO;

public class DerivedMeasurementDAO extends HibernateDAO {
    public DerivedMeasurementDAO(DAOFactory f) {
        super(DerivedMeasurement.class, f);
    }

    public DerivedMeasurement findById(Integer id) {
        return (DerivedMeasurement)super.findById(id);
    }

    public DerivedMeasurement get(Integer id) {
        return (DerivedMeasurement)super.get(id);
    }

    void remove(DerivedMeasurement entity) {
        if (entity.getBaseline() != null)
            super.remove(entity.getBaseline());
        super.remove(entity);
    }

    DerivedMeasurement create(Resource resource,
                              MeasurementTemplate mt,
                              long interval) {
        DerivedMeasurement dm = new DerivedMeasurement(resource.getInstanceId(),
                                                       mt, interval);

        dm.setEnabled(interval != 0);
        dm.setFormula(mt.getTemplate());
        dm.setResource(resource);
        save(dm);
        return dm;
    }

    List findByIds(Integer ids[]) {
        String sql = "from DerivedMeasurement where id IN (:ids)";

        return getSession().createQuery(sql)
            .setParameterList("ids", ids)
            .list();
    }
    
    public DerivedMeasurement findByTemplateForInstance(Integer tid,
                                                        Integer iid) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "where t.id=? and m.instanceId=?";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setInteger(0, tid.intValue())
            .setInteger(1, iid.intValue())
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByTemplateForInstance")
            .uniqueResult();
    }
    
    /**
     * Look up a derived measurement, allowing for the query to return a stale 
     * copy of the derived measurement (for efficiency reasons).
     * 
     * @param tid
     * @param iid
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The derived measurement or <code>null</code>.
     */
    public DerivedMeasurement findByTemplateForInstance(Integer tid, 
                                                        Integer iid, 
                                                        boolean allowStale) {
        Session session = this.getSession();
        FlushMode oldFlushMode = session.getFlushMode();
        
        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);                
            }
            
            return this.findByTemplateForInstance(tid, iid); 
        } finally {
            session.setFlushMode(oldFlushMode);
        } 
    }

    public List findIdsByTemplateForInstances(Integer tid, Integer[] iids) {
        if (iids.length == 0)
            return new ArrayList(0);
        
        String sql = "select id from DerivedMeasurement " +
                     "where template.id = :tid and instanceId IN (:ids)";

        return getSession().createQuery(sql)
            .setInteger("tid", tid.intValue())
            .setParameterList("ids", iids)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findIdsByTemplateForInstances")
            .list();
    }

    List findByTemplate(Integer id) {
        String sql = "select distinct m from DerivedMeasurement m " +
                     "join m.template t " +
                     "where t.id=?";

        return getSession().createQuery(sql)
               .setInteger(0, id.intValue()).list();   
    }
    
    /**
     * Find the AppdefEntityID objects for all the derived measurements 
     * associated with the measurement template.
     * 
     * @param id The measurement template id.
     * @return A list of AppdefEntityID objects.
     */
    List findAppdefEntityIdsByTemplate(Integer id) {
        String sql = "select distinct mt.appdefType, m.instanceId from " +
        		     "DerivedMeasurement m join m.template t " +
                     "join t.monitorableType mt where t.id=?";
        
        List results = getSession()
                   .createQuery(sql)
                   .setInteger(0, id.intValue())
                   .list();
        
        List appdefEntityIds = new ArrayList(results.size());
        
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            Object[] result = (Object[]) iter.next();
            int appdefType = ((Integer)result[0]).intValue();
            int instanceId = ((Integer)result[1]).intValue();            
            appdefEntityIds.add(new AppdefEntityID(appdefType, instanceId));
        }
        
        return appdefEntityIds;
    }
    
    /**
     * Set the interval for all the associated derived measurements to the 
     * measurement template interval. Also, make sure that if the measurement 
     * template has default on set, then the associated derived measurements 
     * are enabled (and vice versa). 
     * 
     * @param template The measurement template (that has been persisted, and 
     *                 thus, has its id set).
     */
    void updateIntervalToTemplateInterval(MeasurementTemplate template) {        
        String sql = "update versioned DerivedMeasurement set " +
                     "interval = :newInterval, enabled = :isEnabled " +
                     "where template.id = :tid";
        
        getSession().createQuery(sql)
                    .setLong("newInterval", template.getDefaultInterval())
                    .setBoolean("isEnabled", template.isDefaultOn())
                    .setInteger("tid", template.getId().intValue())
                    .executeUpdate();
    }
    
    /**
     * Set the interval for all metrics to the specified interval
     */
    void updateInterval(List mids, long interval) {
        if (mids.size() == 0)
            return;
        
        String sql = "UPDATE DerivedMeasurement " +
                     "SET enabled = true, interval = :interval " +
                     "WHERE id IN (:ids)";

        getSession().createQuery(sql)
            .setLong("interval", interval)
            .setParameterList("ids", mids)
            .executeUpdate();
    }

    List findByInstance(Resource resource)
    {
        return createCriteria()
            .add(Restrictions.eq("resource", resource))
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByInstance_with_interval")
            .list();
    }

    int deleteByIds(Collection ids) {
        return getSession()
            .createQuery("delete from DerivedMeasurement where id in (:ids)")
            .setParameterList("ids", ids)
            .executeUpdate();
    }
    
    int clearResource(Resource resource) {
        return getSession()
            .createSQLQuery("update EAM_MEASUREMENT set resource_id = null "
                            + "where resource_id = :res")
            .setInteger("res", resource.getId())
            .executeUpdate();
    }

    public List findEnabledByInstance(Resource resource) {
        return createCriteria()
            .add(Restrictions.eq("resource", resource))
            .add(Restrictions.eq("enabled", Boolean.TRUE))
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByInstance")
            .list();
    }

    List findByInstanceForCategory(int type, int id, String cat) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where mt.appdefType = ? and " +
            "m.instanceId = ? and " +
            "c.name = ? and " +
            "m.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setString(2, cat).list();
    }

    List findByInstanceForCategory(Resource resource, String cat) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.category c " +
            "where m.resource = ? and m.enabled = true and c.name = ? and " +
            "m.interval is not null order by t.name";

        return getSession().createQuery(sql)
            .setParameter(0, resource)
            .setString(1, cat).list();
    }
    
    DerivedMeasurement findByAliasAndID(String alias, Resource resource) {

        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "where t.alias = ? and m.resource = ?";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setString(0, alias)
            .setParameter(1, resource)
            .uniqueResult();
    }

    List findDesignatedByInstanceForCategory(Resource resource, String cat) 
    {
        List res = findDesignatedByInstance(resource);
        
        for (Iterator i=res.iterator(); i.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement)i.next();
            
            if (!dm.getTemplate().getCategory().getName().equals(cat))
                i.remove();
        }
        
        return res;
    }

    List findDesignatedByInstance(Resource resource) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "where m.resource = ? and " +
            "t.designate = true " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setParameter(0, resource)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findDesignatedByInstance")
            .list();
    }

    List findAvailabilityByInstances(int type, Integer[] ids) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where mt.appdefType = :type and m.instanceId in (:ids) and " +
            "t.name = 'Availability'";

        return getSession().createQuery(sql)
            .setInteger("type", type)
            .setParameterList("ids", ids)
            .list();
    }

    List findByRawExcludeIdentity(Integer rid) {
        String sql =
            "select distinct d from DerivedMeasurement d " +
            "join d.template t " +
            "join t.measurementArgsBag a, " +
            "RawMeasurement r " +
            "where d.interval is not null and " +
            "d.instanceId = r.instanceId and " +
            "a.template.id = r.template.id and " +
            "r.id = ? and " +
            "t.template <> ?";

        return getSession().createQuery(sql)
                .setInteger(0, rid.intValue())
                .setString(1, "ARG1").list();
    }

    List findByCategory(String cat) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where m.enabled = true " +
            "and m.interval is not null and " +
            "c.name = ?";

        return getSession().createQuery(sql)
            .setString(0, cat)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByCategory")
            .list();
    }
    
    List findMetricsCountMismatch(String plugin) {
        return getSession().createSQLQuery(
            "SELECT 1, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_PLATFORM_TYPE ST "
              + "INNER JOIN EAM_PLATFORM S ON PLATFORM_TYPE_ID = ST.ID " +
            "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND " +
                  "MT.NAME = ST.NAME AND " +
                  "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M," +
                                           "EAM_MEASUREMENT_TEMPL T " +
                   "WHERE M.TEMPLATE_ID = T.ID AND " +
                         "T.MONITORABLE_TYPE_ID = MT.ID AND " +
                         "INSTANCE_ID = S.ID) < " +
                  "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T " +
                   "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID UNION " +
            "SELECT 2, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_SERVER_TYPE ST " +
                "INNER JOIN EAM_SERVER S ON SERVER_TYPE_ID = ST.ID " +
            "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND " +
                  "MT.NAME = ST.NAME AND " +
                  "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M," +
                                           "EAM_MEASUREMENT_TEMPL T " +
                   "WHERE M.TEMPLATE_ID = T.ID AND " +
                         "T.MONITORABLE_TYPE_ID = MT.ID AND " +
                         "INSTANCE_ID = S.ID) < " +
                  "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T " +
                   "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID UNION " +
            "SELECT 3, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_SERVICE_TYPE ST "+
                "INNER JOIN EAM_SERVICE S ON SERVICE_TYPE_ID = ST.ID " +
            "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND " +
                  "MT.NAME = ST.NAME AND " +
                  "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M," +
                                           "EAM_MEASUREMENT_TEMPL T " +
                   "WHERE M.TEMPLATE_ID = T.ID AND " +
                         "T.MONITORABLE_TYPE_ID = MT.ID AND " +
                         "INSTANCE_ID = S.ID) < " +
                  "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T " +
                   "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID")
            .setString("plugin", plugin)
            .list();
    }
    
    List findMetricCountSummaries() {
        String sql = 
            "SELECT COUNT(m.template_id) AS total, " +
            "m.coll_interval/60000 AS coll_interval, " +  
            "t.name AS name, mt.name AS type " + 
            "FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, " +
            "EAM_MONITORABLE_TYPE mt " +
            "WHERE m.template_id = t.id " +
            " and t.monitorable_type_id=mt.id " +
            " and m.coll_interval > 0 " +
            " and m.enabled = :enabled " +
            "GROUP BY m.template_id, t.name, mt.name, m.coll_interval " +
            "ORDER BY total DESC"; 
        List vals = getSession().createSQLQuery(sql)
            .setBoolean("enabled", true)
            .list();

        List res = new ArrayList(vals.size());
        
        for (Iterator i=vals.iterator(); i.hasNext(); ) {
            Object[] v = (Object[])i.next();
            java.lang.Number total = (java.lang.Number)v[0];
            java.lang.Number interval = (java.lang.Number)v[1];
            String metricName = (String)v[2];
            String resourceName = (String)v[3];
            
            res.add(new CollectionSummary(total.intValue(), interval.intValue(),
                                          metricName, resourceName));
        }
        return res;
    }
    
    /**
     * @see DerivedMeasurementManagerEJBImpl#findAgentOffsetTuples()
     */
    List findAgentOffsetTuples() {
        String sql = "select a, p, s, meas from Agent a " + 
            "join a.platforms p " + 
            "join p.platformType pt " + 
            "join p.servers s " + 
            "join s.serverType st, " + 
            "Measurement as meas " + 
            "join meas.template as templ " + 
            "join templ.monitorableType as mt " + 
            "where " +  
            "pt.plugin = 'system' " +
            "and templ.name = 'Server Offset' " + 
            "and templ.template = 'ARG1' " + 
            "and meas.instanceId = s.id " + 
            "and st.name = 'HQ Agent' "; 

        return getSession().createQuery(sql).list();
    }

    /**
     * @see DerivedMeasurementManagerEJBImpl#findNumMetricsPerAgent()
     */
    Map findNumMetricsPerAgent() {
        String platSQL = 
            "select a.id, count(m) from Agent a " + 
            "join a.platforms p, " +
            "Measurement as m " + 
            "join m.template templ " + 
            "join templ.monitorableType monType " + 
            "where " + 
            " monType.appdefType = '1' and m.instanceId = p.id " + 
            "and m.enabled = true " + 
            "group by a";
        String serverSQL = 
            "select a.id, count(m) from Agent a " + 
            "join a.platforms p " +
            "join p.servers s, " +
            "Measurement as m " + 
            "join m.template templ " + 
            "join templ.monitorableType monType " + 
            "where " + 
            " monType.appdefType = '2' and m.instanceId = s.id " + 
            "and m.enabled = true " + 
            "group by a";
        String serviceSQL = 
            "select a.id, count(m) from Agent a " + 
            "join a.platforms p " +
            "join p.servers s " +
            "join s.services v, " +
            "Measurement as m " + 
            "join m.template templ " + 
            "join templ.monitorableType monType " + 
            "where " + 
            " monType.appdefType = '3' and m.instanceId = v.id " + 
            "and m.enabled = true " + 
            "group by a";
        String[] queries = {platSQL, serverSQL, serviceSQL};
        Map idToCount = new HashMap();
        
        for (int i=0; i<queries.length; i++) {
            List tuples = getSession().createQuery(queries[i]).list();
            
            for (Iterator j=tuples.iterator(); j.hasNext(); ) {
                Object[] tuple = (Object[])j.next();
                Integer id = (Integer)tuple[0];
                java.lang.Number count = (java.lang.Number)tuple[1];
                Long curCount;
                
                curCount = (Long)idToCount.get(id);
                if (curCount == null) {
                    curCount = new Long(0);
                }
                curCount = new Long(curCount.longValue() + count.longValue());
                idToCount.put(id, curCount);
            }
        }
        
        Map res = new HashMap(idToCount.size());
        AgentManagerLocal agentMan = AgentManagerEJBImpl.getOne();
        for (Iterator i=idToCount.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            Integer id = (Integer)ent.getKey();
            Long count = (Long)ent.getValue();
            
            res.put(agentMan.findAgentPojo(id), count);
        }
        return res;
    }

    /**
     * Find a list of Measurement ID's that are no longer associated with a
     * resource.
     *
     * @return A List of DerivedMeasurement ID's.
     */
    List findOrphanedMeasurements() {
        String sql = "SELECT id FROM DerivedMeasurement WHERE resource IS NULL";
        return getSession().createQuery(sql).list();
    }
}
