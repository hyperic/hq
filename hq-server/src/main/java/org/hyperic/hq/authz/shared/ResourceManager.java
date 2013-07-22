/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.authz.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceRelation;
import org.hyperic.hq.authz.server.session.ResourceSortField;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.bizapp.server.session.ResourceCleanupEventListener;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.product.Plugin;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ResourceManager.
 */
public interface ResourceManager {
    /**
     * Find the type that has the given name.
     * @param name The name of the type you're looking for.
     * @return The value-object of the type of the given name.
     * @throws NotFoundException Unable to find a given or dependent entities.
     */
    public ResourceType findResourceTypeByName(String name) throws NotFoundException;

    /**
     * Find a resource, acting as a resource prototype.
     */
    public Resource findResourcePrototypeByName(String name);

    /**
     * Check if there are any resources of a given type
     */
    public boolean resourcesExistOfType(String typeName);

    /**
     * @param {@link Collection} of {@link Resource}s
     * 
     */
    public void resourceHierarchyUpdated(AuthzSubject subj, Collection<Resource> resources);

    /**
     * Create a resource.
     */
    public Resource createResource(AuthzSubject owner, ResourceType rt, Resource prototype,
                                   Integer instanceId, String name, boolean system, Resource parent);

    /**
     * Move a resource. It is the responsibility of the caller (AppdefManager)
     * to ensure that this resource can be moved to the destination. It's also
     * of note that this method only deals with relinking resource edges to the
     * ancestors of the destination resource. This means that in the case of
     * Server moves, it's up to the caller to re-link dependent chilren.
     */
    public void moveResource(AuthzSubject owner, Resource target, Resource destination);

    /**
     * Get the # of resources within HQ inventory
     */
    public Number getResourceCount();

    /**
     * Get the # of resource types within HQ inventory
     */
    public Number getResourceTypeCount();

    /**
     * Get the Resource entity associated with this ResourceType.
     * @param type This ResourceType.
     */
    public Resource getResourceTypeResource(Integer typeId);

    /**
     * Find the Resource that has the given instance ID and ResourceType.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     */
    public Resource findResourceByInstanceId(ResourceType type, Integer instanceId);

    public Resource findResourceByInstanceId(Integer typeId, Integer instanceId);

    /**
     * Find's the root (id=0) resource
     */
    public Resource findRootResource();

    /**
     * When using this method make sure that you know the Resource exists or else it will throw
     * a {@link ObjectNotFoundException} when calling resource.getSomeProperty()
     * If you are unsure, then use getResourceById()
     * @return {@link Resource} proxy
     * @see #getResourceById(Integer)
     */
    public Resource findResourceById(Integer id);

    /**
     * Find the Resource that has the given instance ID and ResourceType name.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     */
    public Resource findResourceByTypeAndInstanceId(String type, Integer instanceId);

    public Resource findResource(AppdefEntityID aeid);

    public Resource findResourcePrototype(AppdefEntityTypeID id);

    /**
     * Removes the specified resource by nulling out its resourceType. Will not
     * null the resourceType of the resource which is passed in. These resources
     * need to be cleaned up eventually by
     * {@link ResourceCleanupEventListener.removeDeletedResources}. This may be done in the
     * background via zevent by issuing a {@link ResourcesCleanupZevent}.
     * @see {@link ResourceCleanupEventListener.removeDeletedResources}
     * @see {@link ResourcesCleanupZevent}
     * @param r {@link Resource} resource to be removed.
     * @param nullResourceType tells the method to null out the resourceType
     * @param removeAllVirtual tells the method to remove all resources, including
     *        associated platforms, under the virtual resource hierarchy
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     */
    public AppdefEntityID[] removeResourceAndRelatedResources(AuthzSubject subj, Resource r,
                                                boolean nullResourceType,
                                                boolean removeAllVirtual) throws VetoException,
        PermissionException;

    public void removeResource(AuthzSubject subject, Resource r) throws VetoException;

    public void setResourceOwner(AuthzSubject whoami, Resource resource, AuthzSubject newOwner)
        throws PermissionException;

    /**
     * Get all the resource types
     * @param subject
     * @param pc Paging information for the request
     */
    public List<ResourceType> getAllResourceTypes(AuthzSubject subject, PageControl pc);

    /**
     * Get viewable resources by "type" OR "resource name"
     * @param subject
     * @return Map of resource values
     */
    public PageList<Resource> findViewables(AuthzSubject subject, String searchFor, PageControl pc);

    /**
     * Get viewable resources either by "type" OR "resource name" OR
     * "type AND resource name".
     * @param subject
     * @return Map of resource values
     */
    public Map<String, Collection<Integer>> findAllViewableInstances(AuthzSubject subject,
                                                                     Collection<ResourceType> types);

    /**
     * Find all the resources which are descendents of the given resource
     */
    public List<Resource> findResourcesByParent(AuthzSubject subject, Resource res);

    /**
     * Find all the resources which are direct descendants of the given resource.
     * In resource edge terminology, distance = 1.
     */
    public List<Resource> findChildren(AuthzSubject subject, Resource res);

    /**
     * Find all the resources of an authz resource type
     * @param resourceType 301 for platforms, etc.
     * @param pInfo A pager, using a sort field of {@link ResourceSortField}
     * @return a list of {@link Resource}s
     */
    public List<Resource> findResourcesOfType(int resourceType, PageInfo pInfo);

    /**
     * Find all the resources which have the specified prototype
     * @return a list of {@link Resource}s
     */
    public List<Resource> findResourcesOfPrototype(Resource proto, PageInfo pInfo);

    /**
     * Get all resources which are prototypes of platforms, servers, and
     * services and have a resource of that type in the inventory.
     */
    public List<Resource> findAppdefPrototypes();

    /**
     * Get all resources which are prototypes of platforms, servers, and
     * services.
     */
    public List<Resource> findAllAppdefPrototypes();

    /**
     * Find all the resources by plugin
     * @return a list of {@link Resource}s
     * 
     */
    public List<Resource> findResourcesByPlugin(String pluginName);
    
    /**
     * Get viewable service resources. Service resources include individual
     * cluster unassigned services as well as service clusters.
     * @param subject
     * @param pc control
     * @return PageList of resource values
     */
    public PageList<Resource> findViewableSvcResources(AuthzSubject subject, String resourceName,
                                                       PageControl pc);

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     */
    public Collection<Resource> findResourceByOwner(AuthzSubject owner);

    Collection<ResourceEdge> findResourceEdges(ResourceRelation relation, List<Resource> parentList);

    public Collection<ResourceEdge> findResourceEdges(ResourceRelation relation, Resource parent);

    public boolean isResourceChildOf(Resource parent, Resource child);

    public boolean hasChildResourceEdges(Resource resource, ResourceRelation relation);

    public int getDescendantResourceEdgeCount(Resource resource, ResourceRelation relation);

    public Collection<ResourceEdge> findChildResourceEdges(Resource resource,
                                                           ResourceRelation relation);

    public Collection<ResourceEdge> findDescendantResourceEdges(Resource resource,
                                                                ResourceRelation relation);

    public Collection<ResourceEdge> findAncestorResourceEdges(Resource resource,
                                                              ResourceRelation relation);

    public Collection<ResourceEdge> findResourceEdgesByName(String name, ResourceRelation relation);

    public ResourceEdge getParentResourceEdge(Resource resource, ResourceRelation relation);

    public boolean hasResourceRelation(Resource resource, ResourceRelation relation);

    public List<ResourceEdge> findResourceEdges(ResourceRelation relation, Integer resourceId,
                                                List<Integer> platformTypeIds, String platformName);

    public void createResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children)
        throws PermissionException, ResourceEdgeCreateException;

    public void createResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children,
                                    boolean deleteExisting) throws PermissionException,
        ResourceEdgeCreateException;

    public void removeResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children)
        throws PermissionException;

    public void removeResourceEdges(AuthzSubject subject, ResourceRelation relation, Resource parent)
        throws PermissionException;

    public ResourceRelation getContainmentRelation();

    ResourceRelation getNetworkRelation();

    void removeAuthzResource(AuthzSubject subject, AppdefEntityID aeid, Resource r)
        throws PermissionException, VetoException;

    public String getAppdefEntityName(AppdefEntityID appEnt);
    
    /**
     * @return the resource count with prototype of
     *         {@link AuthzConstants.authzPlatform} minus resources with the
     *         prototype of
     *         {@link AuthConstants.platformPrototypeVmwareVsphereVm}
     */
    public int getPlatformCountMinusVsphereVmPlatforms();
    
    ResourceRelation getVirtualRelation();

    public ResourceType findResourceTypeById(Integer authzplatform);

    public Collection<Resource> getUnconfiguredResources();

    Collection<Integer> findAllViewableResourceIds(AuthzSubject subject,
                                                   Collection<ResourceType> resourceTypes);

    ResourceType getResourceTypeById(Integer resourceTypeId);

    /**
     * Fetches all types defined by values and deletes the associated {@link Resource}s and
     * types via the asynchronous delete mechanism
     * @param subj {@link AuthzSubject}
     * @param values {@link Collection} of {@link MonitorableType}s
     */
    public void removeResourcesAndTypes(AuthzSubject subj, Collection<MonitorableType> values);

    /**
     * Deletes the ResourceTypes where the type names are defined by typeNames
     * @param typeNames
     */
    public void removeResourceTypes(Collection<String> typeNames);

    /**
     * @return {@link Resource} or null if it doesn't exist
     */
    public Resource getResourceById(Integer id);

    /**
     * @return {@link Map} of {@link String} {@link Plugin}.getName() to a count
     */
    public Map<String, Long> getResourceCountByPlugin(Collection<Plugin> plugins);

    public Collection<Resource> getParentResources(AuthzSubject subj, ResourceGroup group,
                                                   ResourceRelation relation, int distance,
                                                   int maxdistance);

    public Collection<Resource> getParentResources(AuthzSubject subj, Resource resource, ResourceRelation relation);

    /**
     * Finds all descendant resources from the EAM_RESOURCE_EDGE table.  This method will not work with {@link Resource}s
     * of type {@link ResourceGroup}.
     * @param proto may be null
     * @param children if true will fetch all the child resources (distance = 1), if false will fetch all parent
     * resources (distance = -1)
     */
    public Collection<Resource> getDescendantResources(AuthzSubject subj, Collection<Resource> resources,
                                                       ResourceRelation relation, Resource proto, boolean children);

    /**
     * @param proto {@link Resource} representing a {@link Resource} prototype
     * @param regex {@link String} representing a regular expression, may be null
     * @return All {@link Resource}s associated with the proto and optional regex
     */
    public Collection<Resource> getResourcesByPrototypeAndRegex(Resource proto, String regex);

   /**
     * Returns the associated children which are have a distance = 1 in the ResourceEdge mapping from the parent
     * resources
     * @param resources {@link List} of parent {@link Resource}s
     * @param viewableResourceIds {@link Set} of {@link Resource}Ids.  Ideally the {@link PermissionManager}
     * would provide these values.
     * @param includeSystemResources include child resources which are system resources.  This is mainly for platform
     * services which have a system server resource as a placeholder in the hierarchy
     * Return {@link Map} will only include resources which are in this {@link Set}
     * @return {@link Map} of {@link Resource} to its children represented by {@link Collection} of {@link Resource}s.
     * The returned {@link Resource} objects will be a subset of the includes.
     */
    public Map<Resource, Collection<Resource>> findChildResources(List<Resource> resources,
                                                                  Set<Integer> viewableResourceIds,
                                                                  boolean includeSystemResources);
    
    
    public Collection<ResourceType> getPSSTypes();

    public Collection<Resource> getOrphanedResources();

    public Collection<Resource> getRemovableChildren(AuthzSubject subject, Resource parent);
}
