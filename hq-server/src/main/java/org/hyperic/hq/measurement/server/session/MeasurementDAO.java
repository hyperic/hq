/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MeasurementDAO extends HibernateDAO<Measurement> {
    private static final String CAT_AVAILABILITY = MeasurementConstants.CAT_AVAILABILITY.toUpperCase();
    private static final String NON_AVAIL_CLAUSE = " upper(t.alias) != '" + CAT_AVAILABILITY + "' ";
    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" + CAT_AVAILABILITY + "' ";
    private final AgentDAO agentDao;
    private final Log log = LogFactory.getLog(MeasurementDAO.class.getName());

    @Autowired
    public MeasurementDAO(SessionFactory f, AgentDAO agentDao) {
        super(Measurement.class, f);
        this.agentDao = agentDao;
    }

    public void removeBaseline(Measurement m) {
        m.setBaseline(null);
    }

    /**
     * Used primarily for preloaded 2nd level cache measurement objects
     * retrieves List<Object[]> 
     * [0] = Measurement 
     * [1] = MeasurementTemplate
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findAllEnabledMeasurementsAndTemplates() {
        String hql = new StringBuilder().append("from Measurement m").append(" join m.template t")
               .append(" left outer join fetch m.baselinesBag b")
               .append(" where enabled = '1'").toString();
        return getSession().createQuery(hql).list();
    }

    /**
     * Remove all measurements associated with a MeasurementTemplate
     * @param mt The MeasurementTemplate for the Measurements to be removed.
     */
    @SuppressWarnings("unchecked")
    void remove(MeasurementTemplate mt) {
        String sql = "from Measurement where template.id=?";
        List<Measurement> measurements = getSession().createQuery(sql).setInteger(0,
            mt.getId().intValue()).list();

        for (Measurement meas : measurements) {
            remove(meas);
        }
    }

    Measurement create(Resource resource, MeasurementTemplate mt, String dsn, long interval) {
        Measurement m = new Measurement(resource.getInstanceId(), mt, interval);

        m.setEnabled(interval != 0);
        m.setDsn(dsn);
        m.setResource(resource);
        save(m);
        return m;
    }

    /**
     * Look up a Measurement, allowing for the query to return a stale copy (for
     * efficiency reasons).
     * 
     * @param tid The MeasurementTemplate id
     * @param iid The instance id
     * @param allowStale <code>true</code> to allow stale copies of an alert
     *        definition in the query results; <code>false</code> to never allow
     *        stale copies, potentially always forcing a sync with the database.
     * @return The Measurement or <code>null</code>.
     */
    Measurement findByTemplateForInstance(Integer tid, Integer iid, boolean allowStale) {
        Session session = getSession();
        FlushMode oldFlushMode = session.getFlushMode();

        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);
            }

            String sql = "select distinct m from Measurement m " + "join m.template t "
                         + "where t.id=? and m.instanceId=?";

            return (Measurement) getSession().createQuery(sql).setInteger(0, tid.intValue())
                .setInteger(1, iid.intValue()).setCacheable(true).setCacheRegion(
                    "Measurement.findByTemplateForInstance").uniqueResult();
        } finally {
            session.setFlushMode(oldFlushMode);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Measurement> findByTemplatesForInstance(Integer[] tids, Resource res) {
        if (tids.length == 0) // Nothing to do
            return new ArrayList<Measurement>(0);

        String sql = "select m from Measurement m " + "join m.template t "
                     + "where t.id in (:tids) and m.resource = :res";

        return getSession().createQuery(sql).setParameterList("tids", tids)
            .setParameter("res", res).setCacheable(true) // Share the cache for
                                                         // now
            .setCacheRegion("Measurement.findByTemplateForInstance").list();
    }

    @SuppressWarnings("unchecked")
    List<Integer> findIdsByTemplateForInstances(Integer tid, Integer[] iids) {
        if (iids.length == 0) {
            return new ArrayList<Integer>(0);
        }

        String sql = "select id from Measurement "
                     + "where template.id = :tid and instanceId IN (:ids)";

        return getSession().createQuery(sql).setInteger("tid", tid.intValue()).setParameterList(
            "ids", iids).setCacheable(true).setCacheRegion(
            "Measurement.findIdsByTemplateForInstances").list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findByTemplate(Integer id) {
        String sql = "select distinct m from Measurement m " + "join m.template t "
                     + "where t.id=?";

        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    /**
     * Find the AppdefEntityID objects for all the Measurements associated with
     * the MeasurementTemplate.
     * 
     * @param id The measurement template id.
     * @return A list of AppdefEntityID objects.
     */
    @SuppressWarnings("unchecked")
    List<AppdefEntityID> findAppdefEntityIdsByTemplate(Integer id) {
        String sql = "select distinct mt.appdefType, m.instanceId from "
                     + "Measurement m join m.template t "
                     + "join t.monitorableType mt where t.id=?";

        List<Object[]> results = getSession().createQuery(sql).setInteger(0, id.intValue()).list();

        List<AppdefEntityID> appdefEntityIds = new ArrayList<AppdefEntityID>(results.size());

        for (Object[] result : results) {
            int appdefType = ((Integer) result[0]).intValue();
            int instanceId = ((Integer) result[1]).intValue();
            appdefEntityIds.add(new AppdefEntityID(appdefType, instanceId));
        }

        return appdefEntityIds;
    }
    
    @SuppressWarnings("unchecked")
    List<Measurement> findByResources(List<Resource> resources) {
        final List<Measurement> measurements = new ArrayList<Measurement>();
        final String hql = "select m from Measurement m where m.resource in (:resources)";
        final Query query = getSession().createQuery(hql);
        final int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(size, i + BATCH_SIZE);
            final List<Resource> sublist = resources.subList(i, end);
            measurements.addAll(query.setParameterList("resources", sublist).list());
        }
        return measurements;
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findByResource(Resource resource) {
        return createCriteria().add(Restrictions.eq("resource", resource)).list();
    }

    int deleteByIds(List<Integer> ids) {
        int count = 0;
        // need to remove one at a time to avoid EhCache clearing the
        // measurement cache which would lead to thrashing
        for (Integer id : ids) {
            Measurement meas = findById(id);
            if (meas == null) {
                continue;
            }
            count++;
            remove(meas);
        }
        return count;
    }
    
    /**
     * @param {@link Collection} of {@link Resource}s
     * @param includeAvailability - should availability be included in the returned Map
     * @return {@link Map} of {@link Integer} representing resourceId to
     * {@link List} of {@link Measurement}s
     */
    @SuppressWarnings("unchecked")
    public Map<Integer,List<Measurement>> findEnabledByResources(List<Resource> resources,
                                                                 boolean includeAvailability) {
        if (resources == null || resources.size() == 0) {
            return new HashMap<Integer,List<Measurement>>(0,1);
        }
        final String sql = new StringBuilder(256)
            .append("select m from Measurement m ")
            .append((!includeAvailability) ? "join m.template t " : "")
            .append("where m.enabled = '1' and ")
            .append((!includeAvailability) ? NON_AVAIL_CLAUSE + "and ": "")
            .append("m.resource in (:rids) ")
            .toString();
        final Map<Integer,List<Measurement>> rtn = new HashMap<Integer,List<Measurement>>();
        final Query query = getSession().createQuery(sql);
        final int size = resources.size();
        for (int i=0; i<size; i+=BATCH_SIZE) {
            int end = Math.min(size, i+BATCH_SIZE);
            final List<Resource> sublist = resources.subList(i, end);
            final List<Measurement> resultset = query.setParameterList("rids", sublist).list();
            for (final Measurement m : resultset ) {
                final Resource r = m.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                List<Measurement> tmp = rtn.get(r.getId());
                if (tmp == null) {
                    tmp = new ArrayList<Measurement>();
                    rtn.put(r.getId(), tmp);
                }
                tmp.add(m);
            }
        }
        return rtn;
    }
 
    @SuppressWarnings("unchecked")
    public List<Measurement> findEnabledByResource(Resource resource, boolean sortByName) {
        if (resource == null || resource.isInAsyncDeleteState()) {
            return Collections.emptyList();
        }
        final String hql = (sortByName) ? 
            "select m from Measurement m join m.template t where m.enabled = true and m.resource = ? order by t.name" :
            "select m from Measurement m where m.enabled = true and m.resource = ?";
        return getSession().createQuery(hql)
                           .setParameter(0, resource)
                           .setCacheable(true)
                           .setCacheRegion("Measurement.findEnabledByResource")
                           .list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findDefaultsByResource(Resource resource) {
        return getSession().createQuery(
            "select m from Measurement m join m.template t "
                + "where t.defaultOn = true and m.resource = ? " + "order by m.id ").setParameter(
            0, resource).list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findByInstanceForCategory(int type, int id, String cat) {
        String sql = "select m from Measurement m " + "join m.template t "
                     + "join t.monitorableType mt " + "join t.category c "
                     + "where mt.appdefType = ? and " + "m.instanceId = ? and " + "c.name = ?";

        return getSession().createQuery(sql).setInteger(0, type).setInteger(1, id)
            .setString(2, cat).list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findByResourceForCategory(Resource resource, String cat) {
        String sql = "select m from Measurement m " + "join m.template t " + "join t.category c "
                     + "where m.resource = ? and m.enabled = true and c.name = ? "
                     + "order by t.name";

        return getSession().createQuery(sql).setParameter(0, resource).setString(1, cat).list();
    }

    Measurement findByAliasAndID(String alias, Resource resource) {

        String sql = "select distinct m from Measurement m " + "join m.template t "
                     + "where t.alias = ? and m.resource = ?";

        return (Measurement) getSession().createQuery(sql).setString(0, alias).setParameter(1,
            resource).uniqueResult();
    }

    /**
         * @param resources {@link List} of {@link Resource}s
         * @return {@link List} of {@link Measurement}s
    */
    @SuppressWarnings("unchecked")
    List<Measurement> findDesignatedByResourcesForCategory(List<Resource> resources, String cat) {
            String sql = new StringBuilder(512)
                .append("select m from Measurement m ")
                .append("join m.template t ")
                .append("join t.category c ")
                .append("where m.resource in (:rids) and ")
                .append("t.designate = true and ")
                .append("c.name = :cat")
                .toString();
            int size = resources.size();
            List<Measurement> rtn = new ArrayList<Measurement>(size*5);
            for (int i=0; i<size; i=BATCH_SIZE) {
                int end = Math.min(size, i + BATCH_SIZE);
                rtn.addAll(getSession().createQuery(sql)
                    .setParameterList("rids", resources.subList(i, end))
                    .setParameter("cat", cat)
                    .list());
            }
            return rtn;
    }
    
    /**
     * @return {@link List} of {@link Measurement}s
     */
    List<Measurement> findDesignatedByResourceForCategory(Resource resource, String cat) {
        return findDesignatedByResourcesForCategory(Collections.singletonList(resource), cat);
    }


    @SuppressWarnings("unchecked")
    List<Measurement> findDesignatedByResource(Resource resource) {
        String sql = "select m from Measurement m " + "join m.template t "
                     + "where m.resource = ? and " + "t.designate = true " + "order by t.name";

        return getSession().createQuery(sql).setParameter(0, resource).setCacheable(true)
            .setCacheRegion("Measurement.findDesignatedByResource").list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findDesignatedByCategoryForGroup(ResourceGroup g, String cat) {
        String sql = "select m from Measurement m, GroupMember gm " + "join m.template t "
                     + "join t.category c "
                     + "where gm.group = :group and gm.resource = m.resource "
                     + "and t.designate = true and c.name = :cat order by t.name";

        return getSession().createQuery(sql).setParameter("group", g).setParameter("cat", cat)
            .setCacheable(true).setCacheRegion("Measurement.findDesignatedByCategoryForGroup")
            .list();
    }
    
    /**
     * Return the maximum collection interval for the given template within the
     * group.
     * 
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId The maximum collection time in milliseconds.
     */
    public Long getMaxCollectionInterval(ResourceGroup g, Integer templateId) {
        String sql = "select max(m.interval) from Measurement m, GroupMember g "
                     + "join g.group rg " + "join g.resource r "
                     + "where m.instanceId = r.instanceId and " + "rg = ? and m.template.id = ?";

        return (Long) getSession().createQuery(sql).setParameter(0, g).setInteger(1,
            templateId.intValue()).setCacheable(true).setCacheRegion(
            "ResourceGroup.getMaxCollectionInterval").uniqueResult();
    }

    /**
     * Return a List of Measurements that are collecting for the given template
     * ID and group.
     * 
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId A list of Measurement objects with the given template
     *         id in the group that are set to be collected.
     */
    @SuppressWarnings("unchecked")
    public List<Measurement> getMetricsCollecting(ResourceGroup g, Integer templateId) {
        String sql = "select m from Measurement m, GroupMember g " + "join g.group rg "
                     + "join g.resource r " + "where m.instanceId = r.instanceId and "
                     + "rg = ? and m.template.id = ? and m.enabled = true";

        return getSession().createQuery(sql).setParameter(0, g).setInteger(1,
            templateId.intValue()).setCacheable(true).setCacheRegion(
            "ResourceGroup.getMetricsCollecting").list();
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findByCategory(String cat) {
        String sql = "select distinct m from Measurement m " + "join m.template t "
                     + "join t.monitorableType mt " + "join t.category c "
                     + "where m.enabled = true " + "and c.name = ?";

        return getSession().createQuery(sql).setString(0, cat).setCacheable(true).setCacheRegion(
            "Measurement.findByCategory").list();
    }

    /**
     * @return List of all measurement ids for availability, ordered
     */
    @SuppressWarnings("unchecked")
    List<Integer> findAllAvailIds() {
        String sql = new StringBuilder().append("select m.id from Measurement m ").append(
            "join m.template t ").append("where ").append(ALIAS_CLAUSE).append(
            "and m.resource is not null ").append("order BY m.id").toString();
        return getSession().createQuery(sql).setCacheable(true).setCacheRegion(
            "Measurement.findAllAvailIds").list();
    }

    /**
     * @return {@link Measurement}. May return null.
     */
    Measurement findAvailMeasurement(Resource resource) {
        List<Measurement> list = findAvailMeasurements(Collections.singletonList(resource));
        if (list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findAvailMeasurements(Collection<Resource> resources) {
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }
        List<Resource> resList = new ArrayList<Resource>(resources);
       
        List<Measurement> rtn = new ArrayList<Measurement>(resList.size());
        final String sql = new StringBuilder().append("select m from Measurement m ").append(
            "join m.template t ").append("where m.resource in (:resources) AND ").append(
            ALIAS_CLAUSE).toString();
        final Query query = getSession().createQuery(sql);

        // should be a unique result if only one resource is being examined
        if (resources.size() == 1) {
            query.setParameterList("resources", resList);
            Measurement result = (Measurement) query.uniqueResult();
            if (result != null) {
                rtn.add(result);
            }
        } else {
            for (int i = 0; i < resList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, resList.size());
                query.setParameterList("resources", resList.subList(i, end));
                rtn.addAll(query.list());
            }
        }

        return rtn;
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findAvailMeasurements(ResourceGroup g) {
        String hql = "select m from GroupMember gm, " + "Measurement m join m.template t " +
                     "where m.resource = gm.resource and gm.group = :group and " + ALIAS_CLAUSE;
        return createQuery(hql).setParameter("group", g).setCacheable(true).setCacheRegion(
            "Measurement.findAvailMeasurementsForGroup").list();
    }

    /**
     * @param tids - {@link Integer[]} of templateIds
     * @param iids - {@link Integer[]} of AppdefEntityIds
     */
    List<Measurement> findMeasurements(Integer[] tids, Integer[] iids) {
        return findMeasurements(tids, iids, false);
    }

    /**
     * @param tids - {@link Integer[]} of templateIds
     * @param iids - {@link Integer[]} of AppdefEntityIds
     * @param onlyEnabled - only selects enabled measurements
     */
    @SuppressWarnings("unchecked")
    List<Measurement> findMeasurements(Integer[] tids, Integer[] iids, boolean onlyEnabled) {
        final IntegerType iType = new IntegerType();
        // sort to take advantage of query cache
        final List<Integer> iidList = new ArrayList<Integer>(Arrays.asList(iids));
        final List<Integer> tidList = new ArrayList<Integer>(Arrays.asList(tids));
        Collections.sort(tidList);
        Collections.sort(iidList);
        final StringBuilder buf = new StringBuilder(32)
            .append("select m from Measurement m ")
            .append("join m.template t ")
            .append("where m.instanceId in (:iids) AND t.id in (:tids)");
        if (onlyEnabled) {
            buf.append(" and enabled = :enabled");
        }
        final String sql = buf.toString();
        final List<Measurement> rtn = new ArrayList<Measurement>(iidList.size());
        final int batch = BATCH_SIZE/2;
        for (int xx=0; xx<iidList.size(); xx+=batch) {
            final int iidEnd = Math.min(xx+batch, iidList.size());
            for (int yy=0; yy<tidList.size(); yy+=batch) {
                final int tidEnd = Math.min(yy+batch, tidList.size());
                Query query = getSession().createQuery(sql)
                    .setParameterList("iids", iidList.subList(xx, iidEnd), iType)
                    .setParameterList("tids", tidList.subList(yy, tidEnd), iType);
                if (onlyEnabled) {
                    query.setBoolean("enabled", onlyEnabled);
                }
                rtn.addAll(query.setCacheable(true)
                    .setCacheRegion("Measurement.findMeasurements")
                    .list());
            }
        }
        return rtn;
    }

    
    @SuppressWarnings("unchecked")
    List<Measurement> findByIds(Integer[] mids) {
        final StringBuilder buf = new StringBuilder()
            .append("select m from Measurement m ")
            .append("where m.id in (:mids)");
        final String sql = buf.toString();
        Session session = getSession();
        Query query = session.createQuery(sql).setParameterList("mids", mids);
        return query.list();
    }
    
    @SuppressWarnings("unchecked")
    List<Measurement> findAvailMeasurements(Integer[] tids, Integer[] iids) {
        final IntegerType iType = new IntegerType();
        final List<Integer> iidList = Arrays.asList(iids);
        final List<Integer> tidList = Arrays.asList(tids);
        final String sql = new StringBuilder(256)
            .append("select m from Measurement m ").append(
            "join m.template t ").append("where m.instanceId in (:iids) AND t.id in (:tids) AND ")
            .append(ALIAS_CLAUSE).toString();
        final List<Measurement> rtn = new ArrayList<Measurement>(iidList.size());
        final int batch = BATCH_SIZE/2;
        for (int xx=0; xx<iidList.size(); xx+=batch) {
            final int iidEnd = Math.min(xx+batch, iidList.size());
            for (int yy=0; yy<tidList.size(); yy+=batch) {
                final int tidEnd = Math.min(yy+batch, tidList.size());
                rtn.addAll(getSession().createQuery(sql)
                    .setParameterList("iids", iidList.subList(xx, iidEnd), iType)
                    .setParameterList("tids", tidList.subList(yy, tidEnd), iType).list());
            }
        }
        return rtn;
    }

    /**
     * @param {@link List} of {@link Integer} resource ids
     * @return {@link Object[]}
     *  0 = {@link Integer} resourceId
     *  1 = {@link List} of Availability {@link Measurement}s which are children of the resource
     */
    @SuppressWarnings("unchecked")
    final List<Object[]> findRelatedAvailMeasurements(final List<Integer> resourceIds,
                                                      final String resourceRelationType) {
        if (resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        final String sql = new StringBuilder(300)
            .append("select e.from.id, m ")
            .append("from Measurement m ")
            .append("join m.resource.toEdges e ")
            .append("join m.template t ")
            .append("join e.relation r ")
            .append("where m.resource.resourceType is not null ")
            .append("and e.distance > 0 ")
            .append("and r.name = :relationType ")
            .append("and e.from in (:resourceIds) and ")
            .append(ALIAS_CLAUSE)
            .toString();
        final HQDialect dialect = getHQDialect();
        final int max = (dialect.getMaxExpressions() <= 0) ? BATCH_SIZE : dialect.getMaxExpressions();
        final List<Object[]> rtn = new ArrayList<Object[]>(resourceIds.size());
        for (int i = 0; i < resourceIds.size(); i += max) {
            final int end = Math.min(i + max, resourceIds.size());
            final List<Integer> list = resourceIds.subList(i, end);
            rtn.addAll(getSession().createQuery(sql)
                                   .setParameterList("resourceIds", list, new IntegerType())
                                   .setParameter("relationType", resourceRelationType)
                                   .list());
        }
        return rtn;
    }

    /**
     * @param {@link List} of {@link Integer} resource ids
     * @return {@link Object[]} 0 = {@link Integer} 1 = {@link List} of
     *         Availability {@link Measurement}s Availability measurements which
     *         are parents of the resourceId
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findParentAvailMeasurements(List<Integer> resourceIds, String resourceRelationType) {
        if (resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        // Needs to be ordered by DISTANCE in descending order so that
        // it's immediate parent is the first record
        final String sql = new StringBuilder()
            .append("select e.from.id, m from Measurement m ")
            .append("join m.resource.toEdges e ")
            .append("join m.template t ")
            .append("join e.relation r ")
            .append("where m.resource is not null ")
            .append("and e.distance < 0 ")
            .append("and r.name = :relationType ")
            .append("and e.from in (:resourceIds) and ")
            .append(ALIAS_CLAUSE).append("order by e.from.id, e.distance desc ")
            .toString();
        // create a new list so that the original list is not modified
        // and sort the resource ids so that the results are more cacheable
        final List<Integer> sortedResourceIds = new ArrayList<Integer>(resourceIds);
        Collections.sort(sortedResourceIds);
        final List<Object[]> rtn = new ArrayList<Object[]>(sortedResourceIds.size());
        final HQDialect dialect = getHQDialect();
        final int max = (dialect.getMaxExpressions() <= 0) ? BATCH_SIZE : dialect.getMaxExpressions();
        for (int i = 0; i < sortedResourceIds.size(); i += max) {
            final int end = Math.min(i + max, sortedResourceIds.size());
            final List<Integer> list = sortedResourceIds.subList(i, end);
            rtn.addAll(getSession().createQuery(sql)
                                   .setParameterList("resourceIds", list, new IntegerType())
                                   .setParameter("relationType", resourceRelationType)
                                   .setCacheable(true).setCacheRegion("Measurement.findParentAvailMeasurements")
                                   .list());
        }
        return rtn;
    }

    
    
    
    /**
     * @param {@link List} of {@link Integer} resource ids
     * @return {@link Object[]} 0 = {@link Integer} 1 = {@link List} of
     *         Availability {@link Measurement}s Availability measurements which
     *         are parents of the resourceId
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findDirectParentAvailMeasurements(List<Integer> resourceIds, String resourceRelationType) {
        if (resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        // Needs to be ordered by DISTANCE in descending order so that
        // it's immediate parent is the first record
        final String sql = new StringBuilder()
            .append("select e.from.id, m from Measurement m ")
            .append("join m.resource.toEdges e ")
            .append("join m.template t ")
            .append("join e.relation r ")
            .append("where m.resource is not null ")
            .append("and e.distance = -1 ")
            .append("and r.name = :relationType ")
            .append("and e.from in (:resourceIds) and ")
            .append(ALIAS_CLAUSE).append("order by e.from.id, e.distance desc ")
            .toString();
        // create a new list so that the original list is not modified
        // and sort the resource ids so that the results are more cacheable
        final List<Integer> sortedResourceIds = new ArrayList<Integer>(resourceIds);
        Collections.sort(sortedResourceIds);
        final List<Object[]> rtn = new ArrayList<Object[]>(sortedResourceIds.size());
        final HQDialect dialect = getHQDialect();
        final int max = (dialect.getMaxExpressions() <= 0) ? BATCH_SIZE : dialect.getMaxExpressions();
        for (int i = 0; i < sortedResourceIds.size(); i += max) {
            final int end = Math.min(i + max, sortedResourceIds.size());
            final List<Integer> list = sortedResourceIds.subList(i, end);
            rtn.addAll(getSession().createQuery(sql)
                                   .setParameterList("resourceIds", list, new IntegerType())
                                   .setParameter("relationType", resourceRelationType)
                                   .setCacheable(true).setCacheRegion("Measurement.findParentAvailMeasurements")
                                   .list());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    List<Measurement> findAvailMeasurementsByInstances(int type, Integer[] ids) {
        boolean checkIds = (ids != null && ids.length > 0);
        String sql = new StringBuilder().append("select m from Measurement m ").append(
            "join m.template t ").append("join t.monitorableType mt ").append(
            "where mt.appdefType = :type and ").append("m.resource is not null and ").append(
            (checkIds ? "m.instanceId in (:ids) and " : "")).append(ALIAS_CLAUSE).toString();

        Query q = getSession().createQuery(sql).setInteger("type", type);

        if (checkIds) {
            q.setParameterList("ids", ids);
        }

        q.setCacheable(true);
        q.setCacheRegion("Measurement.findAvailMeasurementsByInstances");
        return q.list();
    }

    @SuppressWarnings("unchecked")
    List<java.lang.Number[]> findMetricsCountMismatch(String plugin) {
        return getSession().createSQLQuery(
            "SELECT 1, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_PLATFORM_TYPE ST "
                + "INNER JOIN EAM_PLATFORM S ON PLATFORM_TYPE_ID = ST.ID "
                + "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND "
                + "MT.NAME = ST.NAME AND " + "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M,"
                + "EAM_MEASUREMENT_TEMPL T " + "WHERE M.TEMPLATE_ID = T.ID AND "
                + "T.MONITORABLE_TYPE_ID = MT.ID AND " + "INSTANCE_ID = S.ID) < "
                + "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T "
                + "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID UNION "
                + "SELECT 2, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_SERVER_TYPE ST "
                + "INNER JOIN EAM_SERVER S ON SERVER_TYPE_ID = ST.ID "
                + "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND "
                + "MT.NAME = ST.NAME AND " + "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M,"
                + "EAM_MEASUREMENT_TEMPL T " + "WHERE M.TEMPLATE_ID = T.ID AND "
                + "T.MONITORABLE_TYPE_ID = MT.ID AND " + "INSTANCE_ID = S.ID) < "
                + "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T "
                + "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID UNION "
                + "SELECT 3, S.ID FROM EAM_MONITORABLE_TYPE MT, EAM_SERVICE_TYPE ST "
                + "INNER JOIN EAM_SERVICE S ON SERVICE_TYPE_ID = ST.ID "
                + "WHERE ST.PLUGIN = MT.PLUGIN AND MT.PLUGIN = :plugin AND "
                + "MT.NAME = ST.NAME AND " + "(SELECT COUNT(M.ID) FROM EAM_MEASUREMENT M,"
                + "EAM_MEASUREMENT_TEMPL T " + "WHERE M.TEMPLATE_ID = T.ID AND "
                + "T.MONITORABLE_TYPE_ID = MT.ID AND " + "INSTANCE_ID = S.ID) < "
                + "(SELECT COUNT(T.ID) FROM EAM_MEASUREMENT_TEMPL T "
                + "WHERE MONITORABLE_TYPE_ID = MT.ID) GROUP BY S.ID").setString("plugin", plugin)
            .list();
    }

    @SuppressWarnings("unchecked")
    List<CollectionSummary> findMetricCountSummaries() {
        String sql = "SELECT COUNT(m.template_id) AS total, "
                     + "m.coll_interval/60000 AS coll_interval, "
                     + "t.name AS name, mt.name AS type "
                     + "FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, "
                     + "EAM_MONITORABLE_TYPE mt " + "WHERE m.template_id = t.id "
                     + " and t.monitorable_type_id=mt.id " + " and m.coll_interval > 0 "
                     + " and m.enabled = :enabled "
                     + "GROUP BY m.template_id, t.name, mt.name, m.coll_interval "
                     + "ORDER BY total DESC";
        List<Object[]> vals = getSession().createSQLQuery(sql).setBoolean("enabled", true).list();

        List<CollectionSummary> res = new ArrayList<CollectionSummary>(vals.size());

        for (Object[] v : vals) {
            java.lang.Number total = (java.lang.Number) v[0];
            java.lang.Number interval = (java.lang.Number) v[1];
            String metricName = (String) v[2];
            String resourceName = (String) v[3];

            res.add(new CollectionSummary(total.intValue(), interval.intValue(), metricName,
                resourceName));
        }
        return res;
    }

    /**
     * @see MeasurementManagerImpl#findAgentOffsetTuples()
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findAgentOffsetTuples() {
        String sql = "select a, p, s, meas from Agent a " + "join a.platforms p "
                     + "join p.platformType pt " + "join p.serversBag s "
                     + "join s.serverType st, " + "Measurement as meas "
                     + "join meas.template as templ " + "join templ.monitorableType as mt "
                     + "where " + "pt.plugin = 'system' " + "and templ.name = 'Server Offset' "
                     + "and meas.instanceId = s.id " + "and st.name = 'HQ Agent' ";

        return getSession().createQuery(sql).list();
    }

    /**
     * @see MeasurementManagerImpl#findNumMetricsPerAgent()
     */
    @SuppressWarnings("unchecked")
    Map<Agent, Long> findNumMetricsPerAgent() {
        String platSQL = "select a.id, count(m) from Agent a " + "join a.platforms p, "
                         + "Measurement as m " + "join m.template templ "
                         + "join templ.monitorableType monType " + "where "
                         + " monType.appdefType = '1' and m.instanceId = p.id "
                         + "and m.enabled = true " + "group by a";
        String serverSQL = "select a.id, count(m) from Agent a " + "join a.platforms p "
                           + "join p.serversBag s, " + "Measurement as m "
                           + "join m.template templ " + "join templ.monitorableType monType "
                           + "where " + " monType.appdefType = '2' and m.instanceId = s.id "
                           + "and m.enabled = true " + "group by a";
        String serviceSQL = "select a.id, count(m) from Agent a " + "join a.platforms p "
                            + "join p.serversBag s " + "join s.services v, " + "Measurement as m "
                            + "join m.template templ " + "join templ.monitorableType monType "
                            + "where " + " monType.appdefType = '3' and m.instanceId = v.id "
                            + "and m.enabled = true " + "group by a";
        String[] queries = { platSQL, serverSQL, serviceSQL };
        Map<Integer, Long> idToCount = new HashMap<Integer, Long>();

        for (int i = 0; i < queries.length; i++) {
            List<Object[]> tuples = getSession().createQuery(queries[i]).list();

            for (Object[] tuple : tuples) {
                Integer id = (Integer) tuple[0];
                java.lang.Number count = (java.lang.Number) tuple[1];
                Long curCount;

                curCount = idToCount.get(id);
                if (curCount == null) {
                    curCount = new Long(0);
                }
                curCount = new Long(curCount.longValue() + count.longValue());
                idToCount.put(id, curCount);
            }
        }

        Map<Agent, Long> res = new HashMap<Agent, Long>(idToCount.size());

        for (Map.Entry<Integer, Long> ent : idToCount.entrySet()) {
            Integer id = ent.getKey();
            Long count = ent.getValue();

            res.put(agentDao.findById(id), count);
        }
        return res;
    }

    int clearResource(Resource resource) {
        // need to do this one measurement at a time to avoid the whole EhCache
        // being cleared due to bulk updates
        List<Measurement> list = findByResource(resource);
        int count = 0;
        for (Measurement meas : list) {
            if (meas == null) {
                continue;
            }
            meas.setResource(null);
            count++;
        }
        return count;
    }

    /**
     * Find a list of Measurement ID's that are no longer associated with a
     * resource.
     * 
     * @return A List of Measurement ID's.
     */
    @SuppressWarnings("unchecked")
    List<Integer> findOrphanedMeasurements(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        String sql = "SELECT id FROM Measurement WHERE resource IS NULL";
        return getSession().createQuery(sql).setMaxResults(limit).list();
    }

    public Map<Integer, Collection<Measurement>> getMeasurementsByTemplateIds(Integer[] templIds) {
        final String hql = "from Measurement where template.id in (:ids)";
        @SuppressWarnings("unchecked")
        final List<Measurement> list =
            getSession().createQuery(hql)
                        .setParameterList("ids", templIds, new IntegerType())
                        .list();
        final Map<Integer, Collection<Measurement>> rtn =
            new HashMap<Integer, Collection<Measurement>>(list.size());
        for (final Measurement m : list) {
            final Integer templId = m.getTemplate().getId();
            Collection<Measurement> measurements = rtn.get(templId);
            if (measurements == null) {
                measurements = new ArrayList<Measurement>();
                rtn.put(templId, measurements);
            }
            measurements.add(m);
        }
        return rtn;
    }
    
    @SuppressWarnings("unchecked")
    public List<Integer> getMeasurementsNotInTemplateIds(Integer[] templIds, Resource resource) {
        if (null == resource) return Collections.emptyList();
        
        final String hql = "select id from Measurement m where m.resource = :res ";
        final String notClause = " and m.template.id not in (:ids)";                
        List<Integer> list;
        if (null == templIds) {
            log.info("No template Ids provided. Hence returning all measurements for the resource.");
            list = getSession().createQuery(hql)
            .setParameterList("ids", templIds, new IntegerType())
            .setParameter("res", resource)
            .list();
        }
        else { 
            list = getSession().createQuery(hql + notClause)
                        .setParameterList("ids", templIds, new IntegerType())
                        .setParameter("res", resource)
                        .list();
        }
        return list;
    }    
    

    public Map<Integer, Collection<Measurement>> getMeasurementsForInstanceByTemplateIds(Integer[] templIds, Resource resource) {
        if ((null == resource) || (null == templIds)) return Collections.emptyMap();
        final String hql = "select m from Measurement m " +
                      "where m.template.id in (:tids) and m.resource = :res";
        @SuppressWarnings("unchecked")
        final List<Measurement> list =
            getSession().createQuery(hql)
                        .setParameterList("tids", templIds, new IntegerType())
                        .setParameter("res", resource)                    
                        .list();
        final Map<Integer, Collection<Measurement>> rtn =
            new HashMap<Integer, Collection<Measurement>>(list.size());
        for (final Measurement m : list) {
            final Integer templId = m.getTemplate().getId();
            Collection<Measurement> measurements = rtn.get(templId);
            if (measurements == null) {
                measurements = new ArrayList<Measurement>();
                rtn.put(templId, measurements);
            }
            measurements.add(m);
        }
        return rtn;
    }    
}
