/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceLocal;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.dao.ServiceDAO;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.SortAttribute;

/**
 * Parent abstract class of all appdef session ejbs
 */
public abstract class AppdefSessionEJB 
    extends AppdefSessionUtil
{
    protected SessionContext _ctx;
    protected InitialContext _ic;

    protected Log log = LogFactory.getLog(AppdefSessionEJB.class);

    /**
     * Get the authz resource type 
     * @param resType - the constant indicating the resource type
     * (from AuthzConstants)
     */
    protected ResourceType getResourceType(String resType) 
    	throws FinderException {
        return getResourceManager().findResourceTypeByName(resType);
    }

    /**
     * builds a list of resource types from the list of resources
     * 
     * XXX -- This code is completely rotten.  Needs to be fixed badly.  -- JMT 
     */
    protected Collection filterResourceTypes(Collection resources) {
        // Create TreeSet which sorts on the name
        TreeSet resTypes = new TreeSet(
            new Comparator() {
                private String getName(Object obj) {
                    if (obj instanceof AppdefResourceType)
                        return ((AppdefResourceType) obj).getSortName();

                    return "";
                }
                
                public int compare(Object o1, Object o2) {
                      return getName(o1).compareTo(getName(o2));
                }
            }
        );

        Iterator iterator = resources.iterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof AppdefResourceLocal) {
                AppdefResourceLocal resource = (AppdefResourceLocal)o;
                EJBLocalObject rTypeLocal = resource.getAppdefResourceType();
                if (!resTypes.contains(rTypeLocal))
                    resTypes.add(rTypeLocal);
            } else if (o instanceof Platform) {
                PlatformType st = ((Platform)o).getPlatformType();
                if (!resTypes.contains(st))
                    resTypes.add(st);
            } else if (o instanceof Server) {
                ServerType st = ((Server)o).getServerType();
                if (!resTypes.contains(st))
                    resTypes.add(st);
            } else if (o instanceof Service) {
                ServiceType st = ((Service)o).getServiceType();
                if (!resTypes.contains(st))
                    resTypes.add(st);
            }
        }
        return resTypes;
    }

    /**
     * Create an authz resource
     *
     * @param resTypeVal - the type
     * @param id - the id of the object
     */
    protected void createAuthzResource(AuthzSubject who,
                                       ResourceType resType,
                                       Resource prototype,
                                       Integer id,  String name,
                                       Resource parent) 
    {
        createAuthzResource(who, resType, prototype, id, name, false, parent);
    }
    
    /**
     * Create an authz resource
     *
     * @param resTypeVal - the type
     * @param who - who
     * @param id - the id of the object
     * @param name - the name of the resource
     * @param fsystem - true if the resource should be non-visible
     */
    protected void createAuthzResource(AuthzSubject who, ResourceType resType,
                                       Resource prototype, Integer id,
                                       String name, boolean fsystem,
                                       Resource parent) 
    {
        getResourceManager().createResource(who, resType, prototype, id, name, 
                                            fsystem, parent);
    }

    /**
     * Update the authz resource. Used to update the name in the authz resource
     * table
     */
    protected void updateAuthzResource(ResourceValue rv)
        throws UpdateException {
        getResourceManager().saveResource(rv);
    }

    /**
     * Retrieve the ResourceValue object for a given Appdef Object
     */
    protected ResourceValue getAuthzResource(ResourceType rtV, Integer id)
        throws FinderException {
        return getResourceManager().findResourceByInstanceId(rtV, id);
    }

    /**
     * Retrieve the Resource POJO for a given Appdef Entity ID
     */
    protected Resource getAuthzResource(AppdefEntityID aeid) {
        return getResourceManager().findResource(aeid);
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
                throw new InvalidAppdefTypeException("Type: " + 
                    type + " unknown");
        }
    }
        
    /**
     * remove the authz resource entry
     */
    protected void removeAuthzResource(AuthzSubject subject,
                                       AppdefEntityID aeid)
        throws RemoveException, PermissionException, VetoException 
    {
        if (log.isDebugEnabled())
            log.debug("Removing authz resource: " + aeid);
        
        ResourceManagerLocal rm = getResourceManager();
        AuthzSubject s = 
            AuthzSubjectManagerEJBImpl.getOne().findSubjectById(subject.getId()); 
        rm.removeResources(s, new AppdefEntityID[] { aeid });
        
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(subject, aeid);
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }

    /**
     * Find a ApplicationTypeLocal by primary key
     * @return ApplicationType
     */
    protected ApplicationType findApplicationTypeByPK(Integer pk)
        throws FinderException, NamingException {
        return getApplicationTypeDAO().findById(pk);
    }

    /**
     * Find a ApplicationLocal by primary key
     * @return Application
     */
    protected Application findApplicationByPK(Integer pk)
        throws ApplicationNotFoundException, NamingException {
        try {
            return getApplicationDAO().findById(pk);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(pk, e);
        }
    }

    /**
     * Find an AppdefGroup by id
     * @return AppdefGroupValue
     * @throw AppdefGroupNotFoundException - when group doesn't exist
     * @throw PermissionException - when subject isn't authz.
     */
    protected AppdefGroupValue findGroupById(AuthzSubject subject,
                                             Integer groupId)
        throws PermissionException 
    {
        ResourceGroupManagerLocal groupMan = 
            ResourceGroupManagerEJBImpl.getOne();
        ResourceGroup group = groupMan.findResourceGroupById(subject, groupId);
        return groupMan.convertGroup(subject, group);
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
        if (log.isDebugEnabled()) {
            log.debug("Checking Permission for operation: " +
                      operation + " ResourceType: " + rtV.getName() +
                      " Id: " + id + " Subject: " + subject.getName());
        }
        PermissionManager permMgr = PermissionManagerFactory.getInstance();
        Integer opId = getOpIdByResourceType(rtV, operation);
        permMgr.check(subject.getId(), rtV.getId(), id, opId);
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
     * Check for createPlatform permission for a resource
     * @param subject
     * @throws PermissionException
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void checkCreatePlatformPermission(AuthzSubject subject)
        throws PermissionException {
        try {    
            checkPermission(subject, getRootResourceType(), 
                            AuthzConstants.rootResourceId, 
                            AuthzConstants.platformOpCreatePlatform);
        } catch (FinderException e) {
            // seed data error if this is not there
            throw new SystemException(e);
        }
    }
    
    /**
     * Check for modify permission for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
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
     * Check the scope of alertable resources for a give subject
     * @return a list of AppdefEntityIds
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List checkAlertingScope(AuthzSubject subj) {
        List entityIds = new ArrayList();
        try {
            PermissionManager pm = PermissionManagerFactory.getInstance();
            // platforms 
            List platIds = 
                pm.findOperationScopeBySubject(subj,
                                               AuthzConstants.platformOpManageAlerts,
                                               AuthzConstants.platformResType,
                                               PageControl.PAGE_ALL);
            for(int i = 0; i < platIds.size(); i++) {
                Integer id = (Integer)platIds.get(i);
                entityIds.add(AppdefEntityID.newPlatformID(id.intValue()));                                                             
            }
            // servers
            List serverIds = 
                pm.findOperationScopeBySubject(subj,
                                               AuthzConstants.serverOpManageAlerts,
                                               AuthzConstants.serverResType,
                                               PageControl.PAGE_ALL);
            for(int i = 0; i < serverIds.size(); i++) {
                Integer id = (Integer)serverIds.get(i);
                entityIds.add(AppdefEntityID.newServerID(id.intValue()));                                                                           
            }
            // services
            List serviceIds =
                pm.findOperationScopeBySubject(subj,
                                               AuthzConstants.serviceOpManageAlerts,
                                               AuthzConstants.serviceResType,
                                               PageControl.PAGE_ALL);
            for(int i = 0; i < serviceIds.size(); i++) {
                Integer id = (Integer)serviceIds.get(i);
                entityIds.add(AppdefEntityID.newServiceID(id.intValue()));
            }
            
            // Groups
            List groupids = 
                pm.findOperationScopeBySubject(subj, 
                                               AuthzConstants.groupOpManageAlerts,
                                               AuthzConstants.groupResType,
                                               PageControl.PAGE_ALL);
            for (int i=0; i<groupids.size(); i++) {
                Integer id = (Integer)groupids.get(i);
                entityIds.add(AppdefEntityID.newGroupID(id));
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }
        return entityIds;                
    }

    /**
     * Check to see if the subject can perform an autoinventory scan
     * on the specified resource.  For platforms, the user must have
     * modify platform permissions on the platform, and add server
     * permissions on the platform.  For a group, the user must have
     * these permission on every platform in the group.
     * @param subject The user to check permissions on.
     * @param id An ID of a platform or a group of platforms.
     * @exception GroupNotCompatibleException If the group is not a compatible
     * group.
     * @exception SystemException If the group is empty or is not a group
     * of platforms.
     */
    public void checkAIScanPermission(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, GroupNotCompatibleException {

        int type = id.getType();

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
                groupMembers = GroupUtil.getCompatGroupMembers(
                    subject, id, null, PageControl.PAGE_ALL);
            } catch (AppdefEntityNotFoundException e) {
                // should never happen
                throw new SystemException("Error finding group: " + id, e);
            }
            if (groupMembers.isEmpty()) {
                throw new SystemException("Can't perform autoinventory " +
                                          "scan on an empty group");
            }

            for (Iterator i = groupMembers.iterator(); i.hasNext();) {
                AppdefEntityID platformEntityID = (AppdefEntityID) i.next();
                checkAIScanPermissionForPlatform(subject, platformEntityID);
            }
        } else {
            throw new SystemException("Autoinventory scans may only be " +
                                      "performed on platforms and groups " +
                                      "of platforms");
        }
    }

    /**
     * Chec to see if the subject can perform an autoinventory scan
     * on a platform.  Don't use this method - instead use checkAIScanPermission
     * which will call this method as necessary.
     */
    private void checkAIScanPermissionForPlatform(AuthzSubject subject,
                                                  AppdefEntityID platformID)
        throws PermissionException {
        
        AppdefResourcePermissions arp;
        try {
            arp = getResourcePermissions(subject, platformID);
        } catch (FinderException e) {
            throw new SystemException("Unexpected error reading "
                                         + "permissions: " + e, e);
        }
        if (arp.canCreateChild() && arp.canModify()) {
            // ok, legal operation
        } else {
            // boom, no permissions
            throw new PermissionException("User " + subject.getName() +
                                          " is not permitted to start an " +
                                          "autoinventory scan on platform " +
                                          platformID);
        }
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
     * @ejb:transaction type="REQUIRED"
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
     * Get the AppdefResourcePermissions for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
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
                this.checkViewPermission(who, eid);
                canView = true;
            } catch (PermissionException e) {
            }
            try {
                this.checkModifyPermission(who, eid);
                canModify = true;
            } catch (PermissionException e) {
            }
            try {
                this.checkRemovePermission(who, eid);
                canRemove = true;
            } catch (PermissionException e) {
            }    
            try {
                this.checkControlPermission(who, eid);
                canControl = true;
            } catch (PermissionException e) { 
            }
            try {
                this.checkMonitorPermission(who, eid);
                canMonitor = true;
            } catch (PermissionException e) {
            }
            try {
                this.checkAlertingPermission(who, eid);
                canAlert = true;
            } catch (PermissionException e) {                
            }
            try {
                if(eid.getType() != AppdefEntityConstants.APPDEF_TYPE_SERVICE){
                    this.checkCreateChildPermission(who, eid);                    
                    canCreateChild = true;
                }
            } catch (PermissionException e) {
            } catch (InvalidAppdefTypeException e) {
            }
            // finally create the object
            return new AppdefResourcePermissions(who.getAuthzSubjectValue(),
                                                 eid, canView,
                                                 canCreateChild,
                                                 canModify, canRemove,
                                                 canControl,
                                                 canMonitor, canAlert);
    }
    
    /**
     * Get the root resourceType object. Used to check permissions
     * such as createPlatform which are associated with the root
     * resourceType
     * @return rootResTypeValue - the root resource type
     */
    protected ResourceType getRootResourceType() 
        throws FinderException
    {
        return getResourceType(AuthzConstants.rootResType);
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
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected Operation getOperationByName(ResourceType rtV,
                                                String opName)
        throws PermissionException
    {
        Collection ops = rtV.getOperations();
        for(Iterator it = ops.iterator(); it.hasNext(); ) {
            Operation op = (Operation) it.next();
            if(op.getName().equals(opName)) {
                return op;
            }
        }
        throw new PermissionException("Operation: " + opName +
                                      " not valid for ResourceType: " +
                                      rtV.getName());
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
     * Get the AUTHZ ResourceValue for a Server
     * @return ResourceValue
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ResourceValue getServerResourceValue(Integer pk)
        throws FinderException {
        return getAuthzResource(getServerResourceType(), pk);
    }
 
    /**
     * Get the Authz Resource Type for a Group
     * @return ResourceTypeValue
     */
     public ResourceType getGroupResourceType()
         throws FinderException {
         return getResourceType(AuthzConstants.groupResourceTypeName);
     }

     protected ResourceType getPlatformPrototypeResourceType() 
         throws FinderException
     {
         return getResourceType(AuthzConstants.platformPrototypeTypeName);
     }

     protected ResourceType getServerPrototypeResourceType() 
         throws FinderException
     {
         return getResourceType(AuthzConstants.serverPrototypeTypeName);
     }
     
     protected ResourceType getServicePrototypeResourceType() 
         throws FinderException
     {
         return getResourceType(AuthzConstants.servicePrototypeTypeName);
     }

     protected ResourceType getApplicationPrototypeResourceType() 
         throws FinderException
     {
         return getResourceType(AuthzConstants.appPrototypeTypeName);
     }

     /**
     * Get the AUTHZ ResourceValue for a Platform
     * @return ResourceValue
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ResourceValue getPlatformResourceValue(Integer pk)
        throws FinderException
    {
        return getAuthzResource(getPlatformResourceType(), pk);
    }

    /**
     * Get the AUTHZ ResourceValue for a Service
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ResourceValue getServiceResourceValue(Integer pk)
        throws FinderException
    {
        return getAuthzResource(getServiceResourceType(), pk);
    }

    /**
     * Get the AUTHZ ResourceValue for a Application
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ResourceValue getApplicationResourceValue(Integer  pk)
        throws FinderException
    {
        return getAuthzResource(getApplicationResourceType(), pk);
    }

    /**
     * Get the scope of viewable services for a given user
     * @param whoami - the user
     * @return List of ServicePK's for which subject has AuthzConstants.serviceOpViewService
     */
    protected List getViewableServices(AuthzSubject whoami) 
        throws FinderException, PermissionException
    {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Operation op = 
            getOperationByName(getServiceResourceType(),
                               AuthzConstants.serviceOpViewService);
        List idList = 
            pm.findOperationScopeBySubject(whoami, op.getId(),
                                           PageControl.PAGE_ALL);
        
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            keyList.add(idList.get(i));
        }
        return keyList;
    }

    /* Return a list of appdef entity ids that represent the total set of
       service inventory that the subject is authorized to see. This includes
       all services as well as all clusters */
    protected List getViewableServiceInventory (AuthzSubject whoami)
        throws FinderException, PermissionException
    {
        List idList = getViewableServices(whoami);
        for (int i=0;i<idList.size();i++) {
            Integer pk = (Integer) idList.get(i);
            idList.set(i, AppdefEntityID.newServiceID(pk.intValue()));
        }
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List viewableGroups = 
            pm.findOperationScopeBySubject(whoami,
                                           AuthzConstants.groupOpViewResourceGroup, 
                                           AuthzConstants.groupResourceTypeName,
                                           PageControl.PAGE_ALL);
        for (int i=0;i<viewableGroups.size();i++) {
            Integer gid = (Integer) viewableGroups.get(i);
            viewableGroups.set(i, AppdefEntityID.newGroupID(gid));
        }
        idList.addAll(viewableGroups);
        return idList;
    }

    /**
     * Get the scope of viewable apps for a given user
     * @param whoami     
     * @return list of ApplicationPKs for which the subject has
     * AuthzConstants.applicationOpViewApplication
     */
    protected List getViewableApplications(AuthzSubject whoami)
        throws FinderException, PermissionException
    {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Operation op = 
            getOperationByName(getApplicationResourceType(),
                               AuthzConstants.appOpViewApplication);
        List idList = pm.findOperationScopeBySubject(whoami, op.getId(),
                                                     PageControl.PAGE_ALL);
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            keyList.add(idList.get(i));
        }
        return keyList;
    }

    /**
     * Get the scope of viewable servers for a given user
     * @param whoami - the user
     * @return List of ServerPK's for which subject has 
     * AuthzConstants.serverOpViewServer
     */
    protected List getViewableServers(AuthzSubject whoami) 
        throws FinderException, PermissionException
    {
        if (log.isDebugEnabled()) {
            log.debug("Checking viewable servers for subject: " +
                      whoami.getName());
        }
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Operation op =
            getOperationByName(getServerResourceType(), 
                               AuthzConstants.serverOpViewServer);
        List idList =  pm.findOperationScopeBySubject(whoami, op.getId(),
                                                      PageControl.PAGE_ALL);

        if (log.isDebugEnabled()) {
            log.debug("There are: " + idList.size() + " viewable servers");
        }
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            keyList.add(idList.get(i));
        }
        return keyList;
    } 
    
    /**
     * Filter a list of {@link Server}s by their viewability by the subject 
     */
    protected List filterViewableServers(Collection servers, 
                                         AuthzSubjectValue who) 
    {
        PermissionManager permMgr = PermissionManagerFactory.getInstance();
        List res = new ArrayList();
        ResourceType type;
        Operation op;
        
        try {
            type  = getServerResourceType();
            op = getOperationByName(type, AuthzConstants.serverOpViewServer);
        } catch(Exception e) {
            throw new SystemException("Internal error", e);
        }
        
        Integer typeId = type.getId();
        
        for (Iterator i=servers.iterator(); i.hasNext(); ) {
            Server s = (Server)i.next();

            try {
                permMgr.check(who.getId(), typeId, s.getId(), op.getId());
                res.add(s);
            } catch(PermissionException e) {
                // Ok
            }
        }
        return res;
    }

    /**
     * Get the scope of viewable platforms for a given user
     * @param whoami - the user
     * @return List of PlatformLocals for which subject has 
     * AuthzConstants.platformOpViewPlatform
     */
    protected Collection getViewablePlatforms(AuthzSubject whoami, 
                                              PageControl pc)
        throws FinderException, PermissionException, NamingException
    {
        // first find all, based on the sorting attribute passed in, or
        // with no sorting if the page control is null
        Collection platforms;
        // if page control is null, find all platforms
        if (pc == null) {
            platforms = getPlatformDAO().findAll();
        } else {
            pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
            int attr = pc.getSortattribute();
            switch (attr) {
                case SortAttribute.RESOURCE_NAME:
                    platforms =
                        getPlatformDAO().findAll_orderName(pc.isAscending());
                    break;
                case SortAttribute.CTIME:
                    platforms =
                        getPlatformDAO().findAll_orderCTime(pc.isAscending());
                    break;
                default:
                    throw new FinderException("Invalid sort attribute: "+attr);
            }
        }
        // now get the list of PKs
        List viewable = getViewablePlatformPKs(whoami);
        // and iterate over the ejbList to remove any item not in the
        // viewable list
        for(Iterator i = platforms.iterator(); i.hasNext();) {
            Platform platform = (Platform)i.next();
            if(!viewable.contains(platform.getId())) {
                // remove the item, user cant see it
                i.remove();
            }
        }
        return platforms;
    } 

    protected List getViewablePlatformPKs(AuthzSubject who)
        throws FinderException, PermissionException, NamingException {
        // now get a list of all the viewable items
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Operation op = getOperationByName(getPlatformResourceType(),
                                          AuthzConstants.platformOpViewPlatform);
        return pm.findOperationScopeBySubject(who, op.getId(),
                                              PageControl.PAGE_ALL);
    }

    /**
     * Get the scope of viewable groups for a given user
     * @param whoami - the user
     * @return List of AppdefGroup value objects for which subject has 
     * AuthzConstants.groupOpViewResourceGroup
     */
    protected List getViewableGroups(AuthzSubject whoami) 
        throws FinderException, AppdefGroupNotFoundException,
               PermissionException {
        
        if (log.isDebugEnabled())
            log.debug("Checking viewable groups for subject: " + 
                      whoami.getName());
        
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Operation op =
            getOperationByName(getGroupResourceType(), 
                               AuthzConstants.groupOpViewResourceGroup);
        List idList = pm.findOperationScopeBySubject(whoami, op.getId(),
                                                     PageControl.PAGE_ALL);

        List valueList = new ArrayList(idList.size());
        ResourceGroupManagerLocal rgMan = ResourceGroupManagerEJBImpl.getOne();
        for (int i = 0; i < idList.size(); i++) {
            valueList.add(rgMan.findResourceGroupById(whoami,
                                                      (Integer) idList.get(i)));
        }
        return valueList;
    } 

    protected AppdefResource getResource(AppdefEntityID id)
        throws AppdefEntityNotFoundException
    {
        try {
            switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return getPlatformMgrLocal().findPlatformById(id.getId());
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return getServerMgrLocal().findServerById(id.getId());
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return getServiceMgrLocal().findServiceById(id.getId());
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return findApplicationByPK(id.getId());
            default:
                throw new IllegalArgumentException("The passed entity type " +
                                                   "does not have a base of " +
                                                   "AppdefResourceValue");
            }
        } catch(NamingException e){
            throw new SystemException(e);
        }
    }

    protected void deleteCustomProperties(AppdefEntityID aeid) {
        CPropManagerLocal cpropMan = getCPropMgrLocal();
        cpropMan.deleteValues(aeid.getType(), aeid.getID());
    }

    public void setSessionContext(SessionContext ctx) {
        _ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return _ctx;
    }

    protected void rollback() {
        if(!getSessionContext().getRollbackOnly()) {
            getSessionContext().setRollbackOnly();
        }
    }

    /**
     * Get the overlord. This method should be used by any bizapp session
     * bean which wants to call an authz bound method while bypassing the check.
     */
    protected AuthzSubject getOverlord() {
        return AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();             
    }

    protected InitialContext getInitialContext() {
        try {
            if (_ic == null)
                _ic = new InitialContext();
            return _ic;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Map a ResourceGroup to ServiceCluster, just temporary,
     * should be able to remove when done with the
     * ServiceCluster to ResourceGroup Migration
     * @ejb:interface-method
     */
    public ServiceCluster getServiceCluster(ResourceGroup group) {
        if (group == null) {
            return null;
        }
        ServiceCluster sc = new ServiceCluster();
        sc.setName(group.getName());
        sc.setDescription(group.getDescription());
        sc.setGroup(group);
        
        Collection resources = group.getResources();
    
        Set services = new HashSet(resources.size());
        ServiceDAO dao = getServiceDAO();
        ServiceType st = null;
        for (Iterator i = resources.iterator(); i.hasNext();) {
            Resource resource = (Resource) i.next();
            // this should not be the case
            if (!resource.getResourceType().getId()
                    .equals(AuthzConstants.authzService)) {
                continue;
            }
            Service service = dao.findById(resource.getInstanceId());
            if (st == null) {
                st = service.getServiceType();
            }
            services.add(service);
            service.setResourceGroup(sc.getGroup());
        }
        sc.setServices(services);
        
        if (st == null && group.getResourcePrototype() != null) {
            st = getServiceTypeDAO()
                .findById(group.getResourcePrototype().getInstanceId());
        }
        
        if (st != null) {
            sc.setServiceType(st);
        }
        return sc;
    }
}
