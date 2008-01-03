package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl

class ResourceHelper extends BaseHelper {
    private rman = ResourceManagerEJBImpl.one
    private appBoss = AppdefBossEJBImpl.one
    
    ResourceHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * General purpose utility method for finding resources and resource
     * counts.
     *
     * To find the counts of resource types:
     *   find count:'platforms'
     *   find count:'servers'
     *   find count:'services'
     *
     */
    def find(Map args) {
        // Initialize all used arguments to null
        ['count'].each {args.get(it, null)}
        
        switch (args.count) {
        case 'platforms': return PlatMan.one.platformCount
        case 'servers':   return ServerMan.one.serverCount
        case 'services':  return ServiceMan.one.serviceCount
        default:
            throw new IllegalArgumentException('count must specify a valid ' +
                                               'resource type')
        }
        
        throw new IllegalArgumentException('Unknown arguments passed to find()')
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
        appBoss.getUnavailableResources(user, typeId, pInfo)
    }
    
    Map getDownResourcesMap() {
    	appBoss.getUnavailableResourcesCount(user)
    }
    
    List findResourcesOfType(String typeName, PageInfo pInfo) {
        def rsrc = rman.findResourcePrototypeByName(typeName)
        if (rsrc == null)
            return []
        rman.findResourcesOfPrototype(rsrc, pInfo)
    }
    
    Resource findResourcePrototype(String name) {
        rman.findResourcePrototypeByName(name)
    }
}
