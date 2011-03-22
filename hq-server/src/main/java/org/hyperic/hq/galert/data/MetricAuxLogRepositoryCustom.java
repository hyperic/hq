package org.hyperic.hq.galert.data;

import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MetricAuxLogRepositoryCustom {

    int deleteByMetricIds(Collection<Integer> ids);

}
