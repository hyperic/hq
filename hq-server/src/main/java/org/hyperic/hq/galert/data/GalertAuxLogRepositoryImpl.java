package org.hyperic.hq.galert.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.galerts.server.session.GalertAuxLogProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class GalertAuxLogRepositoryImpl implements GalertAuxLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private DialectAccessor dialectAccessor;

    @Autowired
    public GalertAuxLogRepositoryImpl(DialectAccessor dialectAccessor) {
        this.dialectAccessor = dialectAccessor;
    }

    public void resetAuxType(Collection<Integer> measurementIds) {
        String ql = "update GalertAuxLog g set g.auxType = :type "
                    + "where exists (select p.id from MetricAuxLogPojo p "
                    + "where p.auxLog = g and " + "p.metric.id in (:metrics))";

        HQDialect dialect = dialectAccessor.getHQDialect();
        int maxExprs;
        if (-1 == (maxExprs = dialect.getMaxExpressions())) {
            entityManager.createQuery(ql)
                .setParameter("type", GalertAuxLogProvider.INSTANCE.getCode())
                .setParameter("metrics", measurementIds).executeUpdate();
            return;
        }
        int i = 0;
        ArrayList<Integer> metrics = new ArrayList<Integer>(maxExprs);
        for (Iterator<Integer> it = measurementIds.iterator(); it.hasNext(); i++) {
            if (i != 0 && (i % maxExprs) == 0) {
                metrics.add(it.next());
                entityManager.createQuery(ql)
                    .setParameter("type", GalertAuxLogProvider.INSTANCE.getCode())
                    .setParameter("metrics", metrics).executeUpdate();
                metrics.clear();
            } else {
                metrics.add(it.next());
            }
        }
    }
}
