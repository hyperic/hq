package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.jdbc.DBUtil;

public class MetricProblemRepositoryImpl implements MetricProblemRepositoryCustom {

    private final Log log = LogFactory.getLog(MetricProblemRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    public int deleteByMetricIds(Collection<Integer> ids) {
        String hql = "delete MetricProblem m where m.id.measurementId in (:ids)";

        int count = 0;
        for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
            ArrayList<Integer> subIds = new ArrayList<Integer>();

            for (int i = 0; i < DBUtil.IN_CHUNK_SIZE && it.hasNext(); i++) {
                subIds.add(it.next());
            }

            count += entityManager.createQuery(hql).setParameter("ids", subIds).executeUpdate();

            if (log.isDebugEnabled()) {
                log.debug("deleteByMetricIds() " + subIds.size() + " of " + ids.size() +
                          " metric IDs");
            }
        }

        return count;
    }

}
