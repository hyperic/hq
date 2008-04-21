package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.appdef.shared.PlatformNotFoundException
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl as AppdefBoss
import org.hyperic.hq.authz.HasAuthzOperations

class ResourceHelper extends BaseHelper {
    private rman = ResourceManagerEJBImpl.one
    private groupMan = GroupMan.one
    
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
     *  Currently, this does not take permissions into account whe returning
     *  the count:* methods
     */
    def find(Map args) {
        args = args + [:]  // Don't modify caller's map 
        // Initialize all used arguments to null
        ['count', 'platform', 'server', 'service',
         'byPrototype', 'prototype', 'withPaging',
        ].each {args.get(it, null)}
        args.get('user', user)         // Use default user  
        args.get('operation', 'view')  // Default permission required
        
        if (args.count != null) {
            switch (args.count) {
            case 'platforms': return PlatMan.one.platformCount
            case 'servers':   return ServerMan.one.serverCount
            case 'services':  return ServiceMan.one.serviceCount
            default:
                throw new IllegalArgumentException('count must specify a ' + 
                                                   'valid resource type')
            }
        }

        if (args.prototype) {
            return findPrototype(args)
        }
        
        if (args.byPrototype) {
            return findByPrototype(args)
        }
        
        def plat
        if (args.platform != null) {
            if (args.platform in String) {
                plat = PlatMan.one.getPlatformByName(args.platform)
            } else {
                try { 
                    plat = PlatMan.one.findPlatformById(args.platform as int)
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
                server = ServerMan.one.getServerByName(plat, args.server)
            } else {
                server = ServerMan.one.getServerById(args.server as int)
            }
            
            if (args.service == null)
                return server?.checkPerms(args)
        }
        
        if (args.service != null) {
            def service
            if (args.service in String) {
                if (server) {
                    service = ServiceMan.one.getServiceByName(server, args.service)
                }
                else if (plat) {
                    service = ServiceMan.one.getServiceByName(plat, args.service)
                }
            } else {
                service = ServiceMan.one.getServiceById(args.service as int)
            }
            
            return service?.checkPerms(args)
        }

        throw new IllegalArgumentException('Unknown arguments passed to find()')
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

    Collection getDownResources(String typeId, PageInfo pInfo) {
        AppdefBoss.one.getUnavailableResources(user, typeId, pInfo)
    }
    
    Map getDownResourcesMap() {
    	AppdefBoss.one.getUnavailableResourcesCount(user)
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
     * Find all resource groups:
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
        groupMan.getAllResourceGroups(userValue, true) // excludeRoot
    }
     
    /**
     * Find a prototype by name.
     */
    Resource findResourcePrototype(String name) {
        rman.findResourcePrototypeByName(name)
    }

    /**
     * Find all prototypes of platforms, servers, and services
     *
     * @return a list of {@link Resource}s which are prototypes.
     */
    List findAllAppdefPrototypes() {
        rman.findAllAppdefPrototypes()
    }
     
    /**
     * Find a group by id.  Permission checking is performed.
     */
    ResourceGroup findGroup(int id) {
         groupMan.findResourceGroupById(userValue, id)
    }
    
    /**
     * Find a group by name.  Permission checking is performed.
     */
    ResourceGroup findGroupByName(String name) {
         groupMan.findResourceGroupByName(user, name)
    }
    
    Resource findResource(int id) {
        rman.findResourcePojoById(id)
    }
}
