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
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Application objects in appdef and
 * their relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ApplicationManagerImpl implements ApplicationManager {

    protected final Log log = LogFactory.getLog(ApplicationManagerImpl.class.getName());

    protected static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_app";
    private Pager valuePager;

    private ResourceManager resourceManager;
    
    private ResourceGroupManager resourceGroupManager;

    private PermissionManager permissionManager;

    private ZeventEnqueuer zeventManager;
    
    private static final List<ApplicationType> APPLICATION_TYPES = new ArrayList<ApplicationType>(2);
    
    static {
        ApplicationType generic = new ApplicationType();
        generic.setName("Generic Application");
        generic.setId(1);
        
        
        ApplicationType jEE = new ApplicationType();
        jEE.setName("J2EE Application");
        jEE.setId(2);
      
        
        APPLICATION_TYPES.add(generic);
        APPLICATION_TYPES.add(jEE);
    }

    @Autowired
    public ApplicationManagerImpl(
                                  ResourceManager resourceManager,
                                  PermissionManager permissionManager,
                                  ZeventEnqueuer zeventManager,
                                  ResourceGroupManager resourceGroupManager) {
        this.resourceManager = resourceManager;
        this.permissionManager = permissionManager;
        this.zeventManager = zeventManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    /**
     * Get all Application types
     * @return list of ApplicationTypeValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<AppdefResourceTypeValue> getAllApplicationTypes(AuthzSubject who) {
      
        List<AppdefResourceTypeValue> ret = new ArrayList<AppdefResourceTypeValue>(2);
        for (ApplicationType type : APPLICATION_TYPES) {
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
        //TODO throw Exception if not in list
        return APPLICATION_TYPES.get(id);
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

        findApplicationType(newApp.getApplicationType().getId());
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
        
        //TODO perm check
        //permissionManager.checkPermission(subject, resourceManager
          //  .findResourceTypeByName(AuthzConstants.rootResType), AuthzConstants.rootResourceId,
           // AuthzConstants.appOpCreateApplication);

        try {
            validateNewApplication(newApp);
            trimStrings(newApp);
            // set creator
            newApp.setOwner(subject.getName());
            // set modified by
            newApp.setModifiedBy(subject.getName());
            // call the create
            ResourceGroup application = create(newApp);
            return toApplication(application);
        } catch (ValidationException e) {
            throw e;
        }
    }
    
    private Application toApplication(ResourceGroup resourceGroup) {
        //TODO
        return new Application();
    }
    
    private ResourceGroup create(ApplicationValue appV) {
        ResourceGroup app =  new ResourceGroup();
        app.setName(appV.getName());
        updateApplication(app, appV);
        app.persist();
        app.setType(resourceManager.findResourceTypeById(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP));
        return app;
    }
    
    private void updateApplication(ResourceGroup app, ApplicationValue appV) {
        //a.setSortName(appV.getSortName());
        app.setModifiedBy(appV.getModifiedBy());
        app.setLocation(appV.getLocation());
        //a.setEngContact(appV.getEngContact());
        //a.setOpsContact(appV.getOpsContact());
       // a.setBusinessContact(appV.getBusinessContact());
        app.setDescription(appV.getDescription());
        //app.setCreationTime(appV.getCTime());
       
        
        if (appV.getAddedAppServiceValues() != null) {
            Iterator iAppServiceValue = appV.getAddedAppServiceValues().iterator();
            while (iAppServiceValue.hasNext()) {
                AppServiceValue o = (AppServiceValue) iAppServiceValue.next();
                app.addMember(resourceManager.findResourceById(o.getId()));
            }
        }
        if (appV.getRemovedAppServiceValues() != null) {
            Iterator iAppServiceValue = appV.getRemovedAppServiceValues().iterator();
            while (iAppServiceValue.hasNext()) {
                AppServiceValue o = (AppServiceValue) iAppServiceValue.next();
                app.removeMember(resourceManager.findResourceById(o.getId()));
            }
        }
        if (appV.getApplicationType() != null) {
            app.setProperty("applicationType",appV.getApplicationType().getId());
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

        ResourceGroup app = resourceGroupManager.findResourceGroupById(newValue.getId());
        //TODO
        //permissionManager.checkModifyPermission(subject, app.getEntityId());
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

            app.setName(newValue.getName());
        }
        updateApplication(app, newValue);
        app.merge();
        return findApplicationById(subject, app.getId()).getApplicationValue();
    }

    /**
     * Remove an application
     * 
     * 
     */
    public void removeApplication(AuthzSubject subject, Integer id)
        throws ApplicationNotFoundException, PermissionException, VetoException {

        ResourceGroup app = resourceGroupManager.findResourceGroupById(id);
        //TODO
        //permissionManager.checkRemovePermission(subject, app.getEntityId());
        app.remove();
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(subject, AppdefUtil.newAppdefEntityId(app));
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
            ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
            //TODO perm check
            //permissionManager.checkModifyPermission(caller, app.getEntityId());

            Resource appSvcLoc = resourceManager.findResourceById(appServiceId);
            app.removeMember(appSvcLoc);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId);
        }
    }

  
    private DependencyTree getDependencyTree(Application a) {
        log.debug("Getting Dependency Tree for Application: " + a.getName());
        // construct the tree
        DependencyTree aTree = new DependencyTree(a.getApplicationValue());
        // find all the dependency entries for this app
        Collection<Resource> allDeps = ((ResourceGroup)a.getResource()).getMembers();
        log.debug("Found: " + allDeps.size() + " dependencies");
        // now find all the app services for this app
        //TODO service dependencies?
//        Collection appServices = a.getAppServiceSnapshot();
//        // add them to the top level of the tree
//        for (Iterator i = appServices.iterator(); i.hasNext();) {
//            AppService appSvc = (AppService) i.next();
//            aTree.addNode(appSvc);
//        }
//        for (Iterator i = allDeps.iterator(); i.hasNext();) {
//            AppSvcDependency aDep = (AppSvcDependency) i.next();
//            // get the appservice it refers to
//            AppService appService = aDep.getAppService();
//            AppService depService = aDep.getDependentService();
//       
//            if (log.isDebugEnabled())
//                log.debug("AppService: " + appService + "\n depends on: " + depService);
//            // add the node to the tree. The tree will take care
//            // of appending the dependency if its there already
//            aTree.addNode(appService, depService);
//        }
        return aTree;
    }
    
    private void setDependencyTree(Application a, DependencyTree newTree) {
        log.debug("Setting dependency tree for application: " + a.getName());
        List nodes = newTree.getNodes();
        //TODO
//       
//        for (int i = 0; i < nodes.size(); i++) {
//            DependencyNode aNode = (DependencyNode) nodes.get(i);
//            // first deal with the removed dependencies for this node
//            for (int j = 0; j < aNode.getRemovedChildren().size(); j++) {
//                AppService removedAsv = (AppService) aNode.getRemovedChildren().get(j);
//                // this dep has been removed
//                // look it up and delete it
//                AppSvcDependency dep = appSvcDependencyDAO.findByDependentAndDependor(aNode
//                    .getAppService().getId(), removedAsv.getId());
//                if (dep != null) {
//                    appSvcDependencyDAO.remove(dep);
//                }
//            }
//            // now iterate over the new and existing deps
//        
//            AppService nodeAsv = aNode.getAppService();
//            for (int j = 0; j < aNode.getChildren().size(); j++) {
//                AppService depAsv = (AppService) aNode.getChildren().get(j);
//      
//                // new dependency
//                appServiceDAO.addDependentService(nodeAsv.getId(), depAsv.getId());
//            }
//       
//            // finally set the entry point flag on the AppService
//            boolean isEntryPoint = newTree.isEntryPoint(aNode.getAppService());
//            appServiceDAO.findById(aNode.getAppService().getId()).setEntryPoint(isEntryPoint);
//        }
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
            Application app = toApplication(resourceGroupManager.findResourceGroupById(pk));
            permissionManager.checkViewPermission(subject, app.getEntityId());
            return getDependencyTree(app);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(pk);
        }
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
            Application app = toApplication(resourceGroupManager.findResourceGroupById(pk));
            permissionManager.checkModifyPermission(subject, app.getEntityId());
           setDependencyTree(app, depTree);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(pk);
        }
    }

    /**
     * Find application by name
     * @param subject - who
     * @param name - name of app
     */
    private Application findApplicationByName(AuthzSubject subject, String name)
        throws ApplicationNotFoundException, PermissionException {
        ResourceGroup app = resourceGroupManager.findResourceGroupByName(name);
        if (app == null) {
            throw new ApplicationNotFoundException(name);
        }
        //TODO
        //permissionManager.checkViewPermission(subject, app.getEntityId());
        return toApplication(app);
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
            ResourceGroup app = resourceGroupManager.findResourceGroupById(id);
            //permissionManager.checkViewPermission(subject, app.getEntityId());
            return toApplication(app);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(id, e);
        }
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected OperationType getOperationByName(ResourceType rtV, String opName)
        throws PermissionException {
        Collection<OperationType> ops = rtV.getOperationTypes();
        for (OperationType op : ops) {

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
    protected List<Integer> getViewableApplications(AuthzSubject whoami)
        throws PermissionException, NotFoundException {

        OperationType op = getOperationByName(resourceManager
            .findResourceTypeByName(AuthzConstants.applicationResType),
            AuthzConstants.appOpViewApplication);
        List<Integer> idList = permissionManager.findOperationScopeBySubject(whoami, op.getId());
        List<Integer> keyList = new ArrayList<Integer>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            keyList.add(idList.get(i));
        }
        return keyList;
    }
   
    private AppService toAppService(Resource service) {
        //TODO
        return new AppService();
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
            ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
            //TODO
            //permissionManager.checkModifyPermission(subject, app.getEntityId());
            for (Iterator<Resource> i = app.getMembers().iterator(); i.hasNext();) {
                Resource appSvc = i.next();
                AppdefEntityID anId = AppdefEntityID.newServiceID(appSvc.getId());
                if (!entityIds.contains(anId)) {
                    try {
                        removeAppService(subject, appId, appSvc.getId());
                    } catch (ApplicationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    entityIds.remove(anId);
                }
            }
            // iterate over the list, and create the individual entries

            for (int i = 0; i < entityIds.size(); i++) {
                AppdefEntityID id = (AppdefEntityID) entityIds.get(i);
                app.addMember(resourceManager.findResourceById(id.getId()));
                
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
    
    private Collection<Application> findByPlatformIdOrderName(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Collection<Application> findByPlatformIdOrderOwner(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Collection<Application> findByServerIdOrderName(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Collection<Application> findByServerIdOrderOwner(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Collection<Application> findByServiceIdOrderName(Integer id) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Collection<Application> findByServiceIdOrderOwner(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }

    /*
     * Helper method to do the looking up by platform.
     */
    private Collection<Application> getApplicationsByPlatform(PageControl pc, Integer id) {

        Collection<Application> apps;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                apps = findByPlatformIdOrderName(id, pc.isAscending());
                break;
            case SortAttribute.OWNER_NAME:
                apps = findByPlatformIdOrderOwner(id, pc.isAscending());
                break;
            default:
                apps = findByPlatformIdOrderName(id, true);
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
                apps = findByServerIdOrderName(id, pc.isAscending());
                break;
            case SortAttribute.OWNER_NAME:
                apps = findByServerIdOrderOwner(id, pc.isAscending());
                break;
            default:
                apps = findByServerIdOrderName(id, true);
                break;
        }
        return apps;
    }

    /*
     * Helper method to do the looking up by service.
     */
    private Collection<Application> getApplicationsByService(PageControl pc, Integer id)
        throws AppdefEntityNotFoundException {

        Collection<Application> apps;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.OWNER_NAME:
                apps = findByServiceIdOrderOwner(id, pc.isAscending());
                break;
            case SortAttribute.RESOURCE_NAME:
            default:
                apps = findByServiceIdOrderName(id);

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
        
        
        Collection<Application> apps = new ArrayList<Application>();
        //TODO look up apps by services in the group
        //Collection<Application> apps = applicationDAO.findByServiceIdOrClusterId_orderName(
          //  new Integer(0), new Integer(group.getClusterId()));

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
     * 
     */
    @Transactional(readOnly = true)
    public boolean isApplicationMember(AppdefEntityID application, AppdefEntityID service) {
        ResourceGroup app = resourceGroupManager.findResourceGroupById(application.getId());
        return app.isMember(resourceManager.findResourceById(service.getId()));
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

    public Collection<Application> findUsingGroup(ResourceGroup g) {
        //TODO from ApplicationDAO - find all applications that all services in g belong to
        return null;
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

    public boolean isApplication(ResourceGroup group) {
        return group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
    }
    
}
