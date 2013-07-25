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

package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.appdef.shared.PlatformNotFoundException
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.ResourceManagerImpl
import org.hyperic.hq.authz.server.session.ResourceRelation;
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.bizapp.server.session.AppdefBossImpl as AppdefBoss
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.shared.AppdefEntityID
class ResourceHelper extends BaseHelper {
    private rman = Bootstrap.getBean(ResourceManager.class)
    private groupMan = Bootstrap.getBean(ResourceGroupManager.class)
    private platMan = Bootstrap.getBean(PlatformManager.class)
    
    ResourceHelper(AuthzSubject user) {
        super(user)
    }
    
    /**
     * General purpose utility method for finding resources and resource
     * counts.  View-Permission checking is performed on the resource 
     * (throwing PermissionException if denied) unless the permCheck = false 
     *
     * This method generally returns a {@link Resource}
     *
     * To find the counts of resource types:  (no perm checking done)
     *   find count:'platforms'
     *   find count:'servers'
     *   find count:'services'
     *
     * Since servers and services do not have unique names, you must qualify
     * them by their hosting resources.
     *
     * To find platforms: 
     *   find platform:'My Platform', permCheck:false
     *   find platform:10001  // find the platform by ID
     *   find byFqdn:'platformFqdn'
     *
     * To find servers:
     *   find platform:'My Platform', server:'My Server'
     *   find server:serverId
     *
     * To find services:
     *   find platform:'My Platform', server:'My Server', service:'My Service'
     *   find service:serviceId
     *   find server:10001, service:'My Service'
     *
     * Additional arguments are possible, See: AppdefCategory.checkPerms
     *
     * TODO: 
     *  Currently, this does not take permissions into account when returning
     *  the count:* methods
     */
    def find(Map args) {
        args = args + [:]  // Don't modify caller's map 
        // Initialize all used arguments to null
        ['count', 'platform', 'server', 'service',
                'byPrototype', 'prototype', 'withPaging',
                'byFqdn', 'byPlugin',].each {args.get(it, null)}
        args.get('user', user)         // Use default user  
        args.get('operation', 'view')  // Default permission required
        
        if (args.count != null) {
            switch (args.count) {
                case 'platforms': return platMan.platformCount
                case 'servers':   return Bootstrap.getBean(ServerManager.class).serverCount
                case 'services':  return Bootstrap.getBean(ServiceManager.class).serviceCount
                case 'cpus': return platMan.cpuCount
                case 'applications': return Bootstrap.getBean(ApplicationManager.class).applicationCount
                case 'roles': return Bootstrap.getBean(RoleManager.class).roleCount
                case 'users': return Bootstrap.getBean(RoleManager.class).subjectCount
                case 'alerts': return Bootstrap.getBean(AlertManager.class).alertCount
                case 'alertDefs': return Bootstrap.getBean(AlertDefinitionManager.class).activeCount
                case 'resources': return rman.resourceCount
                case 'resourceTypes': return rman.findAppdefPrototypes().size()
                case 'groups': return groupMan.groupCount
                case 'escalations': return Bootstrap.getBean(EscalationManager.class).escalationCount
                case 'activeEscalations': return Bootstrap.getBean(EscalationManager.class).activeEscalationCount
                default:
                    throw new IllegalArgumentException("count ${args.count} must specify a " + 
                    "valid resource type")
            }
        }
        
        if (args.prototype) {
            return findPrototype(args)
        }
        
        if (args.byPrototype) {
            return findByPrototype(args)
        }
        
        if (args.byFqdn) {
            return findByFqdn(args.byFqdn)
        }
        
        if (args.byPlugin) {
        	return findByPlugin(args.byPlugin)
        }
        
        def plat
        if (args.platform != null) {
            if (args.platform in String) {
                plat = platMan.getPlatformByName(args.platform)
            } else {
                try { 
                    plat = platMan.findPlatformById(args.platform as int)
                } catch(PlatformNotFoundException e) {
                }
            }
            
            if (args.server == null && args.service == null)
                return plat?.checkPerms(args)
        }
        
        def server
        if (args.server != null) {
            if (args.server in String) {
                if (!plat)
                    throw new IllegalArgumentException('Requisite platform ' + 
                    'not found')
                server = Bootstrap.getBean(ServerManager.class).getServerByName(plat, args.server)
            } else {
                server = Bootstrap.getBean(ServerManager.class).getServerById(args.server as int)
            }
            
            if (args.service == null)
                return server?.checkPerms(args)
        }
        
        if (args.service != null) {
            def service
            if (args.service in String) {
                if (server) {
                    service = Bootstrap.getBean(ServiceManager.class).getServiceByName(server, args.service)
                }
                else if (plat) {
                    service = Bootstrap.getBean(ServiceManager.class).getServiceByName(plat, args.service)
                }
            } else {
                service = Bootstrap.getBean(ServiceManager.class).getServiceById(args.service as int)
            }
            
            return service?.checkPerms(args)
        }
        
        throw new IllegalArgumentException('Unknown arguments passed to find()')
    }
    
    public Resource findById(id) {
        rman.findResourceById(id)
    }
    
    private Resource findByFqdn(fqdn) {
        try {
            def plat = platMan.findPlatformByFqdn(user, fqdn)
            return plat.resource
        } catch (PlatformNotFoundException e) {
            return null
        }
    }

    private List findByPlugin(pluginName) {
        rman.findResourcesByPlugin(pluginName)
    }
        
    private Resource findPrototype(Map args) {
        rman.findResourcePrototypeByName(args.prototype)
    }
    
    private List findByPrototype(Map args) {
        def proto = args.byPrototype
        
        if (proto in String) {
            proto = rman.findResourcePrototypeByName(proto)
            if (proto == null) {
                return []  // Correct?  We don't have a proto
            }
        } // else we assume it's already a Resource
        
        def pageInfo = args.withPaging
        if (!pageInfo) {
            pageInfo = PageInfo.getAll(ResourceSortField.NAME, true)
        }
        rman.findResourcesOfPrototype(proto, pageInfo)
    }
    
    /**
     * Find a subset of all platforms
     *
     * @param pInfo a pager, using ResourceSortField for sorting
     *
     * @return a List of {@link Resource}s 
     */
    List findPlatforms(PageInfo pInfo)  {
        rman.findResourcesOfType(AuthzConstants.authzPlatform, pInfo)
    }
    
    /**
     * Find all platforms, sorted by name
     *
     * @return a List of {@link Resource}s
     */
    List findAllPlatforms() {
        findPlatforms(PageInfo.getAll(ResourceSortField.NAME, true))
    }
    
    /**
     * Find a subset of all servers
     *
     * @param pInfo a pager, using ResourceSortField for sorting
     *
     * @return a List of {@link Resource}s 
     */
    List findServers(PageInfo pInfo)  {
        rman.findResourcesOfType(AuthzConstants.authzServer, pInfo)
    }
    
    /**
     * Find all servers, sorted by name
     *
     * @return a List of {@link Resource}s
     */
    List findAllServers() {
        findServers(PageInfo.getAll(ResourceSortField.NAME, true))
    }
    
    /**
     * Find a subset of all services
     *
     * @param pInfo a pager, using ResourceSortField for sorting
     *
     * @return a List of {@link Resource}s 
     */
    List findServices(PageInfo pInfo)  {
        rman.findResourcesOfType(AuthzConstants.authzService, pInfo)
    }
    
    /**
     * Find all services, sorted by name
     *
     * @return a List of {@link Resource}s
     */
    List findAllServices() {
        findServices(PageInfo.getAll(ResourceSortField.NAME, true))
    }
    
    /**
     * Find a subset of all applications
     *
     * @param pInfo a pager, using ResourceSortField for sorting
     *
     * @return a List of {@link Resource}s 
     */
    List findApplications(PageInfo pInfo)  {
        rman.findResourcesOfType(AuthzConstants.authzApplication, pInfo)
    }
    
    /**
     * Find all applications, sorted by name
     *
     * @return a List of {@link Resource}s
     */
    List findAllApplications() {
        findApplications(PageInfo.getAll(ResourceSortField.NAME, true))
    }
    
    /**
     * Find the descendant edges of a resource
     *
     * @return a Collection of {@link ResourceEdge}s
     */
    Collection findResourceEdges(String resourceRelation, Resource parent) {
        def edges = []
        if (resourceRelation.equals(AuthzConstants.ResourceEdgeNetworkRelation)) {
            edges = rman.findResourceEdges(rman.getNetworkRelation(), parent)
        } else if (resourceRelation.equals(AuthzConstants.ResourceEdgeContainmentRelation)) {
            edges = rman.findResourceEdges(rman.getContainmentRelation(), parent)
        }
        return edges
    }
    
    /**
     * Find the descendant edges of a resource
     *
     * @return a Collection of {@link ResourceEdge}s
     */
    Collection findResourceEdges(String resourceRelation, List platformTypeIds, String platformName) {
        def edges = []
        if (resourceRelation == AuthzConstants.ResourceEdgeNetworkRelation) {
            edges = rman.findResourceEdges(rman.getNetworkRelation(), null, platformTypeIds, platformName)
        }
        return edges
    }
    
    /**
     * Find the descendant edges of a resource
     *
     * @return a Collection of {@link ResourceEdge}s
     */
    Collection findResourceEdges(String resourceRelation, String prototype, String platformName) {
        def edges = []
        if (resourceRelation == AuthzConstants.ResourceEdgeNetworkRelation) {
            def platformType = null
            def platformTypeIds = []
            if (prototype) {
                try {
                    platformType = platMan.findPlatformTypeByName(prototype)
                } catch (PlatformNotFoundException e) {
                    return Collections.EMPTY_LIST
                }
            }
            if (platformType == null) {
                platformTypeIds = Collections.EMPTY_LIST
            } else {
                platformTypeIds = Collections.singletonList(platformType.id)
            }    			
            edges = findResourceEdges(resourceRelation, platformTypeIds, platformName)
        }
        return edges
    }
    
    /**
     * Find the child resources of a resource using the virtual relation
     * 
     * @return a List of {@link Resource}s
     */
    Collection findChildResourcesByVirtualRelation(Resource resource) {
        def resourceEdges = rman.findChildResourceEdges(resource, rman.getVirtualRelation())
        
        return resourceEdges.collect { edge -> edge.to }
    }
    
    /**
     * Find all descendant resources of a resource using the virtual relation
     * 
     * @return a List of {@link Resource}s
     */
    Collection findDescendantResourcesByVirtualRelation(Resource resource) {
        def resourceEdges = rman.findDescendantResourceEdges(resource, rman.getVirtualRelation())
        
        return resourceEdges.collect { edge -> edge.to }
    }
    
    /**
     * Find all ancestors of a resource using the virtual relation
     * 
     * @return a List of {@link Resource}s
     */
    Collection findAncestorsByVirtualRelation(Resource resource) {
        def resourceEdges = rman.findAncestorResourceEdges(resource, rman.getVirtualRelation())
        
        return resourceEdges.collect { edge -> edge.to }
    }
    
    /**
     * Find all resources in a virtual relation that contain the supplied name string
     * 
     * @return a List of {@link Resource}s
     */
    Collection findResourcesByNameAndVirtualRelation(String name) {
        def resourceEdges = rman.findResourceEdgesByName(name, rman.getVirtualRelation())
        
        return resourceEdges.collect { edge -> edge.from }
    }
    
    /**
     * 
     */
    Resource getParentResourceByVirtualRelation(Resource resource) {
        def resourceEdges = rman.getParentResourceEdge(resource, rman.getVirtualRelation())
        
        return resourceEdges?.to
    }
    
    /**
     * Check whether or not a resource has children using the virtual relation
     * 
     * @return boolean
     */
    boolean hasChildResourcesByVirtualRelation(Resource resource) {
        return rman.hasChildResourceEdges(resource, rman.getVirtualRelation())
    }   
    
    /**
     * Returns the number of descendant resources using the virtual relation
     * 
     * @return boolean
     */
    int getDescendantResourceCountByVirtualRelation(Resource resource) {
        return rman.getDescendantResourceEdgeCount(resource, rman.getVirtualRelation())
    }
    
    /**
     * Check whether or not a resource exists with a virtual relation
     * 
     * @return boolean
     */
    boolean hasVirtualResourceRelation(Resource resource) {
        return rman.hasResourceRelation(resource, rman.getVirtualRelation())
    }
    
    
    List findParentPlatformsByNetworkRelation(String prototype, String name, Boolean hasChildren) {
        def platformType = null
        def platformTypeIds = null
        
        if (prototype) {
            try {
                platformType = platMan.findPlatformTypeByName(prototype)
            } catch (PlatformNotFoundException e) {
                return Collections.EMPTY_LIST
            }
        }
        
        if (platformType == null) {
            platformTypeIds = Collections.EMPTY_LIST
        } else {
            platformTypeIds = Collections.singletonList(platformType.id)
        }
        
        return findParentPlatformsByNetworkRelation(platformTypeIds, name, hasChildren)
    }
    
    List findParentPlatformsByNetworkRelation(List platformTypeIds, String name, Boolean hasChildren) {
        return platMan.findParentPlatformPojosByNetworkRelation(
        user, platformTypeIds, name, hasChildren)
    }
    
    List findPlatformsByNoNetworkRelation(String prototype, String name) {
        def platformType = null
        def platformTypeIds = null
        def platforms = null
        
        if (prototype) {
            try {
                platformType = platMan.findPlatformTypeByName(prototype)
            } catch (PlatformNotFoundException e) {
                platforms = Collections.EMPTY_LIST
            }
        }
        
        if (platforms == null) {
            if (platformType == null) {
                platformTypeIds = Collections.EMPTY_LIST
            } else {
                platformTypeIds = Collections.singletonList(platformType.id)
            }
            platforms = findPlatformsByNoNetworkRelation(platformTypeIds, name)
        }
        
        return platforms
    }
    
    List findPlatformsByNoNetworkRelation(List platformTypeIds, String name) {
        return platMan.findPlatformPojosByNoNetworkRelation(user, platformTypeIds, name)
    }
    
    void createResourceEdges(String resourceRelation, AppdefEntityID parent, AppdefEntityID[] children, boolean deleteExisting) {
        ResourceRelation relation = null;
        if (AuthzConstants.ResourceEdgeNetworkRelation.equals(resourceRelation)) {
            relation = rman.getNetworkRelation()
        } else if (AuthzConstants.ResourceEdgeVirtualRelation.equals(resourceRelation)) {
            relation = rman.getVirtualRelation()
        } else {
            throw new IllegalArgumentException(
            'Unsupported resource relation: ' + resourceRelation)
        }
        
        rman.createResourceEdges(user, relation, parent, children, deleteExisting)
    }
    
    void removeResourceEdges(String resourceRelation, Resource resource) {
        if (!resourceRelation.equals(AuthzConstants.ResourceEdgeNetworkRelation)) {
            throw new IllegalArgumentException('Only ' 
            + AuthzConstants.ResourceEdgeNetworkRelation
            + ' resource relationships are supported.')
        }
        
        rman.removeResourceEdges(user, rman.getNetworkRelation(), resource)
    }
    
    void removeResourceEdges(String resourceRelation, AppdefEntityID parent, AppdefEntityID[] children) {
        if (!resourceRelation.equals(AuthzConstants.ResourceEdgeNetworkRelation)) {
            throw new IllegalArgumentException('Only ' 
            + AuthzConstants.ResourceEdgeNetworkRelation
            + ' resource relationships are supported.')
        }
        
        rman.removeResourceEdges(user, rman.getNetworkRelation(), parent, children)
    }
    
    Collection getDownResources(String typeId, PageInfo pInfo) {
        Bootstrap.getBean(AppdefBoss.class).getUnavailableResources(user, typeId, pInfo)
    }
    
    Map getDownResourcesMap() {
        Bootstrap.getBean(AppdefBoss.class).getUnavailableResourcesCount(user)
    }
    
    List findResourcesOfType(String typeName, PageInfo pInfo) {
        def rsrc = rman.findResourcePrototypeByName(typeName)
        if (rsrc == null)
            return []
        rman.findResourcesOfPrototype(rsrc, pInfo)
    }
    
    List findResourcesOfType(Resource prototype, PageInfo pInfo) {
        rman.findResourcesOfPrototype(prototype, pInfo)
    }
    
    List findResourcesOfType(Resource prototype) {
        rman.findResourcesOfPrototype(prototype, 
        PageInfo.getAll(ResourceSortField.NAME,
        true))
    }
    
    Resource findRootResource() {
        rman.findRootResource()
    }
    
    /**
     * Find all resource groups
     *
     * Returns a list of {@link ResourceGroup}s
     */
    List findAllGroups() {
        groupMan.getAllResourceGroups()
    }
    
    /**
     * Find all {@link ResourceGroup}s viewable to the passed user.
     */
    List findViewableGroups() {
        groupMan.getAllResourceGroups(user, true).grep { !it.system }
    }
    
    ResourceGroup createGroup(String name, String description, String location,
                              Resource prototype, Collection roles,
                              Collection resources, boolean isPrivate) {
        groupMan.createResourceGroup(user, getCreateInfo(name,description,location,prototype,isPrivate), roles, resources)
    }
    
    private ResourceGroupCreateInfo getCreateInfo(String name, String description, String location,
    Resource prototype, boolean isPrivate) {
        int groupType
        if (!prototype) {
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS
        } else {
            if (prototype.isService()) {
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
            } else {
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS
            }
        }
        
        ResourceGroupCreateInfo info =
                new ResourceGroupCreateInfo(name, description, groupType, prototype,
                location, 0, false, isPrivate);
        return info
    }
    
    /**
     * Find a prototype by name.
     */
    Resource findResourcePrototype(String name) {
        rman.findResourcePrototypeByName(name)
    }
    
    /**
     * Find all prototypes of platforms, servers, and services.
     *
     * @return a list of {@link Resource}s which are prototypes.
     */
    List findAllAppdefPrototypes() {
        rman.findAllAppdefPrototypes()
    }
    
    /**
     * Find prototypes of platforms, servers, and services that have
     * at least 1 instance of that prototype in the inventory.
     *
     * @return a list of {@link Resource}s which are prototypes.
     */
    List findAppdefPrototypes() {
        rman.findAppdefPrototypes()
    }
    
    /**
     * Find a group by id.  Permission checking is performed.
     */
    ResourceGroup findGroup(int id) {
        groupMan.findResourceGroupById(user, id)
    }
    
    /**
     * Find a group by name.  Permission checking is performed.
     */
    ResourceGroup findGroupByName(String name) {
        groupMan.findResourceGroupByName(user, name)
    }
    
    Resource findResource(int id) {
        rman.findResourceById(id)
    }
    
    List findViewableInstances(user, type, name) {
        rman.findViewableInstances(user, type, name, null, null,
        PageControl.PAGE_ALL)
    }
    
    /**
     * Find a platform by name.
     */
    def findPlatformByFQDN(user, name) {
        platMan.findPlatformByFqdn(user, name)
    }
    
    /**
     * Find services by server ID.
     */
    def findServicesByServer(user, id) {
        Bootstrap.getBean(ServiceManager.class).getServicesByServer(user, id,
        PageControl.PAGE_ALL)
    }
    
    /**
     * Get the ResourceType resource
     */
    def findResourceTypeResourceById(id) {
        rman.getResourceTypeResource(id)
    }
}
