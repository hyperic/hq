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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppServiceLocal;
import org.hyperic.hq.appdef.shared.AppServicePK;
import org.hyperic.hq.appdef.shared.AppServiceUtil;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEvent;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationLocal;
import org.hyperic.hq.appdef.shared.ApplicationLocalHome;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ApplicationTypeLocal;
import org.hyperic.hq.appdef.shared.ApplicationTypePK;
import org.hyperic.hq.appdef.shared.ApplicationTypeUtil;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationUtil;
import org.hyperic.hq.appdef.shared.ApplicationVOHelperUtil;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is responsible for managing Application objects in appdef
 * and their relationships
 * @ejb:bean name="ApplicationManager"
 *      jndi-name="ejb/appdef/ApplicationManager"
 *      local-jndi-name="LocalApplicationManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ApplicationManagerEJBImpl extends AppdefSessionEJB implements SessionBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl");

    protected final String VALUE_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_app";
    private Pager valuePager = null;

    /**
     * Get all Application types
     * @return list of ApplicationTypeValue objects
     * @ejb:interface-method
     */
    public List getAllApplicationTypes(AuthzSubjectValue who) 
        throws FinderException {
        Collection ejbs;
        try {
            ejbs = ApplicationTypeUtil.getLocalHome().findAll();
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        ArrayList list = new ArrayList(ejbs.size());
        for(Iterator i = ejbs.iterator(); i.hasNext();) {
            ApplicationTypeLocal appType = (ApplicationTypeLocal)i.next();
            list.add(appType.getApplicationTypeValue());
        }
        return list;
    }

    /**
     * Get ApplicationType by ID
     * @return ApplicationTypeValue
     * @ejb:interface-method
     */
    public ApplicationTypeValue findApplicationTypeById(Integer id)
        throws FinderException {
        ApplicationTypeLocal appType;
        try {
            appType =
                ApplicationTypeUtil.getLocalHome().findByPrimaryKey(
                    new ApplicationTypePK(id));
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        return appType.getApplicationTypeValue();
    }

    /**
     * Create a Application of a specified type
     * @param subject - who
     * @param newApp - the new application to create
     * @param services - A collection of ServiceValue objects that will be
     * the initial set of services for the application.  This can be
     * null if you are creating an empty application.
     * @return The saved value object which will contain a list of AppServiceValue
     * objects inside it.
     * @exception CreateException - if it fails to add the application
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ApplicationPK createApplication(AuthzSubjectValue subject, 
                                              ApplicationValue newApp,
                                              Collection services)
        throws CreateException, ValidationException, PermissionException,
               AppdefDuplicateNameException
    {
        if(log.isDebugEnabled()) {
                log.debug("Begin createApplication: " + newApp);
        }
        
        // check if the object already exists
        try {
            findApplicationByName(subject, newApp.getName());
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateNameException();
        } catch (ApplicationNotFoundException e) {
            // ok
        } catch (PermissionException e) {
            // fall through, will catch this later
        }

        try {
            validateNewApplication(newApp);
            trimStrings(newApp);
            // set creator
            newApp.setOwner(subject.getName());
            // set modified by
            newApp.setModifiedBy(subject.getName());
            // call the create
            ApplicationLocal application = getApplicationLocalHome().create(newApp); 
            // AUTHZ CHECK
            createAuthzApplication(application, subject);
            // now add the services
            for(Iterator i = services.iterator(); i.hasNext();) {
                ServiceValue aService = (ServiceValue)i.next();
                log.debug("Adding service: " + aService + " to application");
                application.addService(aService.getPrimaryKey());
            }
            return (ApplicationPK)application.getPrimaryKey(); 
        } catch (NamingException e) {
            rollback();
            log.error("Unable to get LocalHome", e);
            throw new SystemException("Unable to get LocalHome " + e.getMessage());
        } catch (FinderException e) {
            rollback();
            log.error("Unable to find dependent object", e);
            throw new CreateException("Unable to find dependent object: " +
                                      e.getMessage());
        } catch (CreateException e) {
            rollback();
            throw e;
        } catch (ValidationException e) {
            rollback();
            throw e;
        }
    }

    /**
     * Update the basic properties of an application. Will NOT update
     * service dependencies, etc.
     * @param who
     * @param newValue
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ApplicationValue updateApplication(AuthzSubjectValue subject,
                                              ApplicationValue newValue)
        throws ApplicationNotFoundException, 
               PermissionException, UpdateException, 
               AppdefDuplicateNameException, FinderException {
        try {
            ApplicationLocal app = ApplicationUtil.getLocalHome().
                findByPrimaryKey(newValue.getPrimaryKey());
            checkModifyPermission(subject, app.getEntityId());
            newValue.setModifiedBy(subject.getName());
            newValue.setMTime(new Long(System.currentTimeMillis()));
            trimStrings(newValue);
            if(!newValue.getName().equals(app.getName())) {
                // name has changed. check for duplicate and update authz 
                // resource table
                try {
                    findApplicationByName(subject, newValue.getName());
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateNameException("there is already "
                                                           + "an app named: '" 
                                                           + app.getName() 
                                                           + "'");
                } catch (ApplicationNotFoundException e) {
                    // ok
                } catch (PermissionException e) {
                    // fall through, will catch this later
                }

                ResourceValue rv = getAuthzResource(getApplicationResourceType(),
                        newValue.getId());
                rv.setName(newValue.getName());
                updateAuthzResource(rv);
            }
            app.setApplicationValue(newValue);
            // flush the cache
            VOCache.getInstance().removeApplication(app.getId());
            return getApplicationById(subject, app.getId());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Remove an application
     * @param who
     * @param appId
     * @ejb:interface-method
     */
    public void removeApplication(AuthzSubjectValue caller, Integer id)
        throws ApplicationNotFoundException,
               PermissionException, RemoveException {
        try {
            ApplicationPK pk = new ApplicationPK(id);
            ApplicationLocal app = 
                ApplicationUtil.getLocalHome().findByPrimaryKey(pk);
            checkRemovePermission(caller, app.getEntityId());
            VOCache.getInstance().removeApplication(id);
            this.removeAuthzResource(caller, 
                getApplicationResourceValue(new ApplicationPK(id)));
            app.remove();

            // Send service deleted event
            sendAppdefEvent(caller, new AppdefEntityID(pk),
                            AppdefEvent.ACTION_DELETE);
        } catch(CreateException e) {
            throw new RemoveException("Unable to remove application:" + 
                e.getMessage());
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(id);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Remove an application service.
     * @param caller        - Valid spider subject of caller.
     * @param appId         - The application identifier.
     * @param appServiceId  - The service identifier
     * @throws ApplicationException when unable to perform remove
     * @throws ApplicationNotFoundException - when the app can't be found
     * @throws PermissionException - when caller is not authorized to remove.
     *
     * @ejb:interface-method
     */
    public void removeAppService(AuthzSubjectValue caller, Integer appId, 
        Integer appServiceId)
        throws ApplicationException, ApplicationNotFoundException, 
               PermissionException {
        try {
            ApplicationLocal app = 
                ApplicationUtil.getLocalHome().findByPrimaryKey(
                    new ApplicationPK(appId));
            checkModifyPermission(caller, app.getEntityId());

            AppServicePK pk = new AppServicePK (appServiceId);
            AppServiceLocal appSvcLoc = AppServiceUtil
                .getLocalHome().findByPrimaryKey(pk);                
            appSvcLoc.remove();
            // flush cache
            VOCache.getInstance().removeApplication(appId);
        } catch(RemoveException e) {
            throw new ApplicationException ("Unable to remove application "+
                                               "service:" + appServiceId,e);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(appId);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Change Application owner
     * @param who
     * @param appID
     * @param newOwner
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void changeApplicationOwner(AuthzSubjectValue who,
                                                   Integer appId,
                                                   AuthzSubjectValue newOwner)
        throws ApplicationNotFoundException,
               PermissionException, CreateException {
        try {
            ApplicationPK aPK = new ApplicationPK(appId);
            // first lookup the service
            ApplicationLocal appEJB = getApplicationLocalHome()
                .findByPrimaryKey(aPK);
            // check if the caller can modify this service
            checkModifyPermission(who, appEJB.getEntityId());
            // now get its authz resource
            ResourceValue authzRes = getApplicationResourceValue(aPK);
            // change the authz owner
            getResourceManager().setResourceOwner(who, authzRes, newOwner);
            // update the owner field in the appdef table -- YUCK
            appEJB.setOwner(newOwner.getName());
            appEJB.setModifiedBy(who.getName());
            // flush cache
            VOCache.getInstance().removeApplication(appId);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(appId);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get the service dependency map for an application
     * @ejb:interface-method
     * @param subject
     * @param appId
     */
    public DependencyTree getServiceDepsForApp(AuthzSubjectValue subject,
                                               Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        return getServiceDepsForApp(subject, new ApplicationPK(appId));
    }

    /**
     * Get the service dependency map for an application
     * @ejb:interface-method
     * @param subject
     * @param applicationPK
     */
    public DependencyTree getServiceDepsForApp(AuthzSubjectValue subject, 
                                               ApplicationPK pk)
        throws ApplicationNotFoundException,
               PermissionException {
        try {
            // find the app
            ApplicationLocal app = getApplicationLocalHome()
                .findByPrimaryKey(pk);
            checkViewPermission(subject, app.getEntityId());
            return app.getDependencyTree();
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(pk.getId());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Set the dependency map for an application
     * @ejb:interface-method
     * @param depTree
     * @param subject
     */
    public void setServiceDepsForApp(AuthzSubjectValue subject, 
                                     DependencyTree depTree) 
        throws ApplicationNotFoundException, RemoveException, 
               PermissionException, CreateException {
        ApplicationPK pk = depTree.getApplication().getPrimaryKey();
        try {
            // find the app
            ApplicationLocal app = getApplicationLocalHome()
                .findByPrimaryKey(pk);
            checkModifyPermission(subject, app.getEntityId());
            app.setDependencyTree(depTree);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(pk.getId());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Find application by name
     * @param subject - who
     * @param name - name of app
     * @ejb:interface-method
     */
    public ApplicationValue findApplicationByName(
        AuthzSubjectValue subject, String name)
        throws ApplicationNotFoundException,
               PermissionException {
        try {
            ApplicationLocal app = getApplicationLocalHome().findByName(name);
            checkViewPermission(subject, app.getEntityId());
            return ApplicationVOHelperUtil.getLocalHome().create()
                    .getApplicationValue(app);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(name, e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    /** 
     * Get application by id.
     * @ejb:interface-method
     * @param Integer id
     */
    public ApplicationValue getApplicationById(AuthzSubjectValue subject, 
                                               Integer id) 
        throws ApplicationNotFoundException,
               PermissionException {
        try {
            ApplicationLocal app = ApplicationUtil.getLocalHome()
                .findByPrimaryKey(new ApplicationPK(id));
            checkViewPermission(subject, app.getEntityId());
            return ApplicationVOHelperUtil.getLocalHome().create()
                    .getApplicationValue(app);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(id, e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get all applications.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list applications.
     * @return A List of ApplicationValue objects representing all of the
     * applications that the given subject is allowed to view.
     */
    public PageList getAllApplications ( AuthzSubjectValue subject,
                                         PageControl pc ) 
        throws FinderException, PermissionException {
        try {
            Collection authzPks = getViewableApplications(subject);
            Collection apps = null;
            int attr = -1;
            if(pc != null) {
                attr = pc.getSortattribute();
            }
            switch(attr) {
                case SortAttribute.RESOURCE_NAME:
                    if(pc != null && pc.isDescending()) {
                        apps = ApplicationUtil.getLocalHome().findAll_orderName_desc();
                    } else {
                        apps = ApplicationUtil.getLocalHome().findAll_orderName_asc();
                    } 
                    break;
                default:
                    apps = ApplicationUtil.getLocalHome().findAll();
                    break;
            }
            for(Iterator i = apps.iterator(); i.hasNext();) {
                ApplicationPK appPk = 
                    (ApplicationPK) ((ApplicationLocal)i.next()).getPrimaryKey();
                if(!authzPks.contains(appPk)) {
                    i.remove();
                }
            }
            return valuePager.seek(apps, pc);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get all the application services for this application
     * @param subject
     * @param appId
     * @retur list of AppServiceValue objects
     * @ejb:interface-method
     */
    public List getApplicationServices(AuthzSubjectValue subject,
                                       Integer appId) 
        throws ApplicationNotFoundException,
               PermissionException {
        // find the application
        ApplicationLocal app;
        try {
            app =
                ApplicationUtil.getLocalHome().findByPrimaryKey(
                    new ApplicationPK(appId));
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(appId);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        checkViewPermission(subject, app.getEntityId());
        Collection ejbs = app.getAppServices();
        List appSvc = new ArrayList(ejbs.size());
        for(Iterator i = ejbs.iterator(); i.hasNext();) {
            AppServiceLocal aEJB = (AppServiceLocal)i.next();
            appSvc.add(aEJB.getAppServiceValue());
        }
        return appSvc;
    }

    /**
     * Set the application services for this application
     * @param subject
     * @param map key: Integer service ID value: Boolean indicating
     * that the service is an entry point
     * @ejb:interface-method
     */
    public void setApplicationServices(AuthzSubjectValue subject,
                                       Integer appId,
                                       List entityIds) 
        throws ApplicationNotFoundException, CreateException,
               AppdefGroupNotFoundException, 
               PermissionException {
        try {
            ApplicationPK appPk = new ApplicationPK(appId);
            ApplicationLocal app = ApplicationUtil.getLocalHome()
                .findByPrimaryKey(appPk);
            checkModifyPermission(subject, app.getEntityId());
            for(Iterator i = app.getAppServices().iterator();i.hasNext();) {
                AppServiceLocal appSvc = (AppServiceLocal)i.next();
                AppdefEntityID anId = null;
                if(appSvc.getIsCluster()) {
                    Integer groupId = appSvc.getServiceCluster().getGroupId();
                    anId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                              groupId.intValue());
                } else {
                    anId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                                              appSvc.getService().getId().intValue());
                }
                if(!entityIds.contains(anId)) {
                    i.remove();
                } else {
                    entityIds.remove(anId);
                }
            }
            // iterate over the list, and create the individual entries
            for(int i=0; i < entityIds.size(); i++) {
                AppdefEntityID id = (AppdefEntityID)entityIds.get(i);
                if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
                    app.addService(new ServicePK(id.getId()));
                }
                else if (id.getType() == 
                    AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                    // look up the group so I can get the cluster id
                    AppdefGroupValue agv = AppdefGroupManagerUtil.getLocalHome()
                        .create().findGroup(subject, id);
                    app.addServiceCluster(
                        new ServiceClusterPK(new Integer(agv.getClusterId())));
                }
            }
            // flush cache
            VOCache.getInstance().removeApplication(appId);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(appId);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /*
     * Helper method to look up the applications by resource
     */
    private Collection getApplicationsByResource(AppdefEntityID resource, 
        PageControl pc) 
        throws ApplicationNotFoundException {
        Collection apps;
        Integer id;

        id = resource.getId();
        try {
            switch (resource.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM :
                    apps = getApplicationsByPlatform(pc,id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                    apps = getApplicationsByServer(pc,id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                    apps = getApplicationsByService(pc,id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
                    throw new IllegalArgumentException(
                        "Applications cannot contain " + "other applications");
                default :
                    throw new IllegalArgumentException("Unhandled resource type");
            }
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(
                "Cannot find application by " + id);
        } catch (AppdefEntityNotFoundException e) {
            throw new ApplicationNotFoundException("Cannot find resource "+ id);
        }

        return apps;
    }

    /*
     * Helper method to do the looking up by platform.
     */
    private Collection getApplicationsByPlatform(PageControl pc, Integer id) 
        throws FinderException {
        ApplicationLocalHome appLocalHome;
        try {
            appLocalHome = getApplicationLocalHome();
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        Collection apps;
        pc = PageControl.initDefaults(pc,SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                if (pc.isAscending()) {
                   apps = appLocalHome.findByPlatformId_orderName_asc(id);
                } else {
                    apps = appLocalHome.findByPlatformId_orderName_desc(id);
                }
                break;
            case SortAttribute.OWNER_NAME:
                if (pc.isAscending()) {
                   apps = appLocalHome.findByPlatformId_orderOwner_asc(id);
                } else {
                    apps = appLocalHome.findByPlatformId_orderOwner_desc(id);
                }
                break;
            default :
                apps = appLocalHome.findByPlatformId_orderName_asc(id);
        }
        return apps;
    }
    
    /*
     * Helper method to do the looking up by server.
     */
    private Collection getApplicationsByServer(PageControl pc, Integer id) 
        throws FinderException {
        ApplicationLocalHome appLocalHome;
        try {
            appLocalHome = getApplicationLocalHome();
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        Collection apps;
        pc = PageControl.initDefaults(pc,SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                if (pc.isAscending()) {
                   apps = appLocalHome.findByServerId_orderName_asc(id);
                } else {
                    apps = appLocalHome.findByServerId_orderName_desc(id);
                }
                break;
            case SortAttribute.OWNER_NAME:
                if (pc.isAscending()) {
                   apps = appLocalHome.findByServerId_orderOwner_asc(id);
                } else {
                    apps = appLocalHome.findByServerId_orderOwner_desc(id);
                }
                break;
            default :
                apps = appLocalHome.findByServerId_orderName_asc(id);
        }
        return apps;
    }
    
    /*
     * Helper method to do the looking up by service.
     */
    private Collection getApplicationsByService(PageControl pc, Integer id) 
        throws FinderException, AppdefEntityNotFoundException {
        ApplicationLocalHome appLocalHome;
        try {
            appLocalHome = getApplicationLocalHome();
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        // We need to look up the service so that we can see if we need to 
        // look up its cluster, too
        ServiceValue service = (ServiceValue) getResource(
            new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, id));
        
        boolean cluster = service.getServiceCluster() != null;
        
        Collection apps;
        pc = PageControl.initDefaults(pc,SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.OWNER_NAME:
                if (pc.isAscending()) {
                    apps = appLocalHome.findByServiceId_orderOwner_asc(id);
                 } else {
                     apps = appLocalHome.findByServiceId_orderOwner_desc(id);
                 }
                break;
            case SortAttribute.RESOURCE_NAME:
            default :
                if (cluster)
                    apps = appLocalHome.findByServiceIdOrClusterId_orderName(
                            id, service.getServiceCluster().getId());
                else
                    apps = appLocalHome.findByServiceId_orderName(id);

                if (pc.isDescending()) {
                    List appsList = new ArrayList(apps);
                    Collections.reverse(appsList);
                    apps = appsList;
                }
                
                break;
        }
        
        return apps;
    }

    /*
     * Helper method to do the looking up by group.
     */
    private Collection getApplicationsByGroup(AuthzSubjectValue subject,
                                              AppdefEntityID resource,
                                              PageControl pc) 
        throws AppdefEntityNotFoundException, PermissionException,
               FinderException {
        ApplicationLocalHome appLocalHome;
        try {
            appLocalHome = getApplicationLocalHome();
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        // We need to look up the service so that we can see if we need to 
        // look up its cluster, too
        AppdefGroupValue group = GroupUtil.getGroup(subject, resource);
        
        // Has to be a compatible group first
        if (!group.isGroupCompat())
            return new ArrayList();
        
        Collection apps =
            appLocalHome.findByServiceIdOrClusterId_orderName(
                    new Integer(0), new Integer(group.getClusterId()));

        if (pc.isDescending()) {
            List appsList = new ArrayList(apps);
            Collections.reverse(appsList);
            apps = appsList;
        }

        return apps;
    }
    
    /**
     * Get all applications for a resource.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list services.
     * @param platformId Server ID.
     * @param pagenum The page number to start listing.  First page is page zero.
     * @param pagesize The size of the page (the number of items to return).
     * @param sort The sort order.  XXX These sort constants are not yet defined,
     * but when they are, they should live somewhere in appdef/shared so that clients
     * can use them too.
     * @return A List of ApplicationValue objects representing all of the
     * services that the given subject is allowed to view.
     * @throws PermissionException
     * @throws FinderException
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    public PageList getApplicationsByResource ( AuthzSubjectValue subject,
                                                AppdefEntityID resource,
                                                PageControl pc) 
        throws ApplicationNotFoundException, PermissionException {
        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are 
        // allowed to return.
        Collection apps;
        try {
            if (resource.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP)
                apps = getApplicationsByResource(resource, pc);
            else
                apps = getApplicationsByGroup(subject, resource, pc);
        } catch (FinderException e) {
            throw new ApplicationNotFoundException(
                "Cannot find application by " + resource);
        } catch (AppdefEntityNotFoundException e) {
            throw new ApplicationNotFoundException(
                "Cannot find application by " + resource);
        }
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(apps, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get all application IDs that use the specified resource.
     * @ejb:interface-method
     *
     * @param subject  The subject trying to get the app list
     * @param resource Server ID.
     * @param pagenum  The page number to start listing.  First page is zero.
     * @param pagesize The size of the page (the number of items to return).
     * @param sort     The sort order.
     *
     * @return A List of ApplicationValue objects which use the specified
     *         resource.
     */
    public Integer[] getApplicationIDsByResource(AppdefEntityID resource)
        throws ApplicationNotFoundException {
        Collection apps = getApplicationsByResource(resource,
                                                    PageControl.PAGE_ALL);

        Integer[] ids = new Integer[apps.size()];
        int ind = 0;
        for (Iterator i = apps.iterator(); i.hasNext(); ind++) {
            ApplicationLocal app = (ApplicationLocal) i.next();
            ids[ind] = app.getId();
        }
        return ids;
    }

    /**
     * Generate a resource tree based on the root resources and
     * the traversal (one of ResourceTreeGenerator.TRAVERSE_*)
     *
     * @ejb:interface-method
     */
    public ResourceTree getResourceTree(AuthzSubjectValue subject,
                                        AppdefEntityID[] resources, 
                                        int traversal)
        throws AppdefEntityNotFoundException, PermissionException
    {
        ResourceTreeGenerator generator;

        generator = new ResourceTreeGenerator(subject);
        return generator.generate(resources, traversal);
    }

    /**
     * Private method to validate a new ApplicationValue object
     * @param av
     * @throws ValidationException
     */
    private void validateNewApplication(ApplicationValue av)
        throws ValidationException {
        String msg = null;
        // first check if its new 
        if(av.idHasBeenSet()) {
            msg = "This Application is not new. It has id: " + av.getId();
        }
        // else if(someotherthing)  ...

        // Now check if there's a msg set and throw accordingly
        if(msg != null) {
            throw new ValidationException(msg);
        }
    }     

    /**
     * Create the authz resource and verify the subject has the createApplication
     * permission. 
     * @param application - the application 
     * @param subject - the user creating
     */
    private void createAuthzApplication(ApplicationLocal app,
                                        AuthzSubjectValue subject)
        throws CreateException, FinderException, PermissionException {
        
            log.debug("Begin Authz CreateApplication");
            checkPermission(subject, getRootResourceType(),
                AuthzConstants.rootResourceId,
                AuthzConstants.appOpCreateApplication);
            log.debug("User has permission to create application. " + 
                "Adding authzresource");
        try {
            ApplicationPK pk = (ApplicationPK)app.getPrimaryKey();
            createAuthzResource(subject, getApplicationResourceType(), pk.getId(),
                                app.getName());
        } catch (CreateException e) {
        	log.error("Unable to create authz resource for application: " 
        			+ app.getName(), e);
            try {
            	app.remove();
            } catch (RemoveException f) {
            	log.error("Unable to rollback application.", f);
            	throw new CreateException(f.getMessage());
            }
            throw e;
        }
    }

    /**
     * Create a service manager session bean.
     * @exception CreateException If an error occurs creating the pager
     * for the bean.
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }
    
    private void trimStrings(ApplicationValue app) {
        if (app.getBusinessContact() != null)
            app.setBusinessContact(app.getBusinessContact().trim());
        if (app.getDescription() != null)
            app.setDescription(app.getDescription().trim());
        if (app.getEngContact() != null)
            app.setEngContact(app.getEngContact().trim());
        if (app.getLocation() != null)
            app.setLocation(app.getLocation().trim());
        if (app.getOpsContact() != null)
            app.setOpsContact(app.getOpsContact().trim());
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
