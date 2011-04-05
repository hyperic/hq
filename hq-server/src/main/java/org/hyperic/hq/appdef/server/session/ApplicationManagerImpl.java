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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hyperic.hq.auth.data.AuthzSubjectRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceGroupDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
    
    private ResourceGroupDao resourceGroupDao;
    
    private AuthzSubjectRepository authzSubjectRepository;

    private PermissionManager permissionManager;

    private ZeventEnqueuer zeventManager;
    
    private ServiceFactory serviceFactory;
    
    private ResourceDao resourceDao;
    
    private ResourceTypeDao resourceTypeDao;
    
    public static final String MODIFIED_TIME = "ModifiedTime";

    public static final String CREATION_TIME = "CreationTime";
    
    public static final String ENG_CONTACT = "EngContact";
    
    public static final String OPS_CONTACT = "OpsContact";
    
    public static final String BUSINESS_CONTACT = "BusinessContact";
    
    static final List<ApplicationType> APPLICATION_TYPES = new ArrayList<ApplicationType>(2);
    
    //TODO we are removing application type
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
                                  ResourceGroupManager resourceGroupManager, ServiceFactory serviceFactory,
                                  ResourceGroupDao resourceGroupDao, ResourceDao resourceDao,
                                  ResourceTypeDao resourceTypeDao, AuthzSubjectRepository authzSubjectRepository) {
        this.resourceManager = resourceManager;
        this.permissionManager = permissionManager;
        this.zeventManager = zeventManager;
        this.resourceGroupManager = resourceGroupManager;
        this.serviceFactory = serviceFactory;
        this.resourceGroupDao = resourceGroupDao;
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
        this.authzSubjectRepository= authzSubjectRepository;
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
        return APPLICATION_TYPES.get(id-1);
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
            ResourceGroup application = create(subject,newApp);
            return toApplication(application);
        } catch (ValidationException e) {
            throw e;
        }
    }
    
    private Application toApplication(ResourceGroup resourceGroup) {
        Application application =  new Application();
        application.setId(resourceGroup.getId());
        application.setCreationTime((Long)resourceGroup.getProperty(CREATION_TIME));
        application.setDescription(resourceGroup.getDescription());
        application.setLocation(resourceGroup.getLocation());
        application.setModifiedBy(resourceGroup.getModifiedBy());
        application.setModifiedTime((Long)resourceGroup.getProperty(MODIFIED_TIME));
        application.setName(resourceGroup.getName());
        application.setResource(resourceGroup);
        application.setSortName((String) resourceGroup.getProperty(AppdefResource.SORT_NAME));
        application.setBusinessContact((String)resourceGroup.getProperty(BUSINESS_CONTACT));
        application.setEngContact((String)resourceGroup.getProperty(ENG_CONTACT));
        application.setOpsContact((String)resourceGroup.getProperty(OPS_CONTACT));
        application.setOwnerName(authzSubjectRepository.findOwner(resourceGroup).getName());
        //TODO get rid of ApplicationType.  For now just hard-coding them all to Generic type
        application.setApplicationType(APPLICATION_TYPES.get(0));
        for(Resource member: resourceGroup.getMembers()) {
            application.addAppService(toAppService(member));
        }
        return application;
    }
    
    private ResourceGroup create(AuthzSubject owner, ApplicationValue appV) {
        ResourceGroup app =  new ResourceGroup(appV.getName(),resourceManager.findResourceTypeByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION));
        resourceGroupDao.persist(app);
        authzSubjectRepository.setOwner(owner, app);
        updateApplication(app, appV);
        return app;
    }
    
    private void updateApplication(ResourceGroup app, ApplicationValue appV) {
        app.setProperty(AppdefResource.SORT_NAME,appV.getName().toUpperCase());
        app.setModifiedBy(appV.getModifiedBy());
        app.setLocation(appV.getLocation());
        app.setDescription(appV.getDescription());
        app.setProperty(CREATION_TIME,appV.getCTime());
        app.setProperty(MODIFIED_TIME,System.currentTimeMillis());
        app.setProperty(ENG_CONTACT,appV.getEngContact());
        app.setProperty(OPS_CONTACT,appV.getOpsContact());
        app.setProperty(BUSINESS_CONTACT,appV.getBusinessContact());

        if (appV.getAddedAppServiceValues() != null) {
            for (AppServiceValue o: appV.getAddedAppServiceValues()) {
                app.addMember(resourceManager.findResourceById(o.getId()));
            }
        }
        if (appV.getRemovedAppServiceValues() != null) {
            for( AppServiceValue o : appV.getRemovedAppServiceValues()) {
                app.removeMember(resourceManager.findResourceById(o.getId()));
            }
        }
        resourceGroupDao.merge(app);
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
        //TODO perm check
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
        //TODO perm check
        //permissionManager.checkRemovePermission(subject, app.getEntityId());
        AppdefEntityID appId = AppdefUtil.newAppdefEntityId(app);
        app.remove();
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(subject, appId);
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
        ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
        if(app == null) {
            throw new ApplicationNotFoundException(appId);
        }
        //TODO perm check
        //permissionManager.checkModifyPermission(caller, app.getEntityId());

        Resource appSvcLoc = resourceManager.findResourceById(appServiceId);
        app.removeMember(appSvcLoc);
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
       
        //TODO
        // List nodes = newTree.getNodes();
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
        // find the app
        ResourceGroup group = resourceGroupManager.findResourceGroupById(pk);
        if(group == null) {
            throw new ApplicationNotFoundException(pk);
        }
        Application app = toApplication(group);
        permissionManager.checkViewPermission(subject, app.getEntityId());
        return getDependencyTree(app);
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
        // find the app
        ResourceGroup group = resourceGroupManager.findResourceGroupById(pk);
        if(group == null) {
            throw new ApplicationNotFoundException(pk);
        }
        Application app = toApplication(group);
        permissionManager.checkModifyPermission(subject, app.getEntityId());
        setDependencyTree(app, depTree);
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
        //TODO perm check
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
        ResourceGroup app = resourceGroupManager.findResourceGroupById(id);
        if(app == null) {
            throw new ApplicationNotFoundException(id);
        }
        //permissionManager.checkViewPermission(subject, app.getEntityId());
        return toApplication(app);
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
       AppService appService = new AppService();
       appService.setService(serviceFactory.createService(service));
       appService.setCreationTime((Long)service.getProperty(CREATION_TIME));
       appService.setModifiedTime((Long)service.getProperty(MODIFIED_TIME));
       appService.setId(service.getId());
       return appService;
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
        ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
        if(app == null) {
            throw new ApplicationNotFoundException(appId);
        }
        //TODO perm check
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
        Set<Application> applications = new HashSet<Application>();
        Resource server = resourceManager.findResourceById(id);
        Set<Resource> services = server.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: services) {
           applications.addAll(findByService(service));
        }
        final List<Application> rtn = new ArrayList<Application>(applications);
        if(asc) {
            Collections.sort(rtn, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o1.getSortName().compareTo(o2.getSortName());
                }
            });
        }else {
            Collections.sort(rtn, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o2.getSortName().compareTo(o1.getSortName());
                }
            });
        }
        return rtn;
    }
    
    private Collection<Application> findByServerIdOrderOwner(Integer id, boolean asc) {
        //TODO from ApplicationDAO
        return null;
    }
    
    private Set<Application> findByService(Resource service) {
        ResourceType appType = resourceManager.findResourceTypeByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION);
        Set<Application> applications = new HashSet<Application>();
        for(Resource groupResource: appType.getResources()) {
            ResourceGroup group = (ResourceGroup)groupResource;
            if(group.getMembers().contains(service)) {
                applications.add(toApplication(group));
            }
        }
        return applications;
    }
    
    private Collection<Application> findByServiceIdOrderName(Integer id) {
        Resource service = resourceManager.findResourceById(id);
        final List<Application> rtn = new ArrayList<Application>(findByService(service));
        Collections.sort(rtn, new Comparator<Application>() {
            public int compare(Application o1, Application o2) {
                return o1.getSortName().compareTo(o2.getSortName());
            }
        });
        return rtn;
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
    
    @Transactional(readOnly = true)
    public Collection<Application> getApplicationsByResource(AuthzSubject subject,AppdefEntityID resource) 
        throws ApplicationNotFoundException, PermissionException {
        return getApplications(subject,resource,null);
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
       
        Collection<Application> apps = getApplications(subject, resource, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(apps, pc.getPagenum(), pc.getPagesize());
    }
    
    private Collection<Application> getApplications(AuthzSubject subject,
        AppdefEntityID resource, PageControl pc)  throws ApplicationNotFoundException, PermissionException {
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
        return apps;
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
    
    public Number getApplicationCount() {
      return getAllApplications().size();
    }
    
    private Set<Resource> getAllApplications() {
        return resourceManager.findResourceTypeByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION).
            getResources();
    }
    
    public PageList<Resource> getAllApplicationResources(AuthzSubject subject, PageControl pc) {
        int appGroupTypeId = resourceManager.findResourceTypeByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION).getId();
        PageRequest pageInfo = new PageRequest(pc.getPagenum(),pc.getPagesize(),
            new Sort(pc.getSortorder() == PageControl.SORT_ASC ? Direction.ASC: Direction.DESC,"name"));
        Page<Resource> resources = resourceDao.findByIndexedProperty("type", appGroupTypeId,pageInfo,String.class);
        return new PageList<Resource>(resources.getContent(),(int)resources.getTotalElements());
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
        //TODO move init logic?
        if(resourceTypeDao.findByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION) == null) {
            ResourceType groupType = new ResourceType(AppdefEntityConstants.APPDEF_NAME_APPLICATION);
            resourceTypeDao.persist(groupType);
            setPropertyType(groupType,AppdefResource.SORT_NAME,String.class);
            setPropertyType(groupType,ApplicationManagerImpl.BUSINESS_CONTACT,String.class);
            setPropertyType(groupType,ApplicationManagerImpl.CREATION_TIME,Long.class);
            setPropertyType(groupType,ApplicationManagerImpl.ENG_CONTACT,String.class);
            setPropertyType(groupType,ApplicationManagerImpl.MODIFIED_TIME,Long.class);
            setPropertyType(groupType,ApplicationManagerImpl.OPS_CONTACT,String.class);
        }
       
    }
        
    private void setPropertyType(ResourceType groupType, String propTypeName, Class<?> type) {
        PropertyType propType = new PropertyType(propTypeName,type);
        propType.setDescription(propTypeName);
        propType.setHidden(true);
        groupType.addPropertyType(propType);
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
        return group.getType().getName().equals(AppdefEntityConstants.APPDEF_NAME_APPLICATION);
    }
    
}
