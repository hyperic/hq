package org.hyperic.hq.inventory.data;

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
     * Persists a new ResourceGroup
     * @param resourceGroup The new ResourceGroup
     */
    void persist(ResourceGroup resourceGroup);
}
