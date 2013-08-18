/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.PagerProcessor_operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.shared.HierarchicalAlertingManager;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.util.IntegerTransformer;
import org.hyperic.util.pager.PageControl;

public abstract class PermissionManager {

    public static final String OPERATION_PAGER = PagerProcessor_operation.class.getName();

    protected ResourceTypeDAO getResourceTypeDAO() {
        return Bootstrap.getBean(ResourceTypeDAO.class);
    }

    protected OperationDAO getOperationDAO() {
        return Bootstrap.getBean(OperationDAO.class);
    }

    protected ResourceDAO getResourceDAO() {
        return Bootstrap.getBean(ResourceDAO.class);
    }
    
    protected AlertPermissionManager getAlertPermissionManager() {
        return Bootstrap.getBean(AlertPermissionManager.class);
    }

    /**
     * Check a permission
     * @param subject - who
     * @param rtV - type of resource
     * @param id - the id of the object
     * @param operation - the name of the operation to perform
     */
    public void checkPermission(AuthzSubject subject, ResourceType rtV, Integer id, String operation)
        throws PermissionException {
        Integer opId = getOpIdByResourceType(rtV, operation);
        check(subject.getId(), rtV.getId(), id, opId);
    }

    /**
     * Check to see if the subject can perform an autoinventory scan on the
     * specified resource. For platforms, the user must have modify platform
     * permissions on the platform, and add server permissions on the platform.
     * For a group, the user must have these permission on every platform in the
     * group.
     * @param subject The user to check permissions on.
     * @param id An ID of a platform or a group of platforms.
     * @exception GroupNotCompatibleException If the group is not a compatible
     *            group.
     * @exception SystemException If the group is empty or is not a group of
     *            platforms.
     */
    public void checkAIScanPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, GroupNotCompatibleException {

        // Check permissions - subject must have modify platform
        // permission on the platform in question (or, if it's a group, the
        // subject must have modify platform permissions on all platforms
        // in the group), AND the global "add server" permission.
        if (id.isPlatform()) {
            checkAIScanPermissionForPlatform(subject, id);

        } else if (id.isGroup()) {

            // Check permissions for EVERY platform in the group
            List groupMembers;
            try {
                groupMembers = GroupUtil.getCompatGroupMembers(subject, id, null,
                    PageControl.PAGE_ALL);
            } catch (AppdefEntityNotFoundException e) {
                // should never happen
                throw new SystemException("Error finding group: " + id, e);
            }
            if (groupMembers.isEmpty()) {
                throw new SystemException("Can't perform autoinventory " + "scan on an empty group");
            }

            for (Iterator i = groupMembers.iterator(); i.hasNext();) {
                AppdefEntityID platformEntityID = (AppdefEntityID) i.next();
                checkAIScanPermissionForPlatform(subject, platformEntityID);
            }
        } else {
            throw new SystemException("Autoinventory scans may only be "
                                      + "performed on platforms and groups " + "of platforms");
        }
    }

    /**
     * Chec to see if the subject can perform an autoinventory scan on a
     * platform. Don't use this method - instead use checkAIScanPermission which
     * will call this method as necessary.
     */
    private void checkAIScanPermissionForPlatform(AuthzSubject subject, AppdefEntityID platformID)
        throws PermissionException {

        AppdefResourcePermissions arp = getResourcePermissions(subject, platformID);

        if (arp.canCreateChild() && arp.canModify()) {
            // ok, legal operation
        } else {
            // boom, no permissions
            throw new PermissionException("User " + subject.getName() +
                                          " is not permitted to start an " +
                                          "autoinventory scan on platform " + platformID);
        }
    }

    /**
     * Check for createPlatform permission for a resource
     * @param subject
     * @throws PermissionException
     * 
     * 
     */
    public void checkCreatePlatformPermission(AuthzSubject subject) throws PermissionException {
        try {
            checkPermission(subject, getResourceType(AuthzConstants.rootResType),
                AuthzConstants.rootResourceId, AuthzConstants.platformOpCreatePlatform);
        } catch (NotFoundException e) {
            // seed data error if this is not there
            throw new SystemException(e);
        }
    }

    public void checkCreateProfilePermission(AuthzSubject subject) throws PermissionException {
//        checkPermission(subject, getResourceTypeDAO().findById(AuthzConstants.authzProfile),
//           AuthzConstants.rootResourceId, AuthzConstants.profileOpCreateProfile);
    }

    /**
     * Check for control permission for a given resource
     * 
     * 
     */
    public void checkControlPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpControlPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpControlServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpControlService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpControlApplication;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpControlResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        // now check
        checkPermission(subject, id, opName);
    }

    /**
     * Get the AppdefResourcePermissions for a given resource
     * 
     * 
     * 
     * XXX: DON'T USE THIS!!
     * @deprecated Use the individual check*Permission methods instead.
     * 
     */
    @Deprecated
    public AppdefResourcePermissions getResourcePermissions(AuthzSubject who, AppdefEntityID eid) {
        boolean canView = false;
        boolean canModify = false;
        boolean canCreateChild = false;
        boolean canRemove = false;
        boolean canMonitor = false;
        boolean canControl = false;
        boolean canAlert = false;
        try {
            checkViewPermission(who, eid);
            canView = true;
        } catch (PermissionException e) {
        }
        try {
            checkModifyPermission(who, eid);
            canModify = true;
        } catch (PermissionException e) {
        }
        try {
            checkRemovePermission(who, eid);
            canRemove = true;
        } catch (PermissionException e) {
        }
        try {
            checkControlPermission(who, eid);
            canControl = true;
        } catch (PermissionException e) {
        }
        try {
            checkMonitorPermission(who, eid);
            canMonitor = true;
        } catch (PermissionException e) {
        }
        try {
            checkAlertingPermission(who, eid);
            canAlert = true;
        } catch (PermissionException e) {
        }
        try {
            if (!eid.isService()) {
                checkCreateChildPermission(who, eid);
                canCreateChild = true;
            }
        } catch (PermissionException e) {
        } catch (InvalidAppdefTypeException e) {
        }
        // finally create the object
        return new AppdefResourcePermissions(who, eid, canView, canCreateChild, canModify,
            canRemove, canControl, canMonitor, canAlert);
    }

    /**
     * Check for control permission for a given resource
     * 
     * 
     */
    public void checkRemovePermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpRemovePlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpRemoveServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpRemoveService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpRemoveApplication;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpRemoveResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        // now check
        checkPermission(subject, id, opName);
    }

    /**
     * Check for monitor permission for a given resource
     * 
     * 
     */
    public void checkMonitorPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpMonitorPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpMonitorServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpMonitorService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpMonitorApplication;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpMonitorResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        // now check
        checkPermission(subject, id, opName);
    }

    /**
     * Check for manage alerts permission for a given resource
     * 
     * 
     */
    public void checkAlertingPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        getAlertPermissionManager().canFixAcknowledgeAlerts(subject, id);
    }

    /**
     * Check the scope of alertable resources for a give subject
     * @return a list of AppdefEntityIds
     */
    public List<AppdefEntityID> checkAlertingScope(AuthzSubject subj) {
        final Collection<ResourceType> resourceTypes = new ArrayList<ResourceType>();
        resourceTypes.add(getResourceTypeDAO().get(AuthzConstants.authzPlatform));
        resourceTypes.add(getResourceTypeDAO().get(AuthzConstants.authzServer));
        resourceTypes.add(getResourceTypeDAO().get(AuthzConstants.authzService));
        resourceTypes.add(getResourceTypeDAO().get(AuthzConstants.authzGroup));
        resourceTypes.add(getResourceTypeDAO().get(AuthzConstants.authzApplication));
        return findViewableInstances(subj, resourceTypes);
    }

    /**
     * @return Map of {@link Integer} representing the AppdefTypeID to the count of associated resources which are
     * viewable by the {@link AuthzSubject}
     */
    public abstract TypeCounts findViewableInstanceCounts(AuthzSubject subj, Collection<ResourceType> types);

    public abstract List<AppdefEntityID> findViewableInstances(AuthzSubject subj, Collection<ResourceType> types);

    /**
     * Check for create child object permission for a given resource Child
     * Resources: Platforms -> servers Servers -> services Any other resource
     * will throw an InvalidAppdefTypeException since no other resources have
     * this parent->child relationship with respect to their permissions
     * @param subject
     * @param id - what
     * @param subject - who
     * 
     * 
     */
    public void checkCreateChildPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpAddServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpAddService;
                break;
            default:
                throw new InvalidAppdefTypeException("Type: " + type +
                                                     " does not support child resource creat operations");
        }
        // now check
        checkPermission(subject, id, opName);
    }

    /**
     * Find an operation by name inside a ResourceTypeValue object
     * @param rtV - the resource type value object
     * @return operationId
     * @throws PermissionException - if the op is not found
     */
    private Integer getOpIdByResourceType(ResourceType rtV, String opName)
        throws PermissionException {
        Collection<Operation> ops = rtV.getOperations();
        for (Operation op : ops) {
            if (op.getName().equals(opName)) {
                return op.getId();
            }
        }
        throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " +
                                      rtV.getName());
    }

    /**
     * Check for modify permission for a given resource
     * 
     * 
     */
    public void checkModifyPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName;

        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpModifyPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpModifyServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpModifyService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpModifyApplication;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpModifyResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }

        // now check
        checkPermission(subject, id, opName);
    }

    public void checkModifyPermission(Integer subjectId, AppdefEntityID id)
        throws PermissionException {
        String opName = null;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpModifyPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpModifyServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpModifyService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpModifyResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + id.getType());
        }
        check(subjectId, id.getAuthzTypeName(), id.getId(), opName);

    }

    /**
     * Check a permission
     */
    public void checkPermission(AuthzSubject subject, AppdefEntityID id, String operation)
        throws PermissionException {
        ResourceType rtv = null;
        try {
            // get the resource type
            rtv = getAuthzResourceType(id);
        } catch (Exception e) {
            throw new PermissionException(e);
        }

        // never wrap permission exception unless absolutely necessary
        Integer instanceId = id.getId();
        // now call the protected method
        checkPermission(subject, rtv, instanceId, operation);
    }

    /**
     * Get the authz resource type by AppdefEntityId
     */
    protected ResourceType getAuthzResourceType(AppdefEntityID id) throws NotFoundException {
        int type = id.getType();
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return getPlatformResourceType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return getServerResourceType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return getServiceResourceType();
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return getApplicationResourceType();
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return getGroupResourceType();
            default:
                throw new InvalidAppdefTypeException("Type: " + type + " unknown");
        }
    }

    /**
     * Get the platform resource type
     * @return platformResType
     */
    protected ResourceType getPlatformResourceType() throws NotFoundException {
        return getResourceType(AuthzConstants.platformResType);
    }

    /**
     * Get the application resource type
     * @return applicationResType
     */
    protected ResourceType getApplicationResourceType() throws NotFoundException {
        return getResourceType(AuthzConstants.applicationResType);
    }

    /**
     * Get the Server Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceType getServerResourceType() throws NotFoundException {
        return getResourceType(AuthzConstants.serverResType);
    }

    /**
     * Get the Service Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceType getServiceResourceType() throws NotFoundException {
        return getResourceType(AuthzConstants.serviceResType);
    }

    /**
     * Get the Authz Resource Type for a Group
     * @return ResourceTypeValue
     */
    public ResourceType getGroupResourceType() throws NotFoundException {
        return getResourceType(AuthzConstants.groupResourceTypeName);
    }

    /**
     * Get the authz resource type
     * @param resType - the constant indicating the resource type (from
     *        AuthzConstants)
     */
    protected ResourceType getResourceType(String resType) throws NotFoundException {
        return Bootstrap.getBean(ResourceManager.class).findResourceTypeByName(resType);
    }

    /**
     * Check for view permission for a given resource
     * 
     * 
     */
    public void checkViewPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpViewPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpViewServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpViewService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpViewApplication;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpViewResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        // now check
        checkPermission(subject, id, opName);
    }

    /**
     * Check permission.
     * 
     * @param subject The subject.
     * @param type The type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operation The operation (as a String) that the subject may want to
     *        perform.
     * @exception PermissionException If subject is not authorized to perform
     *            the given operation on the resource of the given type whose id
     *            is instanceId.
     */
    public abstract void check(Integer subject, ResourceType type, Integer instanceId,
                               String operation) throws PermissionException;

    /**
     * Check permission.
     * 
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want to
     *        perform.
     * @exception PermissionException If subject is not authorized to perform
     *            the given operation on the resource of the given type whose id
     *            is instanceId.
     */
    public abstract void check(Integer subjectId, Integer typeId, Integer instanceId,
                               Integer operationId) throws PermissionException;
    

    /**
     * Check permission.
     * 
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param operationId ID of the operation that the subject may want to
     *        perform.
     * @exception PermissionException If subject is not authorized to perform
     *            the given operation on the resource of the given type whose id
     *            is instanceId.
     */
    public abstract void check(Integer subjectId, Integer typeId, 
                               Integer operationId) throws PermissionException;
    

    /**
     * Check permission.
     * 
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want to
     *        perform.
     * @exception PermissionException If subject is not authorized to perform
     *            the given operation on the resource of the given type whose id
     *            is instanceId.
     */
    public abstract void check(Integer subjectId, String resType, Integer instanceId,
                               String operation) throws PermissionException;

    /**
     * Check whether a user has permission to access the admin component.
     * 
     * @return true - if user has administerCAM operation false otherwise
     */
    public abstract boolean hasAdminPermission(Integer who);

    /**
     * Check to see if user can see role dashboards
     */
    public abstract boolean hasGuestRole();

    /**
     * Find the list of instance ids for which a given subject id has the named
     * operation in one of their roles or owns a resource for which the
     * operation is valid
     * @return List of integer instance ids
     */
    public abstract Collection<Integer> findOperationScopeBySubject(AuthzSubject subj, String opName,
                                                                    String resType)
        throws PermissionException, NotFoundException;

    /**
     * Find the list of instance ids for which a given subject id has a given
     * operation.
     * @return List of integer instance ids
     */
    public abstract Collection<Integer> findOperationScopeBySubject(AuthzSubject subj, Integer opId)
        throws PermissionException, NotFoundException;

    /**
     * Find the list of resources for which a given subject id can perform
     * specified operation. This method operates on a batch of resources and
     * their corresponding operations. Unlike, other findOperScopeBySubj
     * methods, this one operates on any type of resource and thus the
     * "resource and operation" tuple should be expressed by common index.
     * 
     * @param whoami - subject
     * @param resArr - batch of resources to verify
     * @param opArr - corresponding batch of operations
     * @return array of authz Resources
     * @exception ApplicationException
     */
    public abstract Resource[] findOperationScopeBySubjectBatch(AuthzSubject whoami,
                                                                ResourceValue[] resArr,
                                                                String[] opArr)
        throws ApplicationException;

    /**
     * @return {@link Set} of viewable {@link Resource}Ids
     */
    public abstract Set<Integer> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes);

    /**
     * @return {@link Set} of viewable {@link Platform}, {@link Server} and {@link Service} {@link Resource}Ids
     */
    public abstract Set<Integer> findViewablePSSResources(AuthzSubject subj);

    /**
     * Search viewable resources of any type
     * @return a list of Integers representing instance ids
     * @deprecated
     */
    @Deprecated
    public abstract List<Integer> findViewableResources(AuthzSubject subj, String searchFor,
                                                        PageControl pc);

    /**
     * Get a clause that you can append to an existing WHERE clause to make it
     * authz-aware. Note that your WHERE clause must include at least 1
     * condition, as the value returned from this method begins with 'AND'.
     * Also, the alias of the EAM_RESOURCE table is assumed to be 'res'.
     * 
     * @return a clause that can be appended to a WHERE clause to query against
     *         authz data.
     */
    public abstract String getSQLWhere(Integer subjectId);

    /**
     * Get all operations for a given subject
     * 
     * @return a list of Integers representing instance ids
     */
    public abstract List<Operation> getAllOperations(AuthzSubject subject, PageControl pc)
        throws PermissionException;

    public abstract String getOperableGroupsHQL(AuthzSubject subject, String alias, String oper);

    public abstract Collection<Resource> getGroupResources(Integer subjectId, Integer groupId,
                                                           Boolean fsystem);

    public abstract Collection<Resource> findServiceResources(AuthzSubject subj, Boolean fsystem);

    public interface RolePermNativeSQL {
        String getSQL();

        Query bindParams(Query q, AuthzSubject subject, List viewResourcesOperations, List manageAlertOperations);
    }

    public abstract RolePermNativeSQL  getRolePermissionNativeSQL(String resourceVar, String eventLogVar,
                                           String subjectParam,
                                           String opListViewResourcesParam,
                                           String opListManageAlertsParam); 

    public abstract String getAlertsHQL(boolean inEscalation, boolean notFixed, Integer groupId,
                                        Integer resourceId, Integer alertDefId, boolean count);

    public abstract String getAlertDefsHQL();

    public abstract String getGroupAlertsHQL(boolean inEscalation, boolean notFixed,
                                             Integer groupId, Integer galertDefId);

    public abstract String getGroupAlertDefsHQL();

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters. Used for a SQL query.
     * @param includeDescendants - include the resource's descendants in the
     *        query
     */
    public EdgePermCheck makePermCheckSql(String resourceVar, boolean includeDescendants) {
        return makePermCheckSql("subject", resourceVar, "resource", "distance", "ops",
            includeDescendants);
    }

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters. Used for a HQL query.
     * @param includeDescendants - include the resource's descendants in the
     *        query
     */
    public EdgePermCheck makePermCheckHql(String resourceVar, boolean includeDescendants) {
        return makePermCheckHql("subject", resourceVar, "resource", "distance", "ops",
            includeDescendants);
    }

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking SQL. This is the SQL version of makePermCheckHql
     * 
     * This method spits out a piece of SQL, like: JOIN EAM_RESOURCE_EDGE edge
     * ON edge.TO_ID = resId edge.FROM_ID = resId WHERE (resId = :resParam AND
     * edge.distance >= :distParam AND resSubjId = :subjParam AND ... AND ...)
     * 
     * Therefore, it must used between the select and last parts of the where
     * clause, preceded by an 'and'
     * 
     * The arguments ending with 'Param' are used to identify names of Query
     * parameters which will later passed in. (e.g.
     * query.setParameter("subject", s)
     * 
     * The arguments ending in 'Var' are the SQL variable names used straight in
     * the SQL text. (e.g. "select rez from Resource rez "... , you would
     * specify the name of your resourceVar as 'rez')
     * @param includeDescendants - include the resource's descendants in the
     *        query
     */
    public abstract EdgePermCheck makePermCheckSql(String subjectParam, String resourceVar,
                                                   String resourceParam, String distanceParam,
                                                   String opsParam, boolean includeDescendants);

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking HQL.
     * 
     * This method spits out a piece of HQL, like: join r.toEdges _e ... where
     * _e.fromDistance >= :distance (could be '=' based on includeDescendants)
     * and ... and ...
     * 
     * Therefore, it must used between the select and last parts of the where
     * clause, preceded by an 'and'
     * 
     * The arguments ending with 'Param' are used to identify names of Query
     * parameters which will later passed in. (e.g.
     * query.setParameter("subject", s)
     * 
     * The arguments ending in 'Var' are the SQL variable names used straight in
     * the SQL text. (e.g. "select rez from Resource rez "... , you would
     * specify the name of your resourceVar as 'rez')
     * @param includeDescendants - include the resource's descendants in the
     *        query
     */
    public abstract EdgePermCheck makePermCheckHql(String subjectParam, String resourceVar,
                                                   String resourceParam, String distanceParam,
                                                   String opsParam, boolean includeDescendants);

    /**
     * Return the MaintenanceEventManager implementation
     */
    public abstract MaintenanceEventManager getMaintenanceEventManager();

    /**
     * Return the HierarchicalAlertingManager implementation
     */
    public abstract HierarchicalAlertingManager getHierarchicalAlertingManager();

    /**
     * @return true if the subj either overlord, hqadmin or associated with a
     *         superuser role
     */
    public boolean isSuperUser(AuthzSubject subj) {
        if (null == subj) return false;
        if (subj.getId().equals(AuthzConstants.overlordId)) {
            return true;
        }
        // may not be in a transaction, so need to make sure that we have a
        // hibernate session by going into the manager
        final RoleManager roleManager = Bootstrap.getBean(RoleManager.class);
        final Collection<Role> roles = roleManager.getRoles(subj);
        return rolesContainSuperuserRoles(roles);
    }
    
    
    protected boolean rolesContainSuperuserRoles(Collection<Role> roles) {
        if (null == roles) return false;
        for(Role role:roles) {
            if (roleIsSuperuserRole(role)) {
                return true;
            }
        }
        return false;
    }    

    protected boolean roleIsSuperuserRole(final Role role) {
        return role.getId().equals(AuthzConstants.rootRoleId);
    }

    public void checkIsSuperUser(AuthzSubject subject) throws PermissionException {
        if (isSuperUser(subject)) {
            return;
        }
        throw new PermissionException(subject.getName() + " does not have super user priviledge");
    }

    /**
     * This method saves processing since it gives the ability to easily intercept the resultset during
     * the while(rs.next()) loop and has the ability to filter out unwanted resources by returning null from convert()
     * @param transformer converts the returned {@link Set} into any type specified by the transformer.  If the call to
     * convert() returns null, the value will not be returned in the resulting {@link Set}.
     * @return {@link Set} of objects, type is determined by the generic type of the specified {@link IntegerTransformer}
     */
    public abstract <T> Set<T> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes,
                                                     IntegerTransformer<T> transformer);

    /**
     * This method saves processing since it gives the ability to easily intercept the resultset during
     * the while(rs.next()) loop and has the ability to filter out unwanted resources by returning null from convert()
     * @param transformer converts the returned {@link Set} into any type specified by the transformer.  If the call to
     * convert() returns null, the value will not be returned in the resulting {@link Set}.
     * @param sortName one of {@link PageControl} SORT_ASC, SORT_DESC, SORT_UNSORTED, sorts on {@link Resource} name
     * @return {@link Set} of objects, type is determined by the generic type of the specified {@link IntegerTransformer}
     */
    public abstract <T> Set<T> findViewableResources(AuthzSubject subject, Collection<ResourceType> types,
                                                     int sortName, IntegerTransformer<T> integerConverter);

    /**
     * This method saves processing since it gives the ability to easily intercept the resultset during
     * the while(rs.next()) loop and has the ability to filter out unwanted resources by returning null from convert()
     * @param transformer converts the returned {@link Set} into any type specified by the transformer.  If the call to
     * convert() returns null, the value will not be returned in the resulting {@link Set}.
     * @param sortName one of {@link PageControl} SORT_ASC, SORT_DESC, SORT_UNSORTED, sorts on {@link Resource} name
     * @param comparator extra sorting that may be applied to the returned {@link Set}
     * @return {@link Set} of objects, type is determined by the generic type of the specified {@link IntegerTransformer}
     */
    public abstract <T> Set<T> findViewableResources(AuthzSubject subject, Collection<ResourceType> types,
                                                     int sortName, IntegerTransformer<T> transformer,
                                                     Comparator<T> comparator);
    
    public abstract <T> Set<T> findResourcesByOperationIds(AuthzSubject subj, Collection<Integer> operationIds,
                                                           IntegerTransformer<T> transformer);
    
    public abstract <T> Set<T> findViewableResources(AuthzSubject subj, Collection<Role> roles,
            Collection<ResourceType> types,  IntegerTransformer<T> transformer);
    

}
