package org.hyperic.hq.measurement.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface MeasurementRepositoryCustom {

    Measurement findAvailabilityMeasurementByResource(Resource resource);

    List<Measurement> findAvailabilityMeasurementsByGroup(ResourceGroup group);

    List<Measurement> findAvailabilityMeasurementsByResources(Collection<Resource> resources);

    List<Measurement> findByResources(List<Resource> resources);

    List<Measurement> findByTemplatesAndResources(Integer[] templateIds, Integer[] resourceIds,
                                                  boolean onlyEnabled);

    List<Measurement> findDesignatedByGroupAndCategoryOrderByTemplate(ResourceGroup group,
                                                                      String category);

    List<Measurement> findDesignatedByResourceAndCategory(Resource resource, String category);

    List<Measurement> findDesignatedByResourcesAndCategory(List<Resource> resources, String category);

    List<Measurement> findEnabledByResourceGroupAndTemplate(ResourceGroup group, Integer templateId);

    /**
     * @param {@link List} of {@link Resource}s
     * @return {@link Map} of {@link Integer} representing resourceId to
     *         {@link List} of {@link Measurement}s
     */
    Map<Integer, List<Measurement>> findEnabledByResources(List<Resource> resources);

    List<CollectionSummary> findMetricCountSummaries();

    /**
     * @param {@link Map of resource id to a {@link List} of related resource
     *        Ids
     * @return {@link Map of resource id to a {@link List} of Availability
     *         {@link Measurement}s Measurements of the related resources
     */
    Map<Integer, List<Measurement>> findRelatedAvailabilityMeasurements(final Map<Integer, List<Integer>> parentToChildIds);

    /**
     * Marks a Measurement for removal (by setting its Resource to null)
     * @param measurements The measurements to remove
     * @return The number of measurements marked for removal
     */
    @Transactional
    int removeMeasurements(List<Measurement> measurements);

}
