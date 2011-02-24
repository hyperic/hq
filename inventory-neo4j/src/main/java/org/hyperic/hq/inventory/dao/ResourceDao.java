package org.hyperic.hq.inventory.dao;

import java.util.List;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.util.pager.PageList;

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
    PageList<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                             PageInfo pageInfo);

    /**
     * 
     * @param owner The user who owns the Resource
     * @return Resources owned by the specified user
     */
    List<Resource> findByOwner(AuthzSubject owner);

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