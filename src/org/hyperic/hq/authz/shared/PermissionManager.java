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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.Query;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.CloningBoss;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.PagerProcessor_operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.HierarchicalAlertingManager;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.hyperic.util.pager.PageControl;

public abstract class PermissionManager   {

    public static final String OPERATION_PAGER =
        PagerProcessor_operation.class.getName();
    
    private InitialContext _ic = null;
    
    protected InitialContext getInitialContext() throws NamingException {
        if (_ic == null)
            _ic = new InitialContext();
        return _ic;
    }
    
    protected ResourceTypeDAO getResourceTypeDAO() {
        return Bootstrap.getBean(ResourceTypeDAO.class);
    }
    
    protected OperationDAO getOperationDAO() {
        return Bootstrap.getBean(OperationDAO.class);
    }
    
    protected ResourceDAO getResourceDAO() {
        return Bootstrap.getBean(ResourceDAO.class);
    }
    
    /**
     * Check a permission 
     * @param subject - who
     * @param rtV - type of resource
     * @param id - the id of the object
     * @param operation - the name of the operation to perform
     */
    protected void checkPermission(AuthzSubject subject, ResourceType rtV,
                                   Integer id, String operation)
        throws PermissionException 
    {
        Integer opId = getOpIdByResourceType(rtV, operation);
        check(subject.getId(), rtV.getId(), id, opId);
    }
    
    /**
     * Check for control permission for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void checkControlPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException 
    {
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
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     *
     * XXX: DON'T USE THIS!!
     * @deprecated Use the individual check*Permission methods instead.
     *
     */ 
    public AppdefResourcePermissions 
        getResourcePermissions(AuthzSubject who, AppdefEntityID eid)
        throws FinderException {
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
            return new AppdefResourcePermissions(who, eid,
                                                 canView,
                                                 canCreateChild,
                                                 canModify,
                                                 canRemove,
                                                 canControl,
                                                 canMonitor,
                                                 canAlert);
    }
    
    /**
     * Check for control permission for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="Required"
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
     * @ejb:interface-method
     * @ejb:transaction type="Required"
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
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void checkAlertingPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpManageAlerts;
                break;                
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        // now check
        checkPermission(subject, id, opName);
    }
    /**
     * Check for create child object permission for a given resource
     * Child Resources:
     * Platforms -> servers
     * Servers -> services
     * Any other resource will throw an InvalidAppdefTypeException since no other
     * resources have this parent->child relationship with respect to their permissions
     * @param subject 
     * @param id - what
     * @param subject - who
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void checkCreateChildPermission(AuthzSubject subject,
                                           AppdefEntityID id)
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
                throw new InvalidAppdefTypeException("Type: " +
                    type + " does not support child resource creat operations");
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
            Collection ops = rtV.getOperations();
            for(Iterator it = ops.iterator(); it.hasNext(); ) {
                Operation op = (Operation) it.next();
                if(op.getName().equals(opName)) {
                    return op.getId();
                }
            }
            throw new PermissionException("Operation: " + opName 
                + " not valid for ResourceType: " + rtV.getName());
    }
    
    /**
     * Check for modify permission for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void checkModifyPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException 
    {
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
    
    /**
     * Check a permission
     */
    protected void checkPermission(AuthzSubject subject, AppdefEntityID id,
                                   String operation) 
        throws PermissionException 
    {
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
    protected ResourceType getAuthzResourceType(AppdefEntityID id)
        throws FinderException {
        int type = id.getType();
        switch(type) {
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
                throw new InvalidAppdefTypeException("Type: " + type +
                                                     " unknown");
        }
    }
    
    /**
     * Get the platform resource type
     * @return platformResType
     */
    protected ResourceType getPlatformResourceType() 
        throws FinderException {
        return getResourceType(AuthzConstants.platformResType);
    }
    
    /**
     * Get the application resource type
     * @return applicationResType
     */
    protected ResourceType getApplicationResourceType() 
        throws FinderException {
        return getResourceType(AuthzConstants.applicationResType);
    }

    /**
     * Get the Server Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceType getServerResourceType() 
        throws FinderException {
        return getResourceType(AuthzConstants.serverResType);
    }

    /**
     * Get the Service Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceType getServiceResourceType() 
        throws FinderException {
        return getResourceType(AuthzConstants.serviceResType);
    }

    /**
     * Get the Authz Resource Type for a Group
     * @return ResourceTypeValue
     */
     public ResourceType getGroupResourceType()
         throws FinderException {
         return getResourceType(AuthzConstants.groupResourceTypeName);
     }
     
     /**
      * Get the authz resource type 
      * @param resType - the constant indicating the resource type
      * (from AuthzConstants)
      */
     protected ResourceType getResourceType(String resType) 
         throws FinderException {
         return ResourceManagerImpl.getOne().findResourceTypeByName(resType);
     }
     
     /**
      * Check for view permission for a given resource
      * @ejb:interface-method
      * @ejb:transaction type="Required"
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
     * @param operation The operation (as a String) that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subject, ResourceType type,
                      Integer instanceId, String operation)
        throws PermissionException;

    /**
     * Check permission.
     *
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subjectId, Integer typeId,
                               Integer instanceId, Integer operationId)
        throws PermissionException;

    /**
     * Check permission.
     *
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subjectId, String resType,
                               Integer instanceId, String operation)
        throws PermissionException;

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
     * Find the list of instance ids for which a given subject id
     * has the named operation in one of their roles or owns a resource
     * for which the operation is valid
     * @return List of integer instance ids
     */
    public abstract List<Integer>
        findOperationScopeBySubject(AuthzSubject subj, String opName,
                                    String resType)
        throws FinderException, PermissionException;

    /**
     * Find the list of instance ids for which a given subject id
     * has a given operation.
     * @return List of integer instance ids
     */
    public abstract List<Integer>
        findOperationScopeBySubject(AuthzSubject subj, Integer opId)
        throws FinderException, PermissionException;

    /**
     * Find the list of resources for which a given subject id can perform
     * specified operation. This method operates on a batch of resources and
     * their corresponding operations. Unlike, other findOperScopeBySubj
     * methods, this one operates on any type of resource and thus the
     * "resource and operation" tuple should be expressed by common index.
     *
     * @param whoami - subject
     * @param resArr - batch of resources to verify
     * @param opArr  - corresponding batch of operations
     * @return array of authz Resources
     * @exception FinderException
     */
    public abstract Resource[]
        findOperationScopeBySubjectBatch(AuthzSubject whoami,
                                         ResourceValue[] resArr,
                                         String[] opArr)
        throws FinderException;

    /**
     * Get viewable resources of a specific type
     * @param resType the authz resource type name
     * @param resName if result should filter by resource name
     * @param appdefTypeStr the Appdef type name, like 'platform', 'server', etc
     * @param typeId the appdef type ID, e.g. the platform_type_id
     *
     * @return a list of Integers representing instance ids
     */
    public abstract List<Integer> findViewableResources(AuthzSubject subj,
                                               String resType,
                                               String resName,
                                               String appdefTypeStr,
                                               Integer typeId,
                                               PageControl pc);

    /**
     * Search viewable resources of any type
     * @return a list of Integers representing instance ids
     */
    public abstract List<Integer> findViewableResources(AuthzSubject subj,
                                               String searchFor,
                                               PageControl pc);

    /**
     * Get a clause that you can append to an existing WHERE clause to make it
     * authz-aware.  Note that your WHERE clause must include at least 1
     * condition, as the value returned from this method begins with 'AND'.
     * Also, the alias of the EAM_RESOURCE table is assumed to be 'res'.
     *
     * @return a clause that can be appended to a WHERE clause to query against
     *  authz data.
     */
    public abstract String getSQLWhere(Integer subjectId);

    /**
     * Get all operations for a given subject
     *
     * @return a list of Integers representing instance ids
     */
    public abstract List<Operation>
        getAllOperations(AuthzSubject subject, PageControl pc)
        throws PermissionException, FinderException;

    public abstract String getResourceTypeSQL(String instanceId,
                                              Integer subjectId,
                                              String resType,
                                              String op);

    public abstract String getOperableGroupsHQL(AuthzSubject subject,
                                                String alias,
                                                String oper);

    public abstract Collection<Resource>
        getGroupResources(Integer subjectId, Integer groupId, Boolean fsystem);

    public abstract Collection<Resource>
        findServiceResources(AuthzSubject subj, Boolean fsystem);

    public interface RolePermNativeSQL {
        String getSQL();
        Query bindParams(Query q, AuthzSubject subject, List operations);
    }

    public abstract RolePermNativeSQL
        getRolePermissionNativeSQL(String resourceVar, String subjectParam,
                                   String opListParam);


    public abstract String getAlertsHQL(boolean inEscalation, boolean notFixed,
                                        Integer groupId, Integer alertDefId,
                                        boolean count);

    public abstract String getAlertDefsHQL();

    public abstract String getGroupAlertsHQL(boolean inEscalation, boolean notFixed,
                                             Integer groupId, Integer galertDefId);

    public abstract String getGroupAlertDefsHQL();

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters.  Used for a SQL query.
     * @param includeDescendants - include the resource's descendants in the query
     */
    public EdgePermCheck makePermCheckSql(String resourceVar,
                                          boolean includeDescendants) {
        return makePermCheckSql("subject", resourceVar, "resource",
                                "distance", "ops", includeDescendants);
    }

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters.  Used for a HQL query.
     * @param includeDescendants - include the resource's descendants in the query
     */
    public EdgePermCheck makePermCheckHql(String resourceVar,
                                          boolean includeDescendants) {
        return makePermCheckHql("subject", resourceVar, "resource",
                                "distance", "ops", includeDescendants);
    }

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking SQL.  This is the SQL version of makePermCheckHql
     *
     * This method spits out a piece of SQL, like:
     *  JOIN EAM_RESOURCE_EDGE edge ON edge.TO_ID = resId edge.FROM_ID = resId
     *  WHERE (resId = :resParam
     *  AND edge.distance >= :distParam
     *  AND resSubjId = :subjParam
     *  AND ...
     *  AND ...)
     *
     * Therefore, it must used between the select and last parts of the
     * where clause, preceded by an 'and'
     *
     * The arguments ending with 'Param' are used to identify names of
     * Query parameters which will later passed in.
     *   (e.g. query.setParameter("subject", s)
     *
     * The arguments ending in 'Var' are the SQL variable names used
     * straight in the SQL text.
     *   (e.g.  "select rez from Resource rez "... , you would specify
     *    the name of your resourceVar as 'rez')
     * @param includeDescendants - include the resource's descendants in the query
     */
    public abstract EdgePermCheck makePermCheckSql(String subjectParam,
                                                   String resourceVar,
                                                   String resourceParam,
                                                   String distanceParam,
                                                   String opsParam,
                                                   boolean includeDescendants);

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking HQL.
     *
     * This method spits out a piece of HQL, like:
     *   join r.toEdges _e
     *  ...
     *  where _e.fromDistance >= :distance (could be '=' based on includeDescendants)
     *   and ...
     *   and ...
     *
     * Therefore, it must used between the select and last parts of the
     * where clause, preceded by an 'and'
     *
     * The arguments ending with 'Param' are used to identify names of
     * Query parameters which will later passed in.
     *   (e.g. query.setParameter("subject", s)
     *
     * The arguments ending in 'Var' are the SQL variable names used
     * straight in the SQL text.
     *   (e.g.  "select rez from Resource rez "... , you would specify
     *    the name of your resourceVar as 'rez')
     * @param includeDescendants - include the resource's descendants in the query
     */
    public abstract EdgePermCheck makePermCheckHql(String subjectParam,
                                                   String resourceVar,
                                                   String resourceParam,
                                                   String distanceParam,
                                                   String opsParam,
                                                   boolean includeDescendants);

    /**
     * Return the MaintenanceEventManager implementation
     */
    public abstract MaintenanceEventManager getMaintenanceEventManager();

    /**
     * Return the CloningBoss implementation
     */
    public abstract CloningBoss getCloningBoss();

    /**
     * Return the HierarchicalAlertingManager implementation
     */
    public abstract HierarchicalAlertingManager getHierarchicalAlertingManager();

}
