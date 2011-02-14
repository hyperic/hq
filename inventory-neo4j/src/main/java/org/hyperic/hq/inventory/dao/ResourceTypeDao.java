package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.ResourceType;

/**
 * 
 * Repository for lookup of {@link ResourceType}s
 * @author jhickey
 * @author dcruchfield
 * 
 */
public interface ResourceTypeDao extends GenericDao<ResourceType> {

    /**
     * 
     * @return The root ResourceType to use when making associations (for
     *         traversal of rootless objs)
     */
    ResourceType findRoot();

    /**
     * Saves changes to an existing ResourceType
     * @param resourceType The changed ResourceType
     * @return The merged ResourceType
     */
    ResourceType merge(ResourceType resourceType);

    /**
     * Persists a new ResourceType
     * @param resourceType The new resource type
     */
    void persist(ResourceType resourceType);
}
