/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEvent;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceLocal;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationLocal;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ApplicationTypeLocal;
import org.hyperic.hq.appdef.shared.ApplicationTypePK;
import org.hyperic.hq.appdef.shared.ApplicationVOHelperUtil;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypeLocal;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerTypeLocal;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerVOHelperUtil;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypePK;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.Service;
import org.hyperic.hq.appdef.Server;
import org.hyperic.hq.appdef.ServiceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.SortAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;

/**
 * Parent abstract class of all appdef session ejbs
 */

public abstract class AppdefSessionEJB 
    extends AppdefSessionUtil
{
    protected SessionContext myCtx;
    protected InitialContext ic;
    
    // cached resource types
    protected ResourceTypeValue platformRTV;
    protected ResourceTypeValue serverRTV;
    protected ResourceTypeValue serviceRTV;
    protected ResourceTypeValue applicationRTV;
    protected ResourceTypeValue rootRTV;
    protected ResourceTypeValue groupRTV;
    protected AuthzSubjectValue overlord;

    protected Log log = LogFactory.getLog(this.getClass().getName());

    /**
     * Get the authz resource type value 
     * @param resType - the constant indicating the resource type
     * (from AuthzConstants)
     */
    protected ResourceTypeValue getResourceType(String resType) 
    	throws FinderException {
            log.debug("Looking up Resource Type: " + resType);
            ResourceTypeValue resTypeValue = getResourceManager()
                .findResourceTypeByName(resType);
            return resTypeValue;
    }

    /**
     * builds a list of resource types from the list of resources
     */
    protected Collection filterResourceTypes(Collection resources) {
        // Create TreeSet which sorts on the name
        TreeSet resTypes = new TreeSet(
            new Comparator() {
                private String getName(Object obj) {
                    if (obj instanceof PlatformTypeLocal)
                        return ((PlatformTypeLocal) obj).getSortName();
                    
                    if (obj instanceof ServerTypeLocal)
                        return ((ServerTypeLocal) obj).getSortName();
                    
                    if (obj instanceof ServiceTypeLocal)
                        return ((ServiceTypeLocal) obj).getSortName();

                    if (obj instanceof ServiceType)
                        return ((ServiceType) obj).getSortName();

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
                AppdefResourceLocal resource = (AppdefResourceLocal)iterator.next();
                EJBLocalObject rTypeLocal = resource.getAppdefResourceType();
                if (!resTypes.contains(rTypeLocal))
                    resTypes.add(rTypeLocal);
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
     * @param resTypeVal - the type
     * @param subject - who
     * @param id - the id of the object
     */
    protected void createAuthzResource(AuthzSubjectValue who, 
                                       ResourceTypeValue resTypeVal, 
                                       Integer id, 
                                       String name) 
        throws CreateException {
        createAuthzResource(who, resTypeVal, id, name, false);
    }
    
    /**
     * Create an authz resource
     * @param resTypeVal - the type
     * @param subject - who
     * @param id - the id of the object
     * @param name - the name of the resource
     * @param fsystem - true if the resource should be non-visible
     */
    protected void createAuthzResource(AuthzSubjectValue who, 
    								   ResourceTypeValue resTypeVal, 
									   Integer id, 
									   String name,
                                       boolean fsystem) 
    	throws CreateException{
        if (log.isDebugEnabled())
            log.debug("Creating Authz Resource Type: " + resTypeVal +
                      " id: " + id + " by: " + who);
        try {
        	getResourceManager().createResource(who, resTypeVal, id,
                                                name, fsystem);
        } catch (FinderException e) {
        	throw new SystemException(e);
        } catch (NamingException e) {
        	throw new SystemException(e);
        }
    }

    /**
     * Update the authz resource. Used to update the name in the authz
     * resource table
     * @param resVal - the resourceVal
     */
    protected void updateAuthzResource(ResourceValue rv)
        throws NamingException, UpdateException {
        try {
            ResourceManagerUtil.getLocalHome().create()
                .saveResource(rv); 
        } catch (CreateException e) {
            throw new UpdateException(e);
        } catch (FinderException e) {
            throw new UpdateException(e);
        }
    }

    /**
     * Retrieve the ResourceValue object for a given Appdef Object
     * @param Object - the Appdef EJB object
     * @return ResourceValue
     */
    protected ResourceValue getAuthzResource(ResourceTypeValue rtV,
    										 Integer id)
        throws FinderException {
            ResourceManagerLocal rm = getResourceManager();
            return rm.findResourceByInstanceId(rtV, id); 
    }

    /**
     * Get the authz resource type by AppdefEntityId
     * @param AppdefEntityId
     * @return ResourceTypeValue
     */
    protected ResourceTypeValue getAuthzResourceType(AppdefEntityID id)
        throws NamingException, FinderException {
            int type = id.getType();
            Integer instanceId = id.getId();
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
     * Get the authz resource by AppdefEntityId
     * @param AppdefEntityId
     * @return ResourceTypeValue
     */
    protected ResourceValue getAuthzResource(AppdefEntityID id)
        throws NamingException, FinderException {
            int type = id.getType();
            Integer instanceId = id.getId();
            switch(type) {
                    case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                        return getAuthzResource(getPlatformResourceType(),
                            instanceId);
                    case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                        return getAuthzResource(getServerResourceType(),
                            instanceId);
                    case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                        return getAuthzResource(getServiceResourceType(),
                            instanceId);
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    return getAuthzResource(getApplicationResourceType(),
                        instanceId);
                default:
                    throw new InvalidAppdefTypeException("Type: " +
                        type + " unknown");
            }
    }

    /**
     * Find a PlatformLocal by primary key
     * @return PlatformLocal
     */
    protected PlatformLocal findPlatformByPK(PlatformPK pk) 
        throws PlatformNotFoundException, NamingException {
        try {            
            return getPlatformLocalHome().findByPrimaryKey(pk);
        } catch (FinderException e) {
            throw new PlatformNotFoundException(pk.getId(), e);
        }
    }

    /**
     * remove the authz resource entry
     */
    protected void removeAuthzResource(AuthzSubjectValue subject,
                                       ResourceValue res)
        throws RemoveException, FinderException {
        log.debug("Removing authz resource: " + res + " by: " + subject);
        try {
            ResourceManagerLocal rm = getResourceManager();
            rm.removeResource(subject, res);
            
            // Who knows what groups this will affect.  Takes too long to figure
            // out which groups, so just tell VOCache to delete them all
            VOCache.getInstance().removeAllGroups();
        } catch (RemoveException e) {
            rollback();
            throw e;
        } catch (NamingException e) {
        	throw new SystemException(e);
        }
    }

    /**
     * Find a PlatformTypeLocal by primary key
     * @return PlatformTypeLocal
     */
    protected PlatformTypeLocal findPlatformTypeByPK(PlatformTypePK pk)
        throws FinderException, NamingException {
            return getPlatformTypeLocalHome().findByPrimaryKey(pk);
    }

    /**
     * Find a ServerLocal by primary key
     * @return ServerLocal
     */
    protected Server findServerByPK(ServerPK pk)
        throws ServerNotFoundException, NamingException {
        try {
            return getServerDAO().findByPrimaryKey(pk);
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(pk.getId(), e);
        }
    }

    
    /**
     * Find a ServerTypeLocal by primary key
     * @return ServerTypeLocal
     */
    protected ServerTypeLocal findServerTypeByPK(ServerTypePK pk)
        throws FinderException, NamingException {
            return getServerTypeLocalHome().findByPrimaryKey(pk);
    }

    /**
     * Find a ServiceLocal by primary key
     * @return ServiceLocal
     */
    protected Service findServiceByPK(ServicePK pk)
        throws ServiceNotFoundException, NamingException {
        try {
            return getServiceDAO().findByPrimaryKey(pk);
        } catch (ObjectNotFoundException e) {
            throw new ServiceNotFoundException(pk.getId(), e);
        }
    }

    /**
     * Find a ServiceTypeLocal by primary key
     * @return ServiceType
     */
    protected ServiceType findServiceTypeByPK(ServiceTypePK pk)
    {
        return getServiceTypeDAO().findByPrimaryKey(pk);
    }

    /**
     * Find a ApplicationTypeLocal by primary key
     * @return ApplicationTypeLocal
     */
    protected ApplicationTypeLocal findApplicationTypeByPK(ApplicationTypePK pk)
        throws FinderException, NamingException {
            return getApplicationTypeLocalHome().findByPrimaryKey(pk);
    }

    /**
     * Find a ApplicationLocal by primary key
     * @return ApplicationLocal
     */
    protected ApplicationLocal findApplicationByPK(ApplicationPK pk)
        throws ApplicationNotFoundException, NamingException {
        try {
            return getApplicationLocalHome().findByPrimaryKey(pk);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(pk.getId(), e);
        }
    }

    /**
     * Find an AppdefGroup by id
     * @return AppdefGroupValue
     * @throw AppdefGroupNotFoundException - when group doesn't exist
     * @throw PermissionException - when subject isn't authz.
     */
    protected AppdefGroupValue findGroupById(AuthzSubjectValue subject,
                                             Integer groupId)
        throws AppdefGroupNotFoundException, PermissionException {
        try {
           return getAppdefGroupManagerLocalHome()
                   .create().findGroup(subject,groupId);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Find the primary key of the platform by the pk of a server
     * @param serverPK
     * @return PlatformPK
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PlatformPK getPlatformPkByServerPk(ServerPK serverPK)
    {
        // TODO refactor this using finder
        // find the Server and get its platform
        Server server = getServerDAO()
            .findByPrimaryKey(serverPK);
        // return the parent server's pk
        return (server.getPlatform().getPrimaryKey());
    }

    /**
     * Find the primary key of the platform by the pk of a service
     * @param servicePK the primary key of the service to find
     * @return The PlatformPK of the service
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PlatformPK getPlatformPkByServicePk(ServicePK servicePK)
        throws NamingException, FinderException
    {
        // TODO refactor this using finder
        // find the installed service
        Service isvc = getServiceDAO().findByPrimaryKey(servicePK);
        // find the Server and get its platform
        Server server = isvc.getServer();
        // return the parent server's pk
        return (server.getPlatform().getPrimaryKey());
    }

    /**
     * Check a permission 
     * @param subject - who
     * @param resourceType - type of resource 
     * @param instance Id - the id of the object
     * @param operation - the name of the operation to perform
     */
    protected void checkPermission(AuthzSubjectValue subject, 
                                   ResourceTypeValue rtV,
                                   Integer id, String operation)
        throws PermissionException {
        
        log.debug("Checking Permission for Operation: "
            + operation + " ResourceType: " + rtV +
            " Instance Id: " + id + " Subject: " + subject);
        PermissionManager permMgr = PermissionManagerFactory.getInstance();
        Integer opId = getOpIdByResourceType(rtV, operation);
        Integer subjId = subject.getId();
        Integer typeId = rtV.getId();
        // note, using the "SLOWER" permission check
        permMgr.check(subjId, typeId, id, opId);
        log.debug("Permission Check Succesful");
    }

    /**
     * Check a permission
     * @param subject - who
     * @param appdefEntityId - what
     * @param operation - name of operation
     */
    protected void checkPermission(AuthzSubjectValue subject, 
                                   AppdefEntityID id,
                                   String operation) 
        throws PermissionException {

        ResourceTypeValue rtv = null;            
        try {
            // get the resource type
            rtv = getAuthzResourceType(id);
        } catch (Exception e) {
            throw new PermissionException(e);
        }
        
        // never wrap permission exception unless absolutely necessary
        Integer instanceId = id.getId();
        // now call the protected method
        this.checkPermission(subject, rtv, instanceId, operation);
    } 
    
    /**
     * Check for createPlatform permission for a resource
     * @param subject
     * @throws PermissionException
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkCreatePlatformPermission(AuthzSubjectValue subject)
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
     * @param subject 
     * @param appdefEntityId - what
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void checkModifyPermission(AuthzSubjectValue subject,
                                      AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
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
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    }

    /**
     * Check for view permission for a given resource
     * @param subject 
     * @param appdefEntityId - what
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkViewPermission(AuthzSubjectValue subject,
                                    AppdefEntityID id)
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
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    }

    /**
     * Check for control permission for a given resource
     * @param subject 
     * @param appdefEntityId - what
     * @param subject - who
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkControlPermission(AuthzSubjectValue subject,
                                       AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
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
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    }
    
    /**
     * Check for control permission for a given resource
     * @param subject 
     * @param appdefEntityId - what
     * @param subject - who
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkRemovePermission(AuthzSubjectValue subject,
                                      AppdefEntityID id)
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
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    }

    /**
     * Check for monitor permission for a given resource
     * @param subject 
     * @param appdefEntityId - what
     * @param subject - who
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkMonitorPermission(AuthzSubjectValue subject,
                                       AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
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
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    } 

    /**
     * Check for manage alerts permission for a given resource
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkAlertingPermission(AuthzSubjectValue subject,
                                        AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
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
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        this.checkPermission(subject, id, opName);
    }
    
    /**
     * Check the scope of alertable resources for a give subject
     * @return a list of AppdefEntityIds
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public List checkAlertingScope(AuthzSubjectValue subj) {
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
                entityIds.add(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, id));                                                                           
            }
            // servers
            List serverIds = 
                    pm.findOperationScopeBySubject(subj,
                                                   AuthzConstants.serverOpManageAlerts,
                                                   AuthzConstants.serverResType,
                                                   PageControl.PAGE_ALL);
            for(int i = 0; i < serverIds.size(); i++) {
                Integer id = (Integer)serverIds.get(i);
                entityIds.add(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, id));                                                                           
            }
            // services
            List serviceIds =
                    pm.findOperationScopeBySubject(subj,
                                                   AuthzConstants.serviceOpManageAlerts,
                                                   AuthzConstants.serviceResType,
                                                   PageControl.PAGE_ALL);
            for(int i = 0; i < serviceIds.size(); i++) {
                Integer id = (Integer)serviceIds.get(i);
                entityIds.add(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, id));
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
    public void checkAIScanPermission (AuthzSubjectValue subject,
                                       AppdefEntityID id)
        throws PermissionException, GroupNotCompatibleException {

        int type = id.getType();

        // Check permissions - subject must have modify platform
        // permission on the platform in question (or, if it's a group, the
        // subject must have modify platform permissions on all platforms
        // in the group), AND the global "add server" permission.
        if (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            checkAIScanPermissionForPlatform(subject, id);

        } else if (type == AppdefEntityConstants.APPDEF_TYPE_GROUP) {

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
                throw new SystemException("Can't perform autoinventory "
                                             + "scan on an empty group");
            }

            ArrayList jobIds = new ArrayList();
            
            for (Iterator i = groupMembers.iterator(); i.hasNext();) {
                AppdefEntityID platformEntityID = (AppdefEntityID) i.next();
                checkAIScanPermissionForPlatform(subject, platformEntityID);
            }
        } else {
            throw new SystemException("Autoinventory scans may only be "
                                         + "performed on platforms and groups "
                                         + "of platforms");
        }
    }

    /**
     * Chec to see if the subject can perform an autoinventory scan
     * on a platform.  Don't use this method - instead use checkAIScanPermission
     * which will call this method as necessary.
     */
    private void checkAIScanPermissionForPlatform(AuthzSubjectValue subject,
                                                  AppdefEntityID platformID)
        throws PermissionException {
        
        AppdefResourcePermissions arp;
        try {
            arp = getResourcePermissions(subject, platformID);
        } catch (NamingException e) {
            throw new SystemException("Unexpected error reading "
                                         + "permissions: " + e, e);
        } catch (FinderException e) {
            throw new SystemException("Unexpected error reading "
                                         + "permissions: " + e, e);
        }
        if (arp.canCreateChild() && arp.canModify()) {
            // ok, legal operation
        } else {
            // boom, no permissions
            throw new PermissionException("User " + subject.getName()
                                          + " is not permitted to start an "
                                          + "autoinventory scan on platform "
                                          + platformID);
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
     * @param appdefEntityId - what
     * @param subject - who
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void checkCreateChildPermission(AuthzSubjectValue subject,
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
        this.checkPermission(subject, id, opName);
    }
    
    /**
     * Get the AppdefResourcePermissions for a given resource
     * @param subject - who
     * @param entityID - what
     * @return AppdefResourcePermissions
     * @throws FinderException
     * @throws NamingException
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */ 
    public AppdefResourcePermissions 
        getResourcePermissions(AuthzSubjectValue who,
                               AppdefEntityID eid)
        throws NamingException, FinderException {
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
            }
            // finally create the object
            return new AppdefResourcePermissions(who, eid, canView, canCreateChild,
                                                 canModify, canRemove, canControl,
                                                 canMonitor, canAlert);
    }
    
    /**
     * Get the root resourceType object. Used to check permissions
     * such as createPlatform which are associated with the root
     * resourceType
     * @return rootResTypeValue - the root resource type
     */
    protected ResourceTypeValue getRootResourceType() 
        throws FinderException
    {
        if(rootRTV != null) {
            return rootRTV;
        }
        rootRTV = this.getResourceType(AuthzConstants.rootResType);
        return rootRTV; 
    }

    /**
     * Find an operation by name inside a ResourceTypeValue object
     * @param rtV - the resource type value object
     * @return operationId 
     * @throws PermissionException - if the op is not found
     */
    private Integer getOpIdByResourceType(ResourceTypeValue rtV, String opName)
        throws PermissionException {
            OperationValue[] ops = rtV.getOperationValues();
            for(int i=0; i < ops.length; i++) {
                if(ops[i].getName().equals(opName)) {
                    return ops[i].getId();
                }
            }
            throw new PermissionException("Operation: " + opName 
                + " not valid for ResourceType: " + rtV.getName());
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     * @param opName
     * @param resTypeVal
     */
    protected OperationValue getOperationByName(ResourceTypeValue rtV,
                                                String opName)
        throws PermissionException
    {
        OperationValue[] ops = rtV.getOperationValues();
        for(int i=0; i < ops.length; i++) {
            if(ops[i].getName().equals(opName)) {
                return ops[i];
            }
        }
        throw new PermissionException("Operation: " + opName
                                      + " not valid for ResourceType: " +
                                      rtV.getName());
    }

    /**
     * Get the platform resource type
     * @return platformResType
     */
    protected ResourceTypeValue getPlatformResourceType() 
    	throws FinderException {
        log.debug("Getting Platform Resource Type");
        if(platformRTV != null) {
            log.debug("Returning Cached Instance");
            return platformRTV;
        }
        platformRTV = this.getResourceType(AuthzConstants.platformResType);
        return platformRTV;
    }

    /**
     * Get the application resource type
     * @return applicationResType
     */
    protected ResourceTypeValue getApplicationResourceType() 
    	throws FinderException {
        log.debug("Getting Application Resource Type");
        if(applicationRTV != null) {
            log.debug("Returning Cached Instance");
            return applicationRTV;
        }
        applicationRTV = this.getResourceType(AuthzConstants.applicationResType);
        return applicationRTV;
    }

    /**
     * Get the Server Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceTypeValue getServerResourceType() 
    	throws FinderException {
        log.debug("Getting Server Resource Type");
        if(serverRTV != null) {
            return serverRTV;
        }
        serverRTV = this.getResourceType(AuthzConstants.serverResType);
        return serverRTV;
    }

    /**
     * Get the Service Resource Type
     * @return ResourceTypeValye
     */
    protected ResourceTypeValue getServiceResourceType() 
    	throws FinderException {
        log.debug("Getting Service Resource Type");
        if(serviceRTV != null) {
            return serviceRTV;
        }
        serviceRTV = this.getResourceType(AuthzConstants.serviceResType);
        return serviceRTV;
    }

    /**
     * Get the AUTHZ ResourceValue for a Server
     * @param ejb - the Server EJB
     * @return ResourceValue
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue getServerResourceValue(ServerPK pk)
        throws NamingException, FinderException, CreateException {
        return this.getAuthzResource(getServerResourceType(),
                                     pk.getId());
    }
 
    /**
     * Get the Authz Resource Type for a Group
     * @return ResourceTypeValue
     */
     public ResourceTypeValue getGroupResourceType()
         throws NamingException, FinderException {
         if (groupRTV != null) {
             return groupRTV;
         }
         groupRTV = this.getResourceType(AuthzConstants.groupResourceTypeName);
         return groupRTV;
     }

    /**
     * Get the AUTHZ ResourceValue for a Platform
     * @param ejb - the Platform EJB
     * @return ResourceValue
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue getPlatformResourceValue(PlatformPK pk)
        throws NamingException, FinderException, CreateException
    {
        return this.getAuthzResource(getPlatformResourceType(),
                                     pk.getId());
    }

    /**
     * Get the AUTHZ ResourceValue for a Service
     * @param ctx
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue getServiceResourceValue(ServicePK pk)
        throws NamingException, FinderException, CreateException
    {
        return this.getAuthzResource(getServiceResourceType(),
                                     pk.getId());
    }

    /**
     * Get the AUTHZ ResourceValue for a Application
     * @param ctx
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue getApplicationResourceValue(ApplicationPK pk)
        throws NamingException, FinderException, CreateException
    {
        return this.getAuthzResource(getApplicationResourceType(),
                                     pk.getId());
    }

    /**
     * Get the scope of viewable services for a given user
     * @param whoami - the user
     * @return List of ServicePK's for which subject has AuthzConstants.serviceOpViewService
     */
    protected List getViewableServices(AuthzSubjectValue whoami) 
        throws FinderException, NamingException, PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        OperationValue opVal = 
            getOperationByName(getServiceResourceType(),
                               AuthzConstants.serviceOpViewService);
        List idList = 
            pm.findOperationScopeBySubject(whoami, opVal.getId(),
                                           PageControl.PAGE_ALL);
        
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            ServicePK aPK = new ServicePK();
            aPK.setId((Integer)idList.get(i));
            keyList.add(aPK);
        }
        return keyList;
    }

    /* Return a list of appdef entity ids that represent the total set of
       service inventory that the subject is authorized to see. This includes
       all services as well as all clusters */
    protected List getViewableServiceInventory (AuthzSubjectValue whoami)
        throws FinderException, NamingException, PermissionException {
        List idList = getViewableServices(whoami);
        for (int i=0;i<idList.size();i++) {
            ServicePK pk = (ServicePK) idList.get(i);
            idList.set(i,new AppdefEntityID(AppdefEntityConstants
                .APPDEF_TYPE_SERVICE,pk.getId() ));
        }
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List viewableGroups = 
            pm.findOperationScopeBySubject(whoami,
                                           AuthzConstants.groupOpViewResourceGroup, 
                                           AuthzConstants.groupResourceTypeName,
                                           PageControl.PAGE_ALL);
        for (int i=0;i<viewableGroups.size();i++) {
            Integer gid = (Integer) viewableGroups.get(i);
            viewableGroups.set(i, new AppdefEntityID(AppdefEntityConstants
                .APPDEF_TYPE_GROUP, gid));
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
    protected List getViewableApplications(AuthzSubjectValue whoami)
        throws FinderException, NamingException, PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        OperationValue opVal = 
            getOperationByName(getApplicationResourceType(),
                               AuthzConstants.appOpViewApplication);
        List idList = 
            pm.findOperationScopeBySubject(whoami, opVal.getId(),
                                           PageControl.PAGE_ALL);
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            ApplicationPK aPK = new ApplicationPK();
            aPK.setId((Integer)idList.get(i));
            keyList.add(aPK);
        }
        return keyList;
    }

    /**
     * Get the scope of viewable servers for a given user
     * @param whoami - the user
     * @return List of ServerPK's for which subject has 
     * AuthzConstants.serverOpViewServer
     */
    protected List getViewableServers(AuthzSubjectValue whoami) 
        throws FinderException, NamingException, PermissionException
    {
        log.debug("Checking viewable servers for subject: "
            + whoami.getName());
        PermissionManager pm = PermissionManagerFactory.getInstance();
        OperationValue opVal =
            getOperationByName(getServerResourceType(), 
                               AuthzConstants.serverOpViewServer);
        List idList = 
            pm.findOperationScopeBySubject(whoami, opVal.getId(),
                                           PageControl.PAGE_ALL);

        log.debug("There are: " + idList.size() + " viewable servers");
        List keyList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            ServerPK aPK = new ServerPK((Integer)idList.get(i));
            log.debug("Adding ServerPK: " + aPK);
            keyList.add(aPK);
        }
        return keyList;
    } 

    /**
     * Get the scope of viewable platforms for a given user
     * @param whoami - the user
     * @return List of PlatformLocals for which subject has 
     * AuthzConstants.platformOpViewPlatform
     */
    protected Collection getViewablePlatforms(AuthzSubjectValue whoami, 
                                              PageControl pc)
        throws FinderException, NamingException, PermissionException
    {
        // first find all, based on the sorting attribute passed in, or
        // with no sorting if the page control is null
        Collection ejbList = null;
        // if page control is null, find all platforms
        if (pc == null) {
            ejbList = getPlatformLocalHome().findAll();
        } else {
            pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
            int attr = pc.getSortattribute();
            switch (attr) {
                case SortAttribute.RESOURCE_NAME:
                    if(pc.isDescending()) {
                        ejbList = getPlatformLocalHome()
                            .findAll_orderName_desc();
                    } else {
                        ejbList = getPlatformLocalHome()
                            .findAll_orderName_asc();
                    }
                    break;
                default:
                    throw new FinderException("Invalid sort attribute: "
                        + attr);
            }
        }
        // now get the list of PKs
        List viewable = getViewablePlatformPKs(whoami);
        // and iterate over the ejbList to remove any item not in the
        // viewable list
        for(Iterator i = ejbList.iterator(); i.hasNext();) {
            PlatformLocal aEJB = (PlatformLocal)i.next();
            if(!viewable.contains((PlatformPK)aEJB.getPrimaryKey())) {
                // remove the item, user cant see it
                i.remove();
            }
        }
        return ejbList; 
    } 

    protected List getViewablePlatformPKs(AuthzSubjectValue who)
        throws FinderException, PermissionException, NamingException {
        // now get a list of all the viewable items
        PermissionManager pm = PermissionManagerFactory.getInstance();
        OperationValue opVal =
            getOperationByName(getPlatformResourceType(),
                               AuthzConstants.platformOpViewPlatform);
        List idList =
            pm.findOperationScopeBySubject(who, opVal.getId(),
                                           PageControl.PAGE_ALL);

        List pkList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            Integer id = (Integer)idList.get(i);
            pkList.add(new PlatformPK(id));
        }
        return pkList;
    }

    /**
     * Get the scope of viewable groups for a given user
     * @param whoami - the user
     * @return List of AppdefGroup value objects for which subject has 
     * AuthzConstants.groupOpViewResourceGroup
     */
    protected List getViewableGroups(AuthzSubjectValue whoami) 
        throws FinderException, NamingException, 
               AppdefGroupNotFoundException,PermissionException {
        log.debug("Checking viewable groups for subject: " + whoami.getName());
        PermissionManager pm = PermissionManagerFactory.getInstance();
        OperationValue opVal =
            getOperationByName(getGroupResourceType(), 
                               AuthzConstants.groupOpViewResourceGroup);
        List idList =
            pm.findOperationScopeBySubject(whoami, opVal.getId(),
                                           PageControl.PAGE_ALL);

        List valueList = new ArrayList(idList.size());
        for(int i=0; i < idList.size(); i++) {
            AppdefGroupValue agv = findGroupById(whoami,(Integer)idList.get(i));
            valueList.add(agv);
        }
        return valueList;
    } 

    protected AppdefResourceValue getResource(AppdefEntityID id)
        throws AppdefEntityNotFoundException
    {
        Integer intID = id.getId();

        try {
            switch(id.getType()){
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                PlatformLocal plat = 
                    this.findPlatformByPK(new PlatformPK(intID));
                return PlatformVOHelperUtil.getLocalHome().create()
                            .getPlatformValue(plat);
            
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server serv = 
                    this.findServerByPK(new ServerPK(intID));
            
                return ServerVOHelperUtil.getLocalHome().create()
                            .getServerValue(serv);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                Service service = findServiceByPK(new ServicePK(intID));
                    
                return service.getServiceValue();
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                ApplicationLocal app =
                    this.findApplicationByPK(new ApplicationPK(intID));

                return ApplicationVOHelperUtil.getLocalHome().create()
                            .getApplicationValue(app);
            default:
                throw new IllegalArgumentException("The passed entity type " +
                                                   "does not have a base of " +
                                                   "AppdefResourceValue");
            }
        } catch(NamingException e){
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    protected void deleteCustomProperties(int appdefType, int appdefId) {
        CPropManagerLocal cpropMan = getCPropMgrLocal();
        cpropMan.deleteValues(appdefType, appdefId);
    }

    public void setSessionContext(SessionContext ctx) {
        myCtx = ctx;
    }

    public SessionContext getSessionContext() {
        return myCtx;
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
    protected AuthzSubjectValue getOverlord() {
        if (overlord == null) {
            try {
                overlord = AuthzSubjectManagerUtil.getLocalHome().create()
                    .findOverlord();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return overlord;
    }

    protected InitialContext getInitialContext() {
        try {
            if (ic == null) ic = new InitialContext();
            return ic;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Send an Appdef event
     *
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public void sendAppdefEvent(AuthzSubjectValue subject,
                                AppdefEntityID id, int eventType)
        throws NamingException
    {
        AppdefEvent event = new AppdefEvent(subject, id, eventType);
        // Now publish the event
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }
}
