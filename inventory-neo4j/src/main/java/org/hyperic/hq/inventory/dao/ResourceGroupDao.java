package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.ResourceGroup;

/**
 * 
 * Repository for lookup of {@link ResourceGroup}s
 * @author jhickey
 * @author dcruchfield
 * 
 */
public interface ResourceGroupDao extends GenericDao<ResourceGroup> {

    /**
     * Saves changes to an existing ResourceGroup
     * @param resourceGroup The changed ResourceGroup
     * @return The merged ResourceGroup
     */
    ResourceGroup merge(ResourceGroup resourceGroup);

    /**
     * Persists a new ResourceGroup
     * @param resourceGroup The new ResourceGroup
     */
    void persist(ResourceGroup resourceGroup);
}
