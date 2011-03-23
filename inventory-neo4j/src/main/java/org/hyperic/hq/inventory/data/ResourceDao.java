package org.hyperic.hq.inventory.data;

import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 
 * Repository for lookup of {@link Resource}s
 * @author jhickey
 * @author dcruchfield
 * 
 */
public interface ResourceDao extends GenericDao<Resource> {
    /**
     * 
     * @param propertyName The name of the property. The property must be
     *        indexed for lookup to succeed (set indexed to true on
     *        PropertyType)
     * @param propertyValue The value to search for
     * @param pageInfo Info on paging and sorting
     * @return A paged list of Resource search results
     */
    Page<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                             Pageable pageInfo);

    /**
     * 
     * @return The root Resource to use when making associations (for traversal
     *         of rootless objs)
     */
    Resource findRoot();

    /**
     * Saves changes to an existing Resource
     * @param resource The changed Resource
     * @return The merged Resource
     */
    Resource merge(Resource resource);

    /**
     * Persists a new Resource
     * @param resource The new resource
     */
    void persist(Resource resource);
}