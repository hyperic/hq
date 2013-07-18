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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.events.group.GroupDeleteRequestedEvent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Application objects in appdef and
 * their relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ApplicationManagerImpl implements ApplicationManager,
    ApplicationListener<GroupDeleteRequestedEvent> {

    protected final Log log = LogFactory.getLog(ApplicationManagerImpl.class.getName());

    protected static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_app";
    private Pager valuePager;

    private ApplicationTypeDAO applicationTypeDAO;

    private AppServiceDAO appServiceDAO;

    private ApplicationDAO applicationDAO;

    private ServiceManager serviceManager;

    private ResourceManager resourceManager;

    private PermissionManager permissionManager;

    private AuthzSubjectManager authzSubjectManager;

    private ZeventEnqueuer zeventManager;

    @Autowired
    public ApplicationManagerImpl(ApplicationTypeDAO applicationTypeDAO,
                                  AppServiceDAO appServiceDAO, ApplicationDAO applicationDAO,
                                  ServiceManager serviceManager, ResourceManager resourceManager,
                                  PermissionManager permissionManager,
                                  AuthzSubjectManager authzSubjectManager,
                                  ZeventEnqueuer zeventManager) {

        this.applicationTypeDAO = applicationTypeDAO;
        this.appServiceDAO = appServiceDAO;
        this.applicationDAO = applicationDAO;
        this.serviceManager = serviceManager;
        this.resourceManager = resourceManager;
        this.permissionManager = permissionManager;
        this.authzSubjectManager = authzSubjectManager;
        this.zeventManager = zeventManager;
    }

    /**
     * Get all Application types
     * @return list of ApplicationTypeValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<AppdefResourceTypeValue> getAllApplicationTypes(AuthzSubject who) {
        List<ApplicationType> all = applicationTypeDAO.findAll();
        List<AppdefResourceTypeValue> ret = new ArrayList<AppdefResourceTypeValue>(all.size());
        for (ApplicationType type : all) {
            ret.add(type.getAppdefResourceTypeValue());
        }
        return ret;
    }

    /**
     * Get ApplicationType by ID
     * 
     */
    @Transactional(readOnly = true)
    public ApplicationType findApplicationType(Integer id) {
        return applicationTypeDAO.findById(id);
    }

    /**
     * Create a Application of a specified type
     * @param subject - who
     * @param newApp - the new application to create
     * @param services - A collection of ServiceValue objects that will be the
     *        initial set of services for the application. This can be null if
     *        you are creating an empty application.
     * 
     */
    public Application createApplication(AuthzSubject subject, ApplicationValue newApp)
        throws ValidationException, PermissionException, AppdefDuplicateNameException,
        NotFoundException {

        applicationTypeDAO.findById(newApp.getApplicationType().getId());
        if (log.isDebugEnabled()) {
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
            Application application = applicationDAO.create(newApp);
            // AUTHZ CHECK
            createAuthzApplication(subject, application);
            return application;
        } catch (ValidationException e) {
            throw e;
        }
    }

    /**
     * Update the basic properties of an application. Will NOT update service
     * dependencies, etc.
     * 
     * 
     */
    public ApplicationValue updateApplication(AuthzSubject subject, ApplicationValue newValue)
        throws ApplicationNotFoundException, PermissionException, UpdateException,
        AppdefDuplicateNameException {

        Application app = applicationDAO.findById(newValue.getId());
        permissionManager.checkModifyPermission(subject, app.getEntityId());
        newValue.setModifiedBy(subject.getName());
        newValue.setMTime(new Long(System.currentTimeMillis()));
        trimStrings(newValue);
        if (!newValue.getName().equals(app.getName())) {
            // name has changed. check for duplicate and update authz
            // resource table
            try {
                findApplicationByName(subject, newValue.getName());
                // duplicate found, throw a duplicate object exception
                throw new AppdefDuplicateNameException("there is already " + "an app named: '" +
                                                       app.getName() + "'");
            } catch (ApplicationNotFoundException e) {
                // ok
            } catch (PermissionException e) {
                // fall through, will catch this later
            }

            app.getResource().setName(newValue.getName());
        }
        applicationDAO.setApplicationValue(app, newValue);
        return findApplicationById(subject, app.getId()).getApplicationValue();
    }

    /**
     * Remove an application
     * 
     * 
     */
    public void removeApplication(AuthzSubject subject, Integer id)
        throws ApplicationNotFoundException, PermissionException, VetoException {

        Application app = applicationDAO.findById(id);
        permissionManager.checkRemovePermission(subject, app.getEntityId());
        applicationDAO.remove(app);
        removeAuthzResource(subject, app.getEntityId(), app.getResource());
        applicationDAO.getSession().flush();
    }

    /**
     * remove the authz resource entry
     */
    protected void removeAuthzResource(AuthzSubject subject, AppdefEntityID aeid, Resource r)
        throws PermissionException, VetoException {
        if (log.isDebugEnabled()) {
            log.debug("Removing authz resource: " + aeid);
        }
        AuthzSubject s = authzSubjectManager.findSubjectById(subject.getId());
        resourceManager.removeResource(s, r);

        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(subject, aeid);
        zeventManager.enqueueEventAfterCommit(zevent);
    }

    /**
     * Remove an application service.
     * @param caller - Valid spider subject of caller.
     * @param appId - The application identifier.
     * @param appServiceId - The service identifier
     * @throws ApplicationException when unable to perform remove
     * @throws ApplicationNotFoundException - when the app can't be found
     * @throws PermissionException - when caller is not authorized to remove.
     * 
     * 
     */
    public void removeAppService(AuthzSubject caller, Integer appId, Integer appServiceId)
        throws ApplicationException, ApplicationNotFoundException, PermissionException {
        try {
            Application app = applicationDAO.findById(appId);
            permissionManager.checkModifyPermission(caller, app.getEntityId());

            AppService appSvcLoc = appServiceDAO.findById(appServiceId);
            app.removeService(appSvcLoc);
            appServiceDAO.remove(appSvcLoc);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId);
        }
    }

    /**
     * 
     */
    public void handleResourceDelete(Resource resource) {
        applicationDAO.clearResource(resource);
    }

    /**
     * Get the service dependency map for an application
     * 
     * @param subject
     * @param appId
     */
    @Transactional(readOnly = true)
    public DependencyTree getServiceDepsForApp(AuthzSubject subject, Integer pk)
        throws ApplicationNotFoundException, PermissionException {
        try {
            // find the app

            Application app = applicationDAO.findById(pk);
            permissionManager.checkViewPermission(subject, app.getEntityId());
            return applicationDAO.getDependencyTree(app);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(pk);
        }
    }

    /**
     * Get the # of applications within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getApplicationCount() {
        return new Integer(applicationDAO.size());
    }

    /**
     * Set the dependency map for an application
     * 
     * @param depTree
     * @param subject
     */
    public void setServiceDepsForApp(AuthzSubject subject, DependencyTree depTree)
        throws ApplicationNotFoundException, PermissionException {
        Integer pk = depTree.getApplication().getId();
        try {
            // find the app

            Application app = applicationDAO.findById(pk);
            permissionManager.checkModifyPermission(subject, app.getEntityId());
            applicationDAO.setDependencyTree(app, depTree);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(pk);
        }
    }

    /**
     * Find application by name
     * @param subject - who
     * @param name - name of app
     */
    public Application findApplicationByName(AuthzSubject subject, String name)
        throws ApplicationNotFoundException, PermissionException {
        Application app = applicationDAO.findByName(name);
        if (app == null) {
            throw new ApplicationNotFoundException(name);
        }
        permissionManager.checkViewPermission(subject, app.getEntityId());
        return app;
    }

    /**
     * Get application pojo by id.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Application findApplicationById(AuthzSubject subject, Integer id)
        throws ApplicationNotFoundException, PermissionException {
        try {
            Application app = applicationDAO.findById(id);
            permissionManager.checkViewPermission(subject, app.getEntityId());
            return app;
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(id, e);
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Application> findDeletedApplications() {
        return applicationDAO.findDeletedApplications();
    }

    /**
     * Get all applications.
     * 
     * 
     * @param subject The subject trying to list applications.
     * @return A List of ApplicationValue objects representing all of the
     *         applications that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ApplicationValue> getAllApplications(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        Collection<Integer> authzPks = getViewableApplications(subject);
        Collection<Application> apps = null;
        int attr = -1;
        if (pc != null) {
            attr = pc.getSortattribute();
        }

        switch (attr) {
            case SortAttribute.RESOURCE_NAME:
                if (pc != null) {
                    apps = applicationDAO.findAll_orderName(!pc.isDescending());
                }
                break;
            default:
                apps = applicationDAO.findAll();
                break;
        }
        for (Iterator<Application> i = apps.iterator(); i.hasNext();) {
            Integer appPk = i.next().getId();
            if (!authzPks.contains(appPk)) {
                i.remove();
            }
        }
        return valuePager.seek(apps, pc);
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected Operation getOperationByName(ResourceType rtV, String opName)
        throws PermissionException {
        Collection<Operation> ops = rtV.getOperations();
        for (Operation op : ops) {

            if (op.getName().equals(opName)) {
                return op;
            }
        }
        throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " +
                                      rtV.getName());
    }

    /**
     * Get the scope of viewable apps for a given user
     * @param whoami
     * @return list of ApplicationPKs for which the subject has
     *         AuthzConstants.applicationOpViewApplication
     */
    protected Collection<Integer> getViewableApplications(AuthzSubject whoami)
    throws PermissionException, NotFoundException {
        Operation op = getOperationByName(
            resourceManager.findResourceTypeByName(AuthzConstants.applicationResType),
            AuthzConstants.appOpViewApplication);
        return permissionManager.findOperationScopeBySubject(whoami, op.getId());
    }
    
    /**
     * @return {@link List} of {@link Resource}
     *
     */
    @Transactional(readOnly=true)
    public List<Resource> getApplicationResources(AuthzSubject subject, Integer appId) 
        throws ApplicationNotFoundException, PermissionException {
        List<AppServiceValue> services = getApplicationServices(subject, appId);
        List<Resource> rtn = new ArrayList<Resource>(services.size());
        for (AppServiceValue val : services ) {
            if (val == null || val.getService() == null ||
                val.getService().getResource() == null ||
                val.getService().getResource().isInAsyncDeleteState()) {
                continue;
            }
            rtn.add(val.getService().getResource());
        }
        return rtn;
    }

    /**
     * Get all the application services for this application
     * @param subject
     * @param appId
     * @retur list of AppServiceValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<AppServiceValue> getApplicationServices(AuthzSubject subject, Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        // find the application
        Application app;
        try {
            app = applicationDAO.findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId);
        }
        permissionManager.checkViewPermission(subject, app.getEntityId());
        Collection<AppService> appSvcs = app.getAppServices();
        List<AppServiceValue> appSvc = new ArrayList<AppServiceValue>(appSvcs.size());
        for (AppService svc : appSvcs) {

            appSvc.add(svc.getAppServiceValue());
        }
        return appSvc;
    }

    /**
     * Set the application services for this application
     * @param subject
     * @param map key: Integer service ID value: Boolean indicating that the
     *        service is an entry point
     * 
     */
    public void setApplicationServices(AuthzSubject subject, Integer appId,
                                       List<AppdefEntityID> entityIds)
        throws ApplicationNotFoundException, AppdefGroupNotFoundException, PermissionException {
        try {
            Application app = applicationDAO.findById(appId);
            permissionManager.checkModifyPermission(subject, app.getEntityId());
            for (Iterator<AppService> i = app.getAppServices().iterator(); i.hasNext();) {
                AppService appSvc = i.next();
                AppdefEntityID anId = null;
                if (appSvc.isIsGroup()) {
                    ResourceGroup group = appSvc.getResourceGroup();
                    anId = AppdefEntityID.newGroupID(group.getId());
                } else {
                    anId = AppdefEntityID.newServiceID(appSvc.getService().getId());
                }
                if (!entityIds.contains(anId)) {
                    i.remove();
                } else {
                    entityIds.remove(anId);
                }
            }
            // iterate over the list, and create the individual entries

            for (int i = 0; i < entityIds.size(); i++) {
                AppdefEntityID id = (AppdefEntityID) entityIds.get(i);
                if (id.isService()) {
                    appServiceDAO.create(id.getId(), app, false);
                } else if (id.isGroup()) {
                    appServiceDAO.create(id.getId(), app);
                }
            }
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId);
        }
    }

    /*
     * Helper method to look up the applications by resource
     */
    private Collection<Application> getApplicationsByResource(AppdefEntityID resource,
                                                              PageControl pc)
        throws ApplicationNotFoundException {
        Collection<Application> apps;
        Integer id;

        id = resource.getId();
        try {
            switch (resource.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    apps = getApplicationsByPlatform(pc, id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    apps = getApplicationsByServer(pc, id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    apps = getApplicationsByService(pc, id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    throw new IllegalArgumentException("Applications cannot contain "
                                                       + "other applications");
                default:
                    throw new IllegalArgumentException("Unhandled resource type");
            }
        } catch (AppdefEntityNotFoundException e) {
            throw new ApplicationNotFoundException("Cannot find resource " + id);
        }

        return apps;
    }

    /*
     * Helper method to do the looking up by platform.
     */
    private Collection<Application> getApplicationsByPlatform(PageControl pc, Integer id) {

        Collection<Application> apps;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                apps = applicationDAO.findByPlatformId_orderName(id, pc.isAscending());
                break;
            case SortAttribute.OWNER_NAME:
                apps = applicationDAO.findByPlatformId_orderOwner(id, pc.isAscending());
                break;
            default:
                apps = applicationDAO.findByPlatformId_orderName(id, true);
                break;
        }
        return apps;
    }

    /*
     * Helper method to do the looking up by server.
     */
    private Collection<Application> getApplicationsByServer(PageControl pc, Integer id) {

        Collection<Application> apps;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                apps = applicationDAO.findByServerId_orderName(id, pc.isAscending());
                break;
            case SortAttribute.OWNER_NAME:
                apps = applicationDAO.findByServerId_orderOwner(id, pc.isAscending());
                break;
            default:
                apps = applicationDAO.findByServerId_orderName(id, true);
                break;
        }
        return apps;
    }

    /*
     * Helper method to do the looking up by service.
     */
    private Collection<Application> getApplicationsByService(PageControl pc, Integer id)
        throws AppdefEntityNotFoundException {

        // We need to look up the service so that we can see if we need to
        // look up its cluster, too
        Service service = serviceManager.findServiceById(id);

        boolean cluster = service.getResourceGroup() != null;

        Collection<Application> apps;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.OWNER_NAME:
                apps = applicationDAO.findByServiceId_orderOwner(id, pc.isAscending());
                break;
            case SortAttribute.RESOURCE_NAME:
            default:
                // ZZZ need to fix this up
                if (cluster)
                    apps = applicationDAO.findByServiceIdOrClusterId_orderName(id, service
                        .getResourceGroup().getId());
                else
                    apps = applicationDAO.findByServiceId_orderName(id);

                if (pc.isDescending()) {
                    List<Application> appsList = new ArrayList<Application>(apps);
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
    private Collection<Application> getApplicationsByGroup(AuthzSubject subject,
                                                           AppdefEntityID resource, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {

        // We need to look up the service so that we can see if we need to
        // look up its cluster, too
        AppdefGroupValue group = GroupUtil.getGroup(subject, resource);

        // Has to be a compatible group first
        if (!group.isGroupCompat()) {
            return new ArrayList<Application>();
        }

        Collection<Application> apps = applicationDAO.findByServiceIdOrClusterId_orderName(
            new Integer(0), new Integer(group.getClusterId()));

        if (pc.isDescending()) {
            List<Application> appsList = new ArrayList<Application>(apps);
            Collections.reverse(appsList);
            apps = appsList;
        }

        return apps;
    }

    /**
     * Get all applications for a resource.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ApplicationValue> getApplicationsByResource(AuthzSubject subject,
                                                                AppdefEntityID resource,
                                                                PageControl pc)
        throws ApplicationNotFoundException, PermissionException {
        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are
        // allowed to return.
        Collection<Application> apps;
        try {
            if (!resource.isGroup())
                apps = getApplicationsByResource(resource, pc);
            else
                apps = getApplicationsByGroup(subject, resource, pc);
        } catch (AppdefEntityNotFoundException e) {
            throw new ApplicationNotFoundException("Cannot find application by " + resource);
        }

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(apps, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get all application IDs that use the specified resource.
     * 
     * 
     * @param subject The subject trying to get the app list
     * @param resource Server ID.
     * @param pagenum The page number to start listing. First page is zero.
     * @param pagesize The size of the page (the number of items to return).
     * @param sort The sort order.
     * 
     * @return A List of ApplicationValue objects which use the specified
     *         resource.
     */
    @Transactional(readOnly = true)
    public Integer[] getApplicationIDsByResource(AppdefEntityID resource)
        throws ApplicationNotFoundException {
        Collection<Application> apps = getApplicationsByResource(resource, PageControl.PAGE_ALL);

        Integer[] ids = new Integer[apps.size()];
        int ind = 0;
        for (Iterator<Application> i = apps.iterator(); i.hasNext(); ind++) {
            Application app = i.next();
            ids[ind] = app.getId();
        }
        return ids;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public boolean isApplicationMember(AppdefEntityID application, AppdefEntityID service) {
        return applicationDAO.isApplicationService(application.getId().intValue(), service.getId()
            .intValue());
    }

    /**
     * Generate a resource tree based on the root resources and the traversal
     * (one of ResourceTreeGenerator.TRAVERSE_*)
     * 
     * 
     */
    @Transactional(readOnly = true)
    public ResourceTree getResourceTree(AuthzSubject subject, AppdefEntityID[] resources,
                                        int traversal) throws AppdefEntityNotFoundException,
        PermissionException {
        ResourceTreeGenerator generator = Bootstrap.getBean(ResourceTreeGenerator.class);
        generator.setSubject(subject);
        return generator.generate(resources, traversal);
    }

    /**
     * Private method to validate a new ApplicationValue object
     * @param av
     * @throws ValidationException
     */
    private void validateNewApplication(ApplicationValue av) throws ValidationException {
        String msg = null;
        // first check if its new
        if (av.idHasBeenSet()) {
            msg = "This Application is not new. It has id: " + av.getId();
        }
        // else if(someotherthing) ...

        // Now check if there's a msg set and throw accordingly
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Create the authz resource and verify the subject has the
     * createApplication permission.
     */
    private void createAuthzApplication(AuthzSubject subject, Application app)
        throws PermissionException, NotFoundException {
        log.debug("Begin Authz CreateApplication");
        permissionManager.checkPermission(subject, resourceManager
            .findResourceTypeByName(AuthzConstants.rootResType), AuthzConstants.rootResourceId,
            AuthzConstants.appOpCreateApplication);
        log.debug("User has permission to create application. " + "Adding authzresource");

        ResourceType appProto = resourceManager
            .findResourceTypeByName(AuthzConstants.appPrototypeTypeName);
        Resource proto = resourceManager.findResourceByInstanceId(appProto, app
            .getApplicationType().getId());
        Resource resource = resourceManager.createResource(subject, resourceManager
            .findResourceTypeByName(AuthzConstants.applicationResType), proto, app.getId(), app
            .getName(), false, null);

        app.setResource(resource);
    }

    public void onApplicationEvent(GroupDeleteRequestedEvent event) {
        Collection<Application> apps = applicationDAO.findUsingGroup(event.getGroup());

        for (Application app : apps) {

            // Find the app service and remove it
            try {
                AppService svc = appServiceDAO.findByAppAndCluster(app, event.getGroup());
                app.getAppServices().remove(svc);
                appServiceDAO.remove(svc);
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {

        valuePager = Pager.getPager(VALUE_PROCESSOR);

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

    @Transactional(readOnly=true)
    public Application getApplicationById(Integer id) {
        if (id == null) {
            return null;
        }
        return applicationDAO.get(id);
    }
}
