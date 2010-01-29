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
import org.hyperic.hq.authz.server.session.RoleManagerImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
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
        PermissionManager permMgr = PermissionManagerFactory.getInstance();

        if (!resourceTypes.containsKey(rtName)) {
            resourceTypes.put(rtName, resourceTypeDAO.findByName(rtName));
        }
        ResourceType resType = (ResourceType) resourceTypes.get(rtName);

        if (!operations.containsKey(opName)) {
            operations.put(opName, operationDAO.findByTypeAndName(resType, opName));
        }
        Operation operation = (Operation) operations.get(opName);

        permMgr.check(subjectId, resType.getId(), instId, operation.getId());
        // Permission Check Succesful
    }

    public void canManageAlerts(AuthzSubject who, AlertDefinitionInterface adi)  throws PermissionException{
        if (adi.isDeleted()) // Don't need to check deleted alert defs
            return;

        Integer parentId = null;
        if (adi instanceof AlertDefinition) {
            AlertDefinition ad = (AlertDefinition) adi;
            parentId = ad.getParent() != null ? ad.getParent().getId() : null;
        }

        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(parentId))
            canManageAlerts(who, getAppdefEntityID(adi));
    }

    /**
     * Check for manage alerts permission for a given resource
     */
    public  void canManageAlerts(AuthzSubject who, AppdefEntityID id)  throws PermissionException {
        if (id instanceof AppdefEntityTypeID) {
            // Make sure the user is a super user
            if (roleManager.isRootRoleMember(who))
                return;
            throw new PermissionException("User must be in Super User role " + "to manage resource type alert "
                                          + "definitions");
        }

        int type = id.getType();
        String rtName = null;
        String opName = null;
        switch (type) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            rtName = AuthzConstants.platformResType;
            opName = AuthzConstants.platformOpManageAlerts;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            rtName = AuthzConstants.serverResType;
            opName = AuthzConstants.serverOpManageAlerts;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            rtName = AuthzConstants.serviceResType;
            opName = AuthzConstants.serviceOpManageAlerts;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            rtName = AuthzConstants.applicationResType;
            opName = AuthzConstants.appOpManageAlerts;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            rtName = AuthzConstants.groupResourceTypeName;
            opName = AuthzConstants.groupOpManageAlerts;
            break;
        default:
            throw new InvalidAppdefTypeException("Unknown type: " + type);
        }

        // now check
        checkPermission(who.getId(), rtName, id.getId(), opName);
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

    public  void canModifyEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpModifyEscalation);
    }

    public  void canRemoveEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpRemoveEscalation);
    }
}
