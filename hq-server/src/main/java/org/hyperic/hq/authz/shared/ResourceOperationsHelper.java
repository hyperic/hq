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
    public final static int POLICY = 8 * MULTIPLIER;
    
    
    
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
    
    private static List<String> operationsList;
    
    static {
        // ArrayList containing all the operations for each resource type...
        // ORDER IS IMPORTANT!
        operationsList = new ArrayList<String>(56);
        
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
        operationsList.add(AuthzConstants.policyOpCreatePolicy);
        operationsList.add(AuthzConstants.policyOpViewPolicy);
        operationsList.add(AuthzConstants.policyOpModifyPolicy);
        operationsList.add(AuthzConstants.policyOpRemovePolicy);
        operationsList.add(null);
        operationsList.add(null);       
    }
    
    public static String getOperationName(int resourceTypeCode, int operationCode) {
        return operationsList.get(resourceTypeCode + operationCode);
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
        if ((resource == null) || (resource.getResourceType() == null)) {
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
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                resourceTypeCode = APPLICATION;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_POLICY:
                resourceTypeCode = POLICY;
                break;
            default:
                resourceTypeCode = -1;
        }
        
        if (resourceTypeCode < 0) {
            throw new IllegalArgumentException("resourceType must be a platform, server, service or group resource type," +
                                               " illegal type was " + resourceTypeId);
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
