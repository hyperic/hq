import org.hyperic.hibernate.Util
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.bizapp.shared.AppdefBossUtil
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants

/**
 * This script assigns all currently existing platforms, servers, and services 
 * to a preexisting group. The user must be logged in as a user assigned the 
 * "Super User" role and that user name and group name must be configured before 
 * executing the script.
 */ 

// Configure the following parameters:

// Set currently logged in "Super User" user name for permissioning purposes
def userName = "hqadmin"

// Set the group name
def groupName = "Group X"


// The script starts here. Do not edit below this line.

def output = new StringBuffer()

// Get the group by name
appdefGroupValue = getAppdefGroupValue(userName, groupName)

// Save all the platforms to the group
saveAllPlatformsToGroup(userName, appdefGroupValue, output)

// Save all the servers to the group
saveAllNonVirtualServersToGroup(userName, appdefGroupValue, output)

// Save all the services to the group
saveAllServicesToGroup(userName, appdefGroupValue, output)

return output.toString()

// Script Functions

def getAppdefGroupValue(userName, groupName) {	
    def sessionId = SessionManager.getInstance().getIdFromUsername(userName)
    return getAppdefBoss().findGroupByName(sessionId, groupName)
}

def saveAllPlatformsToGroup(userName, appdefGroupValue, output) {
    def platformIds = findAllPlatformIds()
       
    def numSaved = 
        saveResourcesToGroup(userName, appdefGroupValue, platformIds, AppdefEntityConstants.APPDEF_TYPE_PLATFORM)
    
    output.append("Saved "+numSaved+" platforms to group "+appdefGroupValue.name+"\n")
}  

def saveAllNonVirtualServersToGroup(userName, appdefGroupValue, output) {
    def serverIds = findAllNonVirtualServerIds()
    
    def numSaved = 
        saveResourcesToGroup(userName, appdefGroupValue, serverIds, AppdefEntityConstants.APPDEF_TYPE_SERVER)
    
    output.append("Saved "+numSaved+" servers to group "+appdefGroupValue.name+"\n")        
}  

def saveAllServicesToGroup(userName, appdefGroupValue, output) {
    def serviceIds = findAllServiceIds()
        
    def numSaved = 
        saveResourcesToGroup(userName, appdefGroupValue, serviceIds, AppdefEntityConstants.APPDEF_TYPE_SERVICE)
    
    output.append("Saved "+numSaved+" services to group "+appdefGroupValue.name+"\n")
}  


// Helper Functions

def saveResourcesToGroup(userName, appdefGroupValue, entityIds, entityType) {
    def numSaved = 0
    
    entityIds.each {
        def entity = new AppdefEntityID(entityType, it)
        
        if (!appdefGroupValue.existsAppdefEntity(entity)) {
            appdefGroupValue.addAppdefEntity(entity)
            numSaved++
        }    
    }
    
    def sessionId = SessionManager.getInstance().getIdFromUsername(userName)
    getAppdefBoss().saveGroup(sessionId, appdefGroupValue)
    
    return numSaved
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

/**
 * Older versions of HQ (3.1 for example), do not have the static 
 * getOne() operation on on the AppdefBossEJBImpl.
 */ 
def getAppdefBoss() {
    return AppdefBossUtil.getLocalHome().create()
}




