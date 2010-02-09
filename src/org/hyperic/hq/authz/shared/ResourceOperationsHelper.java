package org.hyperic.hq.authz.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;

public class ResourceOperationsHelper {
    // Resource type codes...
    private final static int PLATFORM = 0;
    private final static int SERVER = 5;
    private final static int SERVICE = 10;
    private final static int GROUP = 15;
    
    // Operation codes...these are added to the resource type code to get the actual operation
    private final static int CREATE = 0;
    private final static int READ = 1;
    private final static int UPDATE = 2;
    private final static int DELETE = 3;
    private final static int MANAGE_ALERTS = 4;
    
    // Array containing all the operations for each resource type...
    // ORDER IS IMPORTANT!
    private final static String[] operationsArray = {
       AuthzConstants.platformOpCreatePlatform,
       AuthzConstants.platformOpViewPlatform,
       AuthzConstants.platformOpModifyPlatform,
       AuthzConstants.platformOpRemovePlatform,
       AuthzConstants.platformOpManageAlerts,
       AuthzConstants.serverOpCreateServer,
       AuthzConstants.serverOpViewServer,
       AuthzConstants.serverOpModifyServer,
       AuthzConstants.serverOpRemoveServer,
       AuthzConstants.serverOpManageAlerts,
       AuthzConstants.serviceOpCreateService,
       AuthzConstants.serviceOpViewService,
       AuthzConstants.serviceOpModifyService,
       AuthzConstants.serviceOpRemoveService,
       AuthzConstants.serviceOpManageAlerts,
       AuthzConstants.groupOpCreateResourceGroup,
       AuthzConstants.groupOpViewResourceGroup,
       AuthzConstants.groupOpModifyResourceGroup,
       AuthzConstants.groupOpRemoveResourceGroup,
       AuthzConstants.groupOpManageAlerts
    };

    public String getCreateOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, CREATE);
    }

    public String getReadOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, READ);
    }

    public String getUpdateOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, UPDATE);
    }

    public String getDeleteOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, DELETE);
    }

    public String getManageAlertOperation(Resource resource) 
    throws IllegalArgumentException {
        return getOperation(resource, MANAGE_ALERTS);
    }
    
    public String getCreateOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, CREATE);
    }

    public String getReadOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, READ);
    }

    public String getUpdateOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, UPDATE);
    }

    public String getDeleteOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, DELETE);
    }

    public String getManageAlertOperation(int resourceTypeId) 
    throws IllegalArgumentException {
        return getOperation(resourceTypeId, MANAGE_ALERTS);
    }
    
    public String getResourceType(Resource resource)
    throws IllegalArgumentException, UnsupportedOperationException {
        if (resource == null || resource.getResourceType() == null) {
            throw new IllegalArgumentException("resource must be not be null and must have a valid resource type.");
        }
        
        int resourceTypeId = resource.getResourceType().getId().intValue();
        
        return getResourceType(resourceTypeId);
    }
    
    public String getResourceType(int resourceTypeId)
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
    
    private String getOperation(Resource resource, int operationCode) 
    throws IllegalArgumentException {
        int resourceTypeId = resource.getResourceType().getId().intValue();
        
        return getOperation(resourceTypeId, operationCode);
    }
    
    private String getOperation(int resourceTypeId, int operationCode)
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
        
        return operationsArray[resourceTypeCode + operationCode];
    }
}
