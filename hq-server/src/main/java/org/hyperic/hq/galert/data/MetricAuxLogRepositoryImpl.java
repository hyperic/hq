package org.hyperic.hq.galert.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.springframework.beans.factory.annotation.Autowired;

public class MetricAuxLogRepositoryImpl implements MetricAuxLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private final Log log = LogFactory.getLog(MetricAuxLogRepositoryImpl.class);

    private DialectAccessor dialectAccessor;

    @Autowired
    public MetricAuxLogRepositoryImpl(DialectAccessor dialectAccessor) {
        this.dialectAccessor = dialectAccessor;
    }

    public int deleteByMetricIds(Collection<Integer> ids) {
        final String ql = "delete from MetricAuxLogPojo l where l.metric.id in (:ids)";
        HQDialect dialect = dialectAccessor.getHQDialect();
        int maxExprs;
        if (-1 == (maxExprs = dialect.getMaxExpressions())) {
            return entityManager.createQuery(ql).setParameter("ids", ids).executeUpdate();
        }
        int count = 0;
        ArrayList<Integer> subIds = new ArrayList<Integer>(maxExprs);
        for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
            subIds.clear();
            for (int i = 0; i < maxExprs && it.hasNext(); i++) {
                subIds.add(it.next());
            }
            count += entityManager.createQuery(ql).setParameter("ids", subIds).executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("deleteByMetricIds() " + subIds.size() + " of " + ids.size() +
                          " metric IDs");
            }
        }

        return count;
    }
}
