package org.hyperic.hq.measurement.data;

import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;

public interface MetricProblemRepositoryCustom {

    @Transactional
    int deleteByMetricIds(Collection<Integer> ids);
}
