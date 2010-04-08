package org.hyperic.hq.authz.shared;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;

public class ResourceOperationsHelper {
    // This number should be equal to the max number of operation codes per resource
    private final static int MULTIPLIER = 6;
    
    // Resource type codes...
    public final static int PLATFORM = 0;
    public final static int SERVER = 1 * MULTIPLIER;
    public final static int SERVICE = 2 * MULTIPLIER;
    public final static int GROUP = 3 * MULTIPLIER;
    public final static int APPLICATION = 4 * MULTIPLIER;
    public final static int USER = 5 * MULTIPLIER;
    public final static int ROLE = 6 * MULTIPLIER;
    public final static int ESCALATION = 7 * MULTIPLIER;
    
    // Operation codes...these are added to the resource type code to get the actual operation
    public final static int CREATE = 0;
    public final static int READ = 1;
    public final static int UPDATE = 2;
    public final static int DELETE = 3;
    public final static int MANAGE_ALERTS = 4;
    public final static int MANAGE_CONTROLS = 5;
    
    // Permission Levels...
    public final static int NO_PERMISSIONS = 0;
    public final static int READ_ONLY_PERMISSION = 1;
    public final static int READ_WRITE_PERMISSIONS = 2;
    public final static int FULL_PERMISSIONS = 3;
    
    private static List operationsList;
    
    static {
        // ArrayList containing all the operations for each resource type...
        // ORDER IS IMPORTANT!
        operationsList = new ArrayList(48);
        
        operationsList.add(AuthzConstants.platformOpCreatePlatform);
        operationsList.add(AuthzConstants.platformOpViewPlatform);
        operationsList.add(AuthzConstants.platformOpModifyPlatform);
        operationsList.add(AuthzConstants.platformOpRemovePlatform);
        operationsList.add(AuthzConstants.platformOpManageAlerts);
        operationsList.add(AuthzConstants.platformOpControlPlatform);
        // TODO Create server is add server.  This needs to be cleaned up.
        operationsList.add(AuthzConstants.platformOpAddServer); 
        operationsList.add(AuthzConstants.serverOpViewServer);
        operationsList.add(AuthzConstants.serverOpModifyServer);
        operationsList.add(AuthzConstants.serverOpRemoveServer);
        operationsList.add(AuthzConstants.serverOpManageAlerts);
        operationsList.add(AuthzConstants.serverOpControlServer);
        // TODO Create service is add service.  This needs to be cleaned up.
        operationsList.add(AuthzConstants.serverOpAddService);
        operationsList.add(AuthzConstants.serviceOpViewService);
        operationsList.add(AuthzConstants.serviceOpModifyService);
        operationsList.add(AuthzConstants.serviceOpRemoveService);
        operationsList.add(AuthzConstants.serviceOpManageAlerts);
        operationsList.add(AuthzConstants.serviceOpControlService);
        operationsList.add(AuthzConstants.groupOpCreateResourceGroup);
        operationsList.add(AuthzConstants.groupOpViewResourceGroup);
        operationsList.add(AuthzConstants.groupOpModifyResourceGroup);
        operationsList.add(AuthzConstants.groupOpRemoveResourceGroup);
        operationsList.add(AuthzConstants.groupOpManageAlerts);
        operationsList.add(null);
        operationsList.add(AuthzConstants.appOpCreateApplication);
        operationsList.add(AuthzConstants.appOpViewApplication);
        operationsList.add(AuthzConstants.appOpModifyApplication);
        operationsList.add(AuthzConstants.appOpRemoveApplication);
        operationsList.add(null);
        operationsList.add(AuthzConstants.appOpControlApplication);
        operationsList.add(AuthzConstants.subjectOpCreateSubject);
        operationsList.add(AuthzConstants.subjectOpViewSubject);
        operationsList.add(AuthzConstants.subjectOpModifySubject);
        operationsList.add(AuthzConstants.subjectOpRemoveSubject);
        operationsList.add(null);
        operationsList.add(null);
        operationsList.add(AuthzConstants.roleOpCreateRole);
        operationsList.add(AuthzConstants.roleOpViewRole);
        operationsList.add(AuthzConstants.roleOpModifyRole);
        operationsList.add(AuthzConstants.roleOpRemoveRole);
        operationsList.add(null);
        operationsList.add(null);
        operationsList.add(AuthzConstants.escOpCreateEscalation);
        operationsList.add(AuthzConstants.escOpViewEscalation);
        operationsList.add(AuthzConstants.escOpModifyEscalation);
        operationsList.add(AuthzConstants.escOpRemoveEscalation);
        operationsList.add(null);
        operationsList.add(null);       
    }
    
    public static String getOperationName(int resourceTypeCode, int operationCode) {
        return (String) operationsList.get(resourceTypeCode + operationCode);
    }
    
    public static String getCreateOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, CREATE);
    }

    public static String getReadOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, READ);
    }

    public static String getUpdateOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, UPDATE);
    }

    public static String getDeleteOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, DELETE);
    }

    public static String getManageAlertOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, MANAGE_ALERTS);
    }

    public static String getManageControlOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, MANAGE_CONTROLS);
    }
    
    public static String getCreateOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, CREATE);
    }

    public static String getReadOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, READ);
    }

    public static String getUpdateOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, UPDATE);
    }

    public static String getDeleteOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, DELETE);
    }

    public static String getManageAlertOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, MANAGE_ALERTS);
    }

    public static String getManageControlOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, MANAGE_CONTROLS);
    }
   
    public static String getResourceType(Resource resource)
    throws IllegalArgumentException, UnsupportedOperationException {
        if (resource == null || resource.getResourceType() == null) {
            throw new IllegalArgumentException("resource must be not be null and must have a valid resource type.");
        }
        
        int resourceTypeId = resource.getResourceType().getId().intValue();
        
        return getResourceType(resourceTypeId);
    }
    
    public static String getResourceType(int resourceTypeId)
    throws IllegalArgumentException, UnsupportedOperationException {
        switch (resourceTypeId) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return AuthzConstants.platformResType;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return AuthzConstants.serverResType;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return AuthzConstants.serviceResType;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return AuthzConstants.groupResType;
            default:
                throw new UnsupportedOperationException("resource type[" + resourceTypeId + "] associated with resource is not supported");
        }
    }
    
    private static String getOperation(Resource resource, int operationCode) 
    throws IllegalArgumentException {
        int resourceTypeId = resource.getResourceType().getId().intValue();
        
        return getOperation(resourceTypeId, operationCode);
    }
    
    private static String getOperation(int resourceTypeId, int operationCode)
    throws IllegalArgumentException {
        int resourceTypeCode;
        
        switch (resourceTypeId) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resourceTypeCode = PLATFORM;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resourceTypeCode = SERVER;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resourceTypeCode = SERVICE;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                resourceTypeCode = GROUP;
                break;
            default:
                resourceTypeCode = -1;
        }
        
        if (resourceTypeCode < 0) {
            throw new IllegalArgumentException("resourceType must be a platform, server, service or group resource type.");
        }
        
        return getOperationName(resourceTypeCode, operationCode);
    }
    
    public static CodePair getResourceTypeOperationCodePair(String operationName) {
        int index = operationsList.indexOf(operationName);
        int resourceTypeCode = ((index < MULTIPLIER) ? 0 : index/MULTIPLIER) * MULTIPLIER;
        int operationCode = index - resourceTypeCode;
        
        return new CodePair(resourceTypeCode, operationCode);
    }
    
    public static class CodePair {
        int resourceTypeCode;
        int operationCode;
        
        public CodePair(int resourceTypeCode, int operationCode) {
            this.resourceTypeCode = resourceTypeCode;
            this.operationCode = operationCode;
        }
        
        public int getResourceTypeCode() {
            return resourceTypeCode;
        }
        
        public void setResourceTypeCode(int resourceTypeCode) {
            this.resourceTypeCode = resourceTypeCode;
        }
        
        public int getOperationCode() {
            return operationCode;
        }
        
        public void setOperationCode(int operationCode) {
            this.operationCode = operationCode;
        }
    }
}
