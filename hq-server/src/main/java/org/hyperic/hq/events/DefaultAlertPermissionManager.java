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

package org.hyperic.hq.events;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceOperationsHelper;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultAlertPermissionManager implements AlertPermissionManager {

    /**
     * Check the permission based on resource type, instance ID, and operation
     * @param subjectId the subject trying to perform the operation
     * @param rtName the resource type name
     * @param instId the instance ID
     * @param operation the operation
     * @throws PermissionException if the user is not authorized to perform the
     *         operation on the resource
     */
    private  Map resourceTypes = new HashMap();
    private  Map operations = new HashMap();
    private OperationDAO operationDAO;
    private ResourceTypeDAO resourceTypeDAO;
    private RoleManager roleManager;
    
    
    @Autowired
    public DefaultAlertPermissionManager(OperationDAO operationDAO, ResourceTypeDAO resourceTypeDAO, RoleManager roleManager) {
        this.operationDAO = operationDAO;
        this.resourceTypeDAO = resourceTypeDAO;
        this.roleManager = roleManager;
    }

    private  void checkPermission(Integer subjectId, String rtName, Integer instId, String opName) throws PermissionException
    {
        // ...check permission if user is NOT a super user...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(subjectId)) {
            PermissionManager permMgr = PermissionManagerFactory.getInstance();
            if (!resourceTypes.containsKey(rtName)) {
                resourceTypes.put(rtName, resourceTypeDAO.findByName(rtName));
            }
            ResourceType resType = (ResourceType) resourceTypes.get(rtName);
            
            if (!operations.containsKey(opName)) {
                operations.put(opName,operationDAO.findByTypeAndName(resType, opName));
            }
            Operation operation = (Operation) operations.get(opName);
            permMgr.check(subjectId, resType.getId(), instId, operation.getId());
            // Permission Check Succesful
        }
    }
    
    public void canViewResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public void canModifyResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public void canCreateResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public void canDeleteResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }

    public void canViewAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can view the alert definition.  Must have read permission on resource...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getReadOperation(entityId.getType()));
    }
    
    public void canModifyAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }
    
    public void canCreateAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        // TODO ...If we introduce finer grained permission for Alert definition, we can make the change here
        // and the rest should just work...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }

    public void canDeleteAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        // TODO ...If we introduce finer grained permission for Alert definition, we can make the change here
        // and the rest should just work...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }
    
    private void checkAlertDefinitionPermission(AuthzSubject user, AppdefEntityID id, String operationName) 
    throws PermissionException {
        if (user.getId().equals(AuthzConstants.overlordId)) {
            return;
        }
        int resourceType = id.getType();
        String resourceTypeLabel;
        
        switch (resourceType) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resourceTypeLabel = AuthzConstants.platformResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resourceTypeLabel = AuthzConstants.serverResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resourceTypeLabel = AuthzConstants.serviceResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP: 
                resourceTypeLabel = AuthzConstants.groupResType;

                break;
            default:
                throw new PermissionException("Unknown type: " + resourceType);
        }
        
        // ...check based on resource type to see if we have the requested permission...
        checkPermission(user.getId(), resourceTypeLabel, id.getId(), operationName);
    }
    
    public void canFixAcknowledgeAlerts(AuthzSubject who, AlertDefinitionInterface adi)
    throws PermissionException {
        if (adi.isDeleted()) {    // Don't need to check deleted alert defs
            return;
        }
        
        Integer parentId = null;

        if (adi instanceof AlertDefinition) {
            AlertDefinition ad = (AlertDefinition) adi;
            parentId = ad.getParent() != null ? ad.getParent().getId() : null;
        }
        
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) {
            canFixAcknowledgeAlerts(who, AppdefUtil.newAppdefEntityId(adi.getResource()));
        }
    }
    
    /**
     * Check for manage alerts permission for a given resource 
     * 
     * By manage, we mean the ability to fix/acknowledge alerts & pause escalations...
     */
    public void canFixAcknowledgeAlerts(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        try {
            canModifyAlertDefinition(user, entityId);
        } catch(PermissionException e) {
            // ...first check that we can view the alert...
            canViewAlertDefinition(user, entityId);
    
            int resourceTypeId = entityId.getType();
            
            // ...then check if we have fix/acknowledge permissions on alert...
            checkPermission(user.getId(), 
                            ResourceOperationsHelper.getResourceType(resourceTypeId), 
                            entityId.getId(), 
                            ResourceOperationsHelper.getManageAlertOperation(resourceTypeId));            
        }
    }
    
    public  AppdefEntityID getAppdefEntityID(AlertDefinitionInterface adi) {
        try {
            return AppdefUtil.newAppdefEntityId(adi.getResource());
        } catch (IllegalArgumentException e) {
            if (adi instanceof AlertDefinition) {
                AlertDefinition ad = (AlertDefinition) adi;
                return new AppdefEntityTypeID(ad.getAppdefType(), ad.getAppdefId());
            } else {
                throw e;
            }
        }
    }

    private  void checkEscalation(Integer subjectId,
                                        String operation)
        throws PermissionException {
        // The escalation resource type is looked up for its ID to be used
        // instance ID.  The reason is that escalations are global, and we're
        // not applying escalation permission per appdef resource.
        ResourceType rt = resourceTypeDAO
            .findByName(AuthzConstants.escalationResourceTypeName);

        checkPermission(subjectId, AuthzConstants.rootResType, rt.getId(),
                        operation);
    }

    public  void canCreateEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpCreateEscalation);
    }
    
    public void canViewEscalation(Integer subjectId) 
    throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpViewEscalation);
    }

    public  void canModifyEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpModifyEscalation);
    }

    public  void canRemoveEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpRemoveEscalation);
    }
}
