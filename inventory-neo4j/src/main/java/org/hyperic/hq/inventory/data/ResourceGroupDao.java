package org.hyperic.hq.inventory.data;

import java.util.List;

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
     * 
     * @param propertyName The name of the property. The property must be
     *        indexed for lookup to succeed (set indexed to true on
     *        PropertyType)
     * @param propertyValue The value to search for
     * @return A list of ResourceGroup search results
     */
    List<ResourceGroup> findByIndexedProperty(String propertyName, Object propertyValue);

    /**
     * Persists a new ResourceGroup
     * @param resourceGroup The new ResourceGroup
     */
    void persist(ResourceGroup resourceGroup);
}
