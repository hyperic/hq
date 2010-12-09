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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceSortField;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.inventory.domain.ResourceTypeRelation;
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
    public Resource createResource(AuthzSubject owner, ResourceType rt, String name, boolean system, Resource parent);

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
     * Find's the root (id=0) resource
     */
    public Resource findRootResource();

    public Resource findResourceById(Integer id);


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
     * Get viewable resources either by "type" OR "resource name" OR
     * "type AND resource name".
     * @param subject
     * @return Map of resource values
     */
    public List<Integer> findViewableInstances(AuthzSubject subject, String typeName,
                                               String resName, String appdefTypeStr,
                                               Integer typeId, PageControl pc);

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
    public Map<String, List<Integer>> findAllViewableInstances(AuthzSubject subject);

    /**
     * Find all the resources which are descendents of the given resource
     */
    public List<Resource> findResourcesByParent(AuthzSubject subject, Resource res);

    /**
     * Find all the resources of an authz resource type
     * @param resourceType 301 for platforms, etc.
     * @param pInfo A pager, using a sort field of {@link ResourceSortField}
     * @return a list of {@link Resource}s
     */
    public List<Resource> findResourcesOfType(int resourceType, PageInfo pInfo);
    
    //TODO remove this method - ResourceManager (if kept) should have not knowledge of AppdefEntityIDs
    Resource findResource(AppdefEntityID entityID);

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     */
    public Collection<Resource> findResourceByOwner(AuthzSubject owner);

    Collection<ResourceRelation> findResourceEdges(ResourceTypeRelation relation, List<Resource> parentList);

    public Collection<ResourceRelation> findResourceEdges(ResourceTypeRelation relation, Resource parent);

    public boolean isResourceChildOf(Resource parent, Resource child);

    public boolean hasChildResourceEdges(Resource resource, ResourceTypeRelation relation);

    public int getDescendantResourceEdgeCount(Resource resource, ResourceTypeRelation relation);

    public Collection<ResourceRelation> findChildResourceEdges(Resource resource,
                                                           ResourceTypeRelation relation);

    public Collection<ResourceRelation> findDescendantResourceEdges(Resource resource,
                                                                ResourceTypeRelation relation);

    public Collection<ResourceRelation> findAncestorResourceEdges(Resource resource,
                                                              ResourceTypeRelation relation);

    public Collection<ResourceRelation> findResourceEdgesByName(String name, ResourceTypeRelation relation);

    public ResourceRelation getParentResourceEdge(Resource resource, ResourceTypeRelation relation);

    public boolean hasResourceTypeRelation(Resource resource, ResourceTypeRelation relation);


    public void removeResourceEdges(AuthzSubject subject, ResourceTypeRelation relation, Resource parent)
        throws PermissionException;

    public ResourceTypeRelation getContainmentRelation();


}
