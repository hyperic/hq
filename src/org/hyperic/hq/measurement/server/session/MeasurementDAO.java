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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.jdbc.DBUtil;

public class MeasurementDAO extends HibernateDAO {
    private static Log _log = LogFactory.getLog(MeasurementDAO.class);

    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" +
    				MeasurementConstants.CAT_AVAILABILITY.toUpperCase() + "' ";

    public MeasurementDAO(DAOFactory f) {
        super(Measurement.class, f);
    }

    public Measurement findById(Integer id) {
        return (Measurement)super.findById(id);
    }

    public Measurement get(Integer id) {
        return (Measurement)super.get(id);
    }

    void remove(Measurement entity) {
        if (entity.getBaseline() != null)
            super.remove(entity.getBaseline());
        super.remove(entity);
    }

    /**
     * Remove all measurements associated with a MeasurementTemplate
     * @param mt The MeasurementTemplate for the Measurements to be removed.
     */
    void remove(MeasurementTemplate mt) {
        String sql = "from Measurement where template.id=?";
        List measurements = getSession().createQuery(sql)
            .setInteger(0, mt.getId().intValue())
            .list();

        MeasurementDAO dao =
            new MeasurementDAO(DAOFactory.getDAOFactory());

        for (Iterator it = measurements.iterator(); it.hasNext();) {

            Measurement meas = (Measurement) it.next();
            dao.remove(meas);
        }
    }

    Measurement create(Resource resource,
                       MeasurementTemplate mt,
                       String dsn,
                       long interval) {
        Measurement m = new Measurement(resource.getInstanceId(),
                                        mt, interval);

        m.setEnabled(interval != 0);
        m.setDsn(dsn);
        m.setResource(resource);
        save(m);
        return m;
    }
    
    /**
     * Look up a Measurement, allowing for the query to return a stale
     * copy (for efficiency reasons).
     * 
     * @param tid The MeasurementTemplate id
     * @param iid The instance id
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The Measurement or <code>null</code>.
     */
    Measurement findByTemplateForInstance(Integer tid, Integer iid,
                                          boolean allowStale) {
        Session session = getSession();
        FlushMode oldFlushMode = session.getFlushMode();
        
        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);                
            }
            
            String sql =
                "select distinct m from Measurement m " +
                "join m.template t " +
                "where t.id=? and m.instanceId=?";
            
            return (Measurement) getSession().createQuery(sql)
                .setInteger(0, tid.intValue())
                .setInteger(1, iid.intValue())
                .setCacheable(true)
                .setCacheRegion("Measurement.findByTemplateForInstance")
                .uniqueResult(); 
        } finally {
            session.setFlushMode(oldFlushMode);
        } 
    }

    public List findByTemplatesForInstance(Integer[] tids, Resource res) {
        if (tids.length == 0)   // Nothing to do
            return new ArrayList(0);
        
        String sql =
            "select m from Measurement m " +
            "join m.template t " +
            "where t.id in (:tids) and m.resource = :res";

        return getSession().createQuery(sql)
            .setParameterList("tids", tids)
            .setParameter("res", res)
            .setCacheable(true)     // Share the cache for now
            .setCacheRegion("Measurement.findByTemplateForInstance")
            .list();
    }
    
    public List findIdsByTemplateForInstances(Integer tid, Integer[] iids) {
        if (iids.length == 0)
            return new ArrayList(0);
        
        String sql = "select id from Measurement " +
                     "where template.id = :tid and instanceId IN (:ids)";

        return getSession().createQuery(sql)
            .setInteger("tid", tid.intValue())
            .setParameterList("ids", iids)
            .setCacheable(true)
            .setCacheRegion("Measurement.findIdsByTemplateForInstances")
            .list();
    }

    List findByTemplate(Integer id) {
        String sql = "select distinct m from Measurement m " +
                     "join m.template t " +
                     "where t.id=?";

        return getSession().createQuery(sql)
               .setInteger(0, id.intValue()).list();   
    }
    
    /**
     * Find the AppdefEntityID objects for all the Measurements
     * associated with the MeasurementTemplate.
     * 
     * @param id The measurement template id.
     * @return A list of AppdefEntityID objects.
     */
    List findAppdefEntityIdsByTemplate(Integer id) {
        String sql = "select distinct mt.appdefType, m.instanceId from " +
        		     "Measurement m join m.template t " +
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
     * Set the interval for all the associated Measurements to the
     * MeasurementTemplate interval. Also, make sure that if the
     * MeasurementTemplate has default on set, then the associated Measurements
     * are enabled (and vice versa). 
     * 
     * @param template The MeasurementTemplate (that has been persisted, and
     *                 thus, has its id set).
     */
    void updateIntervalToTemplateInterval(MeasurementTemplate template) {        
        String sql = "update versioned Measurement set " +
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
     * @param mids  The list of Measurement id's to update
     * @param interval The new interval in milliseconds
     */
    void updateInterval(List mids, long interval) {
        if (mids.size() == 0)
            return;
        
        String sql = "UPDATE Measurement " +
                     "SET enabled = true, interval = :interval " +
                     "WHERE id IN (:ids)";

        getSession().createQuery(sql)
            .setLong("interval", interval)
            .setParameterList("ids", mids)
            .executeUpdate();
    }

    List findByResource(Resource resource)
    {
        return createCriteria()
            .add(Restrictions.eq("resource", resource))
            .setCacheable(true)
            .setCacheRegion("Measurement.findByResource")
            .list();
    }

    int deleteByIds(Collection ids) {
        final String hql = "delete from Measurement where id in (:ids)";

        Session session = getSession();
        int count = 0;
        for (Iterator it = ids.iterator(); it.hasNext(); ) {
            ArrayList subIds = new ArrayList();
            
            for (int i = 0; i < DBUtil.IN_CHUNK_SIZE && it.hasNext(); i++) {
                subIds.add(it.next());
            }
            
            count += session.createQuery(hql).setParameterList("ids", subIds)
                            .executeUpdate();
            
            if (_log.isDebugEnabled()) {
                _log.debug("deleteByIds() " + subIds.size() + " of " +
                           ids.size() + " metric IDs");
            }
        }
        
        return count;
    }

    public List findEnabledByResource(Resource resource) {
        String sql =
            "select m from Measurement m " +
            "join m.template t " +
            "where m.enabled = ? and " +
            "m.resource = ? " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setBoolean(0, true)
            .setParameter(1, resource)
            .setCacheable(true)
            .setCacheRegion("Measurement.findEnabledByResource").list();
    }

    List findByInstanceForCategory(int type, int id, String cat) {
        String sql =
            "select m from Measurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where mt.appdefType = ? and " +
            "m.instanceId = ? and " +
            "c.name = ?";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setString(2, cat).list();
    }

    List findByResourceForCategory(Resource resource, String cat) {
        String sql =
            "select m from Measurement m " +
            "join m.template t " +
            "join t.category c " +
            "where m.resource = ? and m.enabled = true and c.name = ? " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setParameter(0, resource)
            .setString(1, cat).list();
    }
    
    Measurement findByAliasAndID(String alias, Resource resource) {

        String sql =
            "select distinct m from Measurement m " +
            "join m.template t " +
            "where t.alias = ? and m.resource = ?";

        return (Measurement)getSession().createQuery(sql)
            .setString(0, alias)
            .setParameter(1, resource)
            .uniqueResult();
    }

    List findDesignatedByResourceForCategory(Resource resource, String cat)
    {
        List res = findDesignatedByResource(resource);
        
        for (Iterator i=res.iterator(); i.hasNext(); ) {
            Measurement dm = (Measurement)i.next();
            
            if (!dm.getTemplate().getCategory().getName().equals(cat))
                i.remove();
        }
        
        return res;
    }

    List findDesignatedByResource(Resource resource) {
        String sql =
            "select m from Measurement m " +
            "join m.template t " +
            "where m.resource = ? and " +
            "t.designate = true " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setParameter(0, resource)
            .setCacheable(true)
            .setCacheRegion("Measurement.findDesignatedByResource")
            .list();
    }

    List findByCategory(String cat) {
        String sql =
            "select distinct m from Measurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where m.enabled = true " +
            "and c.name = ?";

        return getSession().createQuery(sql)
            .setString(0, cat)
            .setCacheable(true)
            .setCacheRegion("Measurement.findByCategory")
            .list();
    }

    /**
     * @return List of all measurement ids for availability, ordered
     */
    List findAllAvailIds() {
        String sql = new StringBuffer()
            .append("select m.id from Measurement m ")
            .append("join m.template t ")
            .append("where ")
            .append(ALIAS_CLAUSE)
            .append("and m.resource is not null ")
            .append("order BY m.id").toString();
        return getSession()
            .createQuery(sql)
            .setCacheable(true)
            .setCacheRegion("Measurement.findAllAvailIds")
            .list();
    }

    Measurement findAvailMeasurement(Resource resource) {
        String sql = new StringBuffer()
            .append("select distinct m from Measurement m ")
            .append("join m.template t ")
            .append("where m.resource = :res AND ")
            .append(ALIAS_CLAUSE).toString();
        return (Measurement) getSession().createQuery(sql)
            .setParameter("res", resource)
            .setCacheable(true)
            .setCacheRegion("Measurement.findAvailMeasurement")
            .uniqueResult();
    }

    List findAvailMeasurementsByInstances(int type, Integer[] ids) {
        boolean checkIds = (ids != null && ids.length > 0);
        String sql = new StringBuffer()
            .append("select m from Measurement m ")
            .append("join m.template t ")
            .append("join t.monitorableType mt ")
            .append("where mt.appdefType = :type and ")
            .append("m.resource is not null and ")
            .append((checkIds ? "m.instanceId in (:ids) and " : ""))
            .append(ALIAS_CLAUSE).toString();

        Query q = getSession().createQuery(sql).setInteger("type", type);

        if (checkIds) {
            q.setParameterList("ids", ids);
        }

        q.setCacheable(true);
        q.setCacheRegion("Measurement.findAvailMeasurementsByInstances");
        return q.list();
    }

    /**
     * param List of resourceIds return List of Availability Measurements which
     * are children of the resourceIds
     */
    List findAvailMeasurements(List resourceIds) {
        String sql = new StringBuffer()
            .append("select m from Measurement m ")
            .append("join m.resource.toEdges e ")
            .append("join m.template t ")
            .append("where m.resource is not null ")
            .append("and e.distance > 0 ")
            .append("and e.from in (:ids) and ")
            .append(ALIAS_CLAUSE).toString();
        return getSession()
            .createQuery(sql)
            .setParameterList("ids", resourceIds, new IntegerType())
            .setCacheable(true)
            .setCacheRegion("Measurement.findAvailMeasurements")
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
     * @see MeasurementManagerEJBImpl#findAgentOffsetTuples()
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
            "and meas.instanceId = s.id " + 
            "and st.name = 'HQ Agent' "; 

        return getSession().createQuery(sql).list();
    }

    /**
     * @see MeasurementManagerEJBImpl#findNumMetricsPerAgent()
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

    int clearResource(Resource resource) {
        // XXX: Shouldn't this reference Measurement rather than the mapped
        //      table? -RPM
        return getSession()
            .createSQLQuery("update EAM_MEASUREMENT set resource_id = null "
                            + "where resource_id = :res")
            .setInteger("res", resource.getId().intValue())
            .executeUpdate();
    }

    /**
     * Find a list of Measurement ID's that are no longer associated with a
     * resource.
     *
     * @return A List of Measurement ID's.
     */
    List findOrphanedMeasurements() {
        String sql = "SELECT id FROM Measurement WHERE resource IS NULL";
        return getSession().createQuery(sql).list();
    }
}
