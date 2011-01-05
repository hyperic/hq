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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ResourceManager.
 */
public interface ResourceManager {
    
    /**
     * Check if there are any resources of a given type
     * 
     * 
     */
    boolean resourcesExistOfType(String typeName);
    /**
     * Find the type that has the given name.
     * @param name The name of the type you're looking for.
     * @return The value-object of the type of the given name.
     * @throws NotFoundException Unable to find a given or dependent entities.
     */
    public ResourceType findResourceTypeByName(String name);

    ResourceType findResourceTypeById(Integer id);

    /**
     * @param {@link Collection} of {@link Resource}s
     * 
     */
    public void resourceHierarchyUpdated(AuthzSubject subj, Collection<Resource> resources);

    /**
     * Find's the root (id=0) resource
     */
    public Resource findRootResource();

    ResourceType findRootResourceType();

    public Resource findResourceById(Integer id);

    Resource findResourceByName(String name);

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

    // TODO remove this method - ResourceManager (if kept) should have not
    // knowledge of AppdefEntityIDs
    Resource findResource(AppdefEntityID entityID);

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     */
    public Collection<Resource> findResourceByOwner(AuthzSubject owner);

    public boolean isResourceChildOf(Resource parent, Resource child);

}
