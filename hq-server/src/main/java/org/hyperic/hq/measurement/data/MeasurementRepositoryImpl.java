package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.server.session.Measurement;

public class MeasurementRepositoryImpl implements MeasurementRepositoryCustom {

    private static final int BATCH_SIZE = 1000;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Measurement> findByResources(List<Resource> resources) {
        List<Measurement> measurements = new ArrayList<Measurement>();
        String ql = "select m from Measurement m " + "where m.resource in (:resources)";
        final TypedQuery<Measurement> query = entityManager.createQuery(ql, Measurement.class);
        final int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(size, i + BATCH_SIZE);
            final List<Resource> sublist = resources.subList(i, end);
            measurements.addAll(query.setParameter("resources", sublist).getResultList());
        }
        return measurements;
    }

    public Map<Integer, List<Measurement>> findEnabledByResources(List<Resource> resources) {
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
            final List<Resource> sublist = resources.subList(i, end);
            final List<Measurement> resultset = query.setParameter("rids", sublist).getResultList();
            for (final Measurement m : resultset) {
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

    public List<Measurement> findDesignatedByResourcesAndCategory(List<Resource> resources,
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

    public List<Measurement> findDesignatedByResourceAndCategory(Resource resource, String category) {
        return findDesignatedByResourcesAndCategory(Collections.singletonList(resource), category);
    }

    public List<Measurement> findDesignatedByGroupAndCategoryOrderByTemplate(ResourceGroup group,
                                                                             String category) {
        String sql = "select m from Measurement m join m.template t " + "join t.category c "
                     + "where m.resource in (:resources) "
                     + "and t.designate = true and c.name = :cat order by t.name";

        return entityManager.createQuery(sql, Measurement.class).setParameter("cat", category)
            .setParameter("resources", new ArrayList<Resource>(group.getMembers()))
            .setHint("org.hibernate.cacheable", true)
            .setHint("org.hibernate.cacheRegion", "Measurement.findDesignatedByCategoryForGroup")
            .getResultList();
    }

}
