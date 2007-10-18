package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hibernate.SortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl

class ResourceHelper extends BaseHelper {
    private rsrcMan = ResourceManagerEJBImpl.one
    private appBoss = AppdefBossEJBImpl.one
    
    ResourceHelper(AuthzSubject user) {
        super(user)
    }

    def findPlatforms(PageInfo pInfo)  {
        rsrcMan.findResourcesOfType(AuthzConstants.authzPlatform, pInfo)
    }
    
    def findAllPlatforms() {
        findPlatforms(PageInfo.getAll(ResourceSortField.NAME, true))
    }

    def findServers(PageInfo pInfo)  {
        rsrcMan.findResourcesOfType(AuthzConstants.authzServer, pInfo)
    }
    
    def findAllServers() {
        findServers(PageInfo.getAll(ResourceSortField.NAME, true))
    }

    def findServices(PageInfo pInfo)  {
        rsrcMan.findResourcesOfType(AuthzConstants.authzService, pInfo)
    }
    
    def findAllServices() {
        findServices(PageInfo.getAll(ResourceSortField.NAME, true))
    }

    def getDownResources(PageInfo pInfo) {
        appBoss.getUnavailableResources(user, pInfo)
    }
}
