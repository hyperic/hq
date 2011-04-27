package org.hyperic.hq.measurement.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class MeasurementRepositoryImpl implements MeasurementRepositoryCustom {

    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" +
                                               MeasurementConstants.CAT_AVAILABILITY.toUpperCase() +
                                               "' ";

    private static final int BATCH_SIZE = 1000;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbcTemplate;

    private DialectAccessor dialectAccessor;

    @Autowired
    public MeasurementRepositoryImpl(JdbcTemplate jdbcTemplate, DialectAccessor dialectAccessor) {
        this.jdbcTemplate = jdbcTemplate;
        this.dialectAccessor = dialectAccessor;
    }

    public Measurement findAvailabilityMeasurementByResource(Integer resource) {
        final String sql = new StringBuilder().append("select m from Measurement m ")
            .append("join m.template t ").append("where m.resource=:resource AND ")
            .append(ALIAS_CLAUSE).toString();
        try {
            return entityManager.createQuery(sql, Measurement.class)
                .setParameter("resource", resource).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Measurement> findAvailabilityMeasurementsByGroupMembers(Set<Integer> groupMembers) {
        if (groupMembers.isEmpty()) {
            return new ArrayList<Measurement>(0);
        }
        String ql = "select m from  " + "Measurement m join m.template t " +
                    "where m.resource in (:resources) and " + ALIAS_CLAUSE;
        return entityManager.createQuery(ql, Measurement.class)
            .setParameter("resources", new ArrayList<Integer>(groupMembers))
            .setHint("org.hibernate.cacheable", true)
            .setHint("org.hibernate.cacheRegion", "Measurement.findAvailMeasurementsForGroup")
            .getResultList();
    }
    
    

    public List<Measurement> findAvailabilityMeasurementsByTemplatesAndResources(Integer[] templateIds,
                                                                                 Integer[] resourceIds) {
        final List<Integer> iidList = Arrays.asList(resourceIds);
        final List<Integer> tidList = Arrays.asList(templateIds);
        final String sql = new StringBuilder(256)
            .append("select m from Measurement m ").append(
            "join m.template t ").append("where m.resource in (:iids) AND t.id in (:tids) AND ")
            .append(ALIAS_CLAUSE).toString();
        final List<Measurement> rtn = new ArrayList<Measurement>(iidList.size());
        final int batch = BATCH_SIZE/2;
        for (int xx=0; xx<iidList.size(); xx+=batch) {
            final int iidEnd = Math.min(xx+batch, iidList.size());
            for (int yy=0; yy<tidList.size(); yy+=batch) {
                final int tidEnd = Math.min(yy+batch, tidList.size());
                rtn.addAll(entityManager.createQuery(sql,Measurement.class)
                    .setParameter("iids", iidList.subList(xx, iidEnd))
                    .setParameter("tids", tidList.subList(yy, tidEnd)).getResultList());
            }
        }
        return rtn;
    }

    public List<Measurement> findByResources(List<Integer> resources) {
        if (resources.isEmpty()) {
            return new ArrayList<Measurement>(0);
        }
        List<Measurement> measurements = new ArrayList<Measurement>();
        String ql = "select m from Measurement m " + "where m.resource in (:resources)";
        final TypedQuery<Measurement> query = entityManager.createQuery(ql, Measurement.class);
        final int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(size, i + BATCH_SIZE);
            final List<Integer> sublist = resources.subList(i, end);
            measurements.addAll(query.setParameter("resources", sublist).getResultList());
        }
        return measurements;
    }

    public List<Measurement> findByTemplatesAndResources(Integer[] templateIds,
                                                         Integer[] resourceIds, boolean onlyEnabled) {
        if (templateIds.length == 0 || resourceIds.length == 0) {
            return new ArrayList<Measurement>(0);
        }
        // sort to take advantage of query cache
        final List<Integer> resourceIdList = new ArrayList<Integer>(Arrays.asList(resourceIds));
        final List<Integer> templateIdList = new ArrayList<Integer>(Arrays.asList(templateIds));
        Collections.sort(templateIdList);
        Collections.sort(resourceIdList);
        final StringBuilder buf = new StringBuilder(32).append("select m from Measurement m ")
            .append("join m.template t ")
            .append("where m.resource in (:resources) AND t.id in (:templates)");
        if (onlyEnabled) {
            buf.append(" and enabled = :enabled");
        }
        final String sql = buf.toString();
        final List<Measurement> rtn = new ArrayList<Measurement>(resourceIdList.size());
        final int batch = BATCH_SIZE / 2;
        for (int xx = 0; xx < resourceIdList.size(); xx += batch) {
            final int iidEnd = Math.min(xx + batch, resourceIdList.size());
            for (int yy = 0; yy < templateIdList.size(); yy += batch) {
                final int tidEnd = Math.min(yy + batch, templateIdList.size());
                TypedQuery<Measurement> query = entityManager.createQuery(sql, Measurement.class)
                    .setParameter("resources", resourceIdList.subList(xx, iidEnd))
                    .setParameter("templates", templateIdList.subList(yy, tidEnd));
                if (onlyEnabled) {
                    query.setParameter("enabled", onlyEnabled);
                }
                rtn.addAll(query.setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion", "Measurement.findMeasurements")
                    .getResultList());
            }
        }
        return rtn;
    }

    public List<Measurement> findDesignatedByGroupAndCategoryOrderByTemplate(Set<Integer> groupMembers,
                                                                             String category) {
        if (groupMembers.isEmpty()) {
            return new ArrayList<Measurement>(0);
        }
        String sql = "select m from Measurement m join m.template t " + "join t.category c "
                     + "where m.resource in (:resources) "
                     + "and t.designate = true and c.name = :cat order by t.name";

        return entityManager.createQuery(sql, Measurement.class).setParameter("cat", category)
            .setParameter("resources", new ArrayList<Integer>(groupMembers))
            .setHint("org.hibernate.cacheable", true)
            .setHint("org.hibernate.cacheRegion", "Measurement.findDesignatedByCategoryForGroup")
            .getResultList();
    }

    public List<Measurement> findDesignatedByResourceAndCategory(Integer resource, String category) {
        return findDesignatedByResourcesAndCategory(Collections.singletonList(resource), category);
    }

    public List<Measurement> findDesignatedByResourcesAndCategory(List<Integer> resources,
                                                                  String category) {
        String sql = new StringBuilder(512).append("select m from Measurement m ")
            .append("join m.template t ").append("join t.category c ")
            .append("where m.resource in (:rids) and ").append("t.designate = true and ")
            .append("c.name = :cat").toString();
        int size = resources.size();
        List<Measurement> rtn = new ArrayList<Measurement>(size * 5);
        for (int i = 0; i < size; i = BATCH_SIZE) {
            int end = Math.min(size, i + BATCH_SIZE);
            rtn.addAll(entityManager.createQuery(sql, Measurement.class)
                .setParameter("rids", resources.subList(i, end)).setParameter("cat", category)
                .getResultList());
        }
        return rtn;
    }

    public List<Measurement> findEnabledByResourceGroupAndTemplate(Set<Integer> groupMembers,
                                                                   Integer templateId) {
        if (groupMembers.isEmpty()) {
            return new ArrayList<Measurement>(0);
        }
        String sql = "select m from Measurement m where m.resource in (:resources) "
                     + "and m.template.id = :template and m.enabled = true";

        return entityManager.createQuery(sql, Measurement.class)
            .setParameter("resources", new ArrayList<Integer>(groupMembers))
            .setParameter("template", templateId.intValue())
            .setHint("org.hibernate.cacheable", true)
            .setHint("org.hibernate.cacheRegion", "ResourceGroup.getMetricsCollecting")
            .getResultList();
    }

    public Map<Integer, List<Measurement>> findEnabledByResources(List<Integer> resources) {
        if (resources == null || resources.size() == 0) {
            return new HashMap<Integer, List<Measurement>>(0, 1);
        }
        final String ql = new StringBuilder(256).append("select m from Measurement m ")
            .append("where m.enabled = true and ").append("m.resource in (:rids) ").toString();
        final Map<Integer, List<Measurement>> rtn = new HashMap<Integer, List<Measurement>>();
        final TypedQuery<Measurement> query = entityManager.createQuery(ql, Measurement.class);
        final int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(size, i + BATCH_SIZE);
            final List<Integer> sublist = resources.subList(i, end);
            final List<Measurement> resultset = query.setParameter("rids", sublist).getResultList();
            for (final Measurement m : resultset) {
                final Integer r = m.getResource();
                if (r == null) {
                    continue;
                }
                List<Measurement> tmp = rtn.get(r);
                if (tmp == null) {
                    tmp = new ArrayList<Measurement>();
                    rtn.put(r, tmp);
                }
                tmp.add(m);
            }
        }
        return rtn;
    }

    public List<CollectionSummary> findMetricCountSummaries() {
        String sql = "SELECT COUNT(m.template_id) AS total, "
                     + "m.coll_interval/60000 AS coll_interval, "
                     + "t.name AS name, mt.name AS type "
                     + "FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, "
                     + "EAM_MONITORABLE_TYPE mt " + "WHERE m.template_id = t.id "
                     + " and t.monitorable_type_id=mt.id " + " and m.coll_interval > 0 "
                     + " and m.enabled = ? "
                     + "GROUP BY m.template_id, t.name, mt.name, m.coll_interval "
                     + "ORDER BY total DESC";
        List<CollectionSummary> summaries = jdbcTemplate.query(sql, new Object[] { true },
            new RowMapper<CollectionSummary>() {
                public CollectionSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new CollectionSummary(rs.getInt("total"), rs.getInt("coll_interval"), rs
                        .getString("name"), rs.getString("type"));
                }

            });
        return summaries;
    }

    public Map<Integer, List<Measurement>> findRelatedAvailabilityMeasurements(final Map<Integer, List<Integer>> parentToChildIds) {
        final String sql = new StringBuilder().append("select m from Measurement m ")
            .append("join m.template t ").append("where m.resource in (:childrenIds) and ")
            .append(ALIAS_CLAUSE).toString();
        final HQDialect dialect = dialectAccessor.getHQDialect();
        int max = Integer.MAX_VALUE;
        if (dialect.getMaxExpressions() > 0) {
            max = dialect.getMaxExpressions();
        }
        final Map<Integer, List<Measurement>> rtn = new HashMap<Integer, List<Measurement>>(
            parentToChildIds.size());
        for (Map.Entry<Integer, List<Integer>> entry : parentToChildIds.entrySet()) {
            List<Measurement> childMeasurements = new ArrayList<Measurement>();
            for (int i = 0; i < entry.getValue().size(); i += max) {
                final int end = Math.min(i + max, entry.getValue().size());
                final List<Integer> list = entry.getValue().subList(i, end);
                childMeasurements.addAll(entityManager
                    .createQuery(sql, Measurement.class)
                    .setParameter("childrenIds", list)
                    .setHint("org.hibernate.cacheable", true)
                    .setHint("org.hibernate.cacheRegion",
                        "Measurement.findRelatedAvailMeasurements").getResultList());
            }
            rtn.put(entry.getKey(), childMeasurements);
        }
        return rtn;
    }

    public int removeMeasurements(List<Measurement> measurements) {
        // need to do this one measurement at a time to avoid the whole EhCache
        // being cleared due to bulk updates
        int count = 0;
        for (Measurement meas : measurements) {
            if (meas == null) {
                continue;
            }
            meas.setResource(null);
            count++;
        }
        return count;
    }

}
