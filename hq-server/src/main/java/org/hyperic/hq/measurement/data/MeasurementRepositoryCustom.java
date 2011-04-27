package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface MeasurementRepositoryCustom {

    Measurement findAvailabilityMeasurementByResource(Integer resource);

    List<Measurement> findAvailabilityMeasurementsByGroupMembers(Set<Integer> groupMembers);
    
    List<Measurement> findAvailabilityMeasurementsByTemplatesAndResources(Integer[] templateIds, Integer[] resourceIds);

    List<Measurement> findByResources(List<Integer> resources);

    List<Measurement> findByTemplatesAndResources(Integer[] templateIds, Integer[] resourceIds,
                                                  boolean onlyEnabled);

    List<Measurement> findDesignatedByGroupAndCategoryOrderByTemplate(Set<Integer> groupMembers,
                                                                      String category);

    List<Measurement> findDesignatedByResourceAndCategory(Integer resource, String category);

    List<Measurement> findDesignatedByResourcesAndCategory(List<Integer> resources, String category);

    List<Measurement> findEnabledByResourceGroupAndTemplate(Set<Integer> groupMembers, Integer templateId);

    /**
     * @param {@link List} of Resource IDs
     * @return {@link Map} of {@link Integer} representing resourceId to
     *         {@link List} of {@link Measurement}s
     */
    Map<Integer, List<Measurement>> findEnabledByResources(List<Integer> resources);

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
