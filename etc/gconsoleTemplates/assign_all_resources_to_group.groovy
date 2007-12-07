import org.hyperic.hibernate.Util
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl as appdefBoss
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants

/**
 * This script assigns all currently existing platforms, servers, and services 
 * to a preexisting group. The user must be logged in as the admin user and the 
 * admin user name and group name must be configured before executing the script.
 */


// Configure the following parameters:

// Set currently logged in admin user name for permissioning purposes
def userName = "hqadmin"

// Set the group name
def groupName = "Group X"


// The script starts here. Do not edit below this line.


// Get the group by name
appdefGroupValue = getAppdefGroupValue(userName, groupName)

// Save all the platforms to the group
saveAllPlatformsToGroup(userName, appdefGroupValue)

// Save all the servers to the group
saveAllNonVirtualServersToGroup(userName, appdefGroupValue)

// Save all the services to the group
saveAllServicesToGroup(userName, appdefGroupValue)



// Script Functions

def getAppdefGroupValue(userName, groupName) {	
    def sessionId = SessionManager.getInstance().getIdFromUsername(userName)
    return appdefBoss.one.findGroupByName(sessionId, groupName)
}

def saveAllPlatformsToGroup(userName, appdefGroupValue) {
    def platformIds = findAllPlatformIds()
    
    println("Saving "+platformIds.size()+" platforms to group "+appdefGroupValue.name)
    
    saveResourcesToGroup(userName, appdefGroupValue, platformIds, AppdefEntityConstants.APPDEF_TYPE_PLATFORM)
}  

def saveAllNonVirtualServersToGroup(userName, appdefGroupValue) {
    def serverIds = findAllNonVirtualServerIds()
    
    println("Saving "+serverIds.size()+" servers to group "+appdefGroupValue.name)    
    
    saveResourcesToGroup(userName, appdefGroupValue, serverIds, AppdefEntityConstants.APPDEF_TYPE_SERVER)
}  

def saveAllServicesToGroup(userName, appdefGroupValue) {
    def serviceIds = findAllServiceIds()
    
    println("Saving "+serviceIds.size()+" services to group "+appdefGroupValue.name)    
    
    saveResourcesToGroup(userName, appdefGroupValue, serviceIds, AppdefEntityConstants.APPDEF_TYPE_SERVICE)
}  


// Helper Functions

def saveResourcesToGroup(userName, appdefGroupValue, entityIds, entityType) {
    entityIds.each {
        def entity = new AppdefEntityID(entityType, it)
        
        if (!appdefGroupValue.existsAppdefEntity(entity))
            appdefGroupValue.addAppdefEntity(entity)
    }
    
    def sessionId = SessionManager.getInstance().getIdFromUsername(userName)
    appdefBoss.one.saveGroup(sessionId, appdefGroupValue)
}

def findAllPlatformIds() {
    def session = null;
    def platformIds = null;
    
    try {
        session = Util.getSessionFactory().openSession()
        platformIds = session.createQuery("select id from Platform").list()
    } finally {
        if (session != null)
            session.close()
    }

    return platformIds    
}

def findAllNonVirtualServerIds() {
    def session = null;
    def serverIds = null;
    
    try {
        session = Util.getSessionFactory().openSession()
        def sql = "select s.id from Server s join s.serverType st where st.virtual=false"
        serverIds = session.createQuery(sql).list()
    } finally {
        if (session != null)
            session.close()
    }

    return serverIds        
} 

def findAllServiceIds() {
    def session = null;
    def serviceIds = null;
        
    try {
        session = Util.getSessionFactory().openSession()
        serviceIds = session.createQuery("select id from Service").list()
    } finally {
        if (session != null)
            session.close()
    }

    return serviceIds        
}




