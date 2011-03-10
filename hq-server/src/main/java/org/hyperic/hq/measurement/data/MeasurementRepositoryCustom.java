package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.transaction.annotation.Transactional;

public interface MeasurementRepositoryCustom {

    @Transactional(readOnly = true)
    List<Measurement> findByResources(List<Resource> resources);

    @Transactional(readOnly = true)
    Map<Integer, List<Measurement>> findEnabledByResources(List<Resource> resources);

    @Transactional(readOnly = true)
    List<Measurement> findDesignatedByResourcesAndCategory(List<Resource> resources, String category);

    @Transactional(readOnly = true)
    List<Measurement> findDesignatedByResourceAndCategory(Resource resource, String category);

    @Transactional(readOnly = true)
    List<Measurement> findDesignatedByGroupAndCategoryOrderByTemplate(ResourceGroup group,
                                                                      String category);
}
