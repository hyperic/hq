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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Server objects in appdef and their
 * relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ServiceManagerImpl implements ServiceManager {

    private static final String AUTO_DISCOVERY_ZOMBIE = "autoDiscoveryZombie";

    private final Log log = LogFactory.getLog(ServiceManagerImpl.class);

    private static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_service";
    private Pager valuePager;
  
    private PermissionManager permissionManager;
    private ResourceManager resourceManager;
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public ServiceManagerImpl(PermissionManager permissionManager,
                              ResourceManager resourceManager,
                              AuthzSubjectManager authzSubjectManager) {
     
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.authzSubjectManager = authzSubjectManager;
    }
    
    private Service toService(Resource resource) {
        //TODO
        return new Service();
    }
    
    private ServiceType toServiceType(ResourceType resourceType) {
        //TODO
        return new ServiceType();
    }
    
    private Resource create(AuthzSubject subject,ResourceType type, Resource server, String name, String desc,
                                            String location) {
      
        // TODO perm check
        //permissionManager.checkPermission(subject, resourceManager
        //  .findResourceTypeByName(AuthzConstants.serverResType), server.getId(),
        //AuthzConstants.serverOpAddService);
     
        Resource s = new Resource();
        s.setName(name);
        s.setProperty("autoInventoryIdentifier",name);
        s.setProperty(AUTO_DISCOVERY_ZOMBIE,false);
        s.setProperty("serviceRT",false);
        s.setProperty("endUserRT",false);
        s.setDescription(desc);
        s.setModifiedBy(subject.getName());
        s.setLocation(location);
        s.persist();
        s.setType(type);
        server.relateTo(s, RelationshipTypes.SERVICE);
        return s;
   }
    
    /**
     * Create a Service which runs on a given server
     * @return The service id.
     */

    public Service createService(AuthzSubject subject, Integer serverId, Integer serviceTypeId,
                                 String name, String desc, String location)
        throws ValidationException, PermissionException, ServerNotFoundException,
        AppdefDuplicateNameException {
        Resource server = Resource.findResource(serverId);
        ResourceType serviceType = ResourceType.findResourceType(serviceTypeId);
        return toService(create(subject, serviceType, server,name, desc, location));
    }

   
    private Collection<Resource> findByServerType(Integer serverTypeId, boolean asc) {
        Set<Resource> services = new HashSet<Resource>();
        //TODO sort
        Collection<ResourceType> relatedServiceTypes = ResourceType.findResourceType(serverTypeId).getResourceTypesFrom(RelationshipTypes.SERVICE_TYPE);
        for(ResourceType serviceType:relatedServiceTypes) {
            services.addAll(serviceType.getResources());
        }
        return services;
    }

    /**
     * Get service IDs by service type.
     * 
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    @Transactional(readOnly = true)
    public Integer[] getServiceIds(AuthzSubject subject, Integer servTypeId)
        throws PermissionException {

        //try {

            Collection<Resource> services = findByServerType(servTypeId, true);
            if (services.size() == 0) {
                return new Integer[0];
            }
            List<Integer> serviceIds = new ArrayList<Integer>(services.size());

            // TODO now get the list of PKs
            //Set<Integer> viewable = new HashSet<Integer>(getViewableServices(subject));
            // and iterate over the List to remove any item not in the
            // viewable list
            int i = 0;
            for (Iterator<Resource> it = services.iterator(); it.hasNext(); i++) {
                Resource service = it.next();
                //if (viewable.contains(service.getId())) {
                    // add the item, user can see it
                    serviceIds.add(service.getId());
                //}
            }

            return (Integer[]) serviceIds.toArray(new Integer[0]);
            //TODO
        //} catch (NotFoundException e) {
            // There are no viewable servers
         //   return new Integer[0];
       // }
    }

    /**
     * Find Service by Id.
     */
    @Transactional(readOnly = true)
    public Service findServiceById(Integer id) throws ServiceNotFoundException {
        Service service = getServiceById(id);

        if (service == null) {
            throw new ServiceNotFoundException(id);
        }

        return service;
    }

    /**
     * Get Service by Id.
     * 
     * @return The Service identified by this id, or null if it does not exist.
     */
    @Transactional(readOnly = true)
    public Service getServiceById(Integer id) {
        return toService(Resource.findResource(id));
    }

    /**
     * Get Service by Id and perform permission check.
     * 
     * @return The Service identified by this id.
     */
    @Transactional(readOnly = true)
    public Service getServiceById(AuthzSubject subject, Integer id)
        throws ServiceNotFoundException, PermissionException {

        Service service = findServiceById(id);
        //TODO
        //permissionManager.checkViewPermission(subject, service.getId());
        return service;
    }
    
    /**
     * @param server {@link Server}
     * @param aiid service autoinventory identifier
     * @return {@link List} of {@link Service}
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByAIID(Server server, String aiid) {
      //TODO
        return null;
    }

    /**
     * Find a ServiceType by id
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceType(Integer id) throws ObjectNotFoundException {
        return toServiceType(ResourceType.findResourceType(id));
    }

    /**
     * Find service type by name
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceTypeByName(String name) {
        return toServiceType(ResourceType.findResourceTypeByName(name));
    }

    /**
     * @return PageList of ServiceTypeValues
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getAllServiceTypes(AuthzSubject subject, PageControl pc) {
       
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(getAllServiceTypes(), pc);
    }
    
    private Set<ServiceType> getAllServiceTypes() {
        Set<ServiceType> serviceTypes = new HashSet<ServiceType>();
        for(ResourceType serviceType: getAllServiceResourceTypes()) {
            serviceTypes.add(toServiceType(serviceType));
        }
        return serviceTypes;
    }
    
    private Set<ResourceType> getAllServiceResourceTypes() {
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        Collection<ResourceType> platformTypes = ResourceType.findRootResourceType().getResourceTypesFrom(RelationshipTypes.PLATFORM_TYPE);
        for(ResourceType platformType: platformTypes) {
            Collection<ResourceType> serverTypes = platformType.getResourceTypesFrom(RelationshipTypes.SERVER_TYPE);
            for(ResourceType serverType: serverTypes) {
                resourceTypes.addAll(serverType.getResourceTypesFrom(RelationshipTypes.SERVICE_TYPE));
            }
        }
        for(ResourceType platformType: platformTypes) {
            resourceTypes.addAll(platformType.getResourceTypesFrom(RelationshipTypes.SERVICE_TYPE));
        }
        return resourceTypes;
    }
    
    private Collection<ServiceType> getServiceTypes(List<Integer> authzPks,boolean asc) {
        //TODO from ServiceDAO
        return null;
    }
    
    private Collection<ServiceType> findByServerTypeOrderName(Integer serverTypeId, boolean asc) {
        //TODO from ServiceDAO
        return null; 
    }

    /**
     * @return List of ServiceTypeValues
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getViewableServiceTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // build the server types from the visible list of servers
        //TODO
        //final List<Integer> authzPks = getViewableServices(subject);
        final Collection<ServiceType> serviceTypes = getServiceTypes(new ArrayList<Integer>(), true);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getServiceTypesByServerType(AuthzSubject subject,
                                                                  int serverTypeId) {
        Collection<ServiceType> serviceTypes = findByServerTypeOrderName(
            serverTypeId, true);
        if (serviceTypes.size() == 0) {
            return new PageList<ServiceTypeValue>();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    private PageList<ServiceValue> filterAndPage(Collection<Service> svcCol, AuthzSubject subject, Integer svcTypeId,
                                   PageControl pc) throws ServiceNotFoundException,
        PermissionException {
        List<Service> services = new ArrayList<Service>();
        // iterate over the services and only include those whose pk is
        // present in the viewablePKs list
        if (svcTypeId != null) {
            for (Service o : svcCol) {
                
                 Integer thisSvcTypeId = ((Service) o).getServiceType().getId();
                 
                //TODO groups?
                //else {
                    //ResourceGroup cluster = (ResourceGroup) o;
                    //thisSvcTypeId = cluster.getResourcePrototype().getInstanceId();
                //}
                // first, if they specified a server type, then filter on it
                if (!(thisSvcTypeId.equals(svcTypeId))) {
                    continue;
                }

                services.add(o);
                //services.add(o instanceof Service ? o : getServiceCluster((ResourceGroup) o));
            }
        } else {
            services.addAll(svcCol);
        }

        List<Service> toBePaged = filterUnviewable(subject, services);
        return valuePager.seek(toBePaged, pc);
    }


    private List<Service> filterUnviewable(AuthzSubject subject, Collection<Service> services)
        throws PermissionException, ServiceNotFoundException {
        //TODO
        //        List<AppdefEntityID> viewableEntityIds;
//        try {
//            viewableEntityIds = getViewableServiceInventory(subject);
//        } catch (NotFoundException e) {
//            throw new ServiceNotFoundException("no viewable services for " + subject);
//        }

        List<Service> retVal = new ArrayList<Service>();
        // if a cluster has some members that aren't viewable then
        // the user can't get at them but we don't worry about it here
        // when the cluster members are accessed, the group subsystem
        // will filter them
        // so here's the case for the ServiceLocal amongst the
        // List of services
        // *****************
        // Note: yes, that's the case with regard to group members,
        // but not groups themselves. Clusters still need to be weeded
        // out here. - desmond
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof Service) {
                Service aService = (Service) o;
                //if (viewableEntityIds != null && viewableEntityIds.contains(aService.getEntityId())) {
                    retVal.add(aService);
                //}
//            } else if (o instanceof ResourceGroup) {
//                ResourceGroup aCluster = (ResourceGroup) o;
//                AppdefEntityID clusterId = AppdefEntityID.newGroupID(aCluster.getId());
//                if (viewableEntityIds != null && viewableEntityIds.contains(clusterId)) {
//                    retVal.add(getServiceCluster(aCluster));
//                }
            }
        }
        return retVal;
    }

    /**
     * Get services by server and type.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId, PageControl pc)
        throws ServiceNotFoundException, ServerNotFoundException, PermissionException {
        return getServicesByServer(subject, serverId, null, pc);
    }

    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId,
                                                      Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        List<Service> toBePaged = getServicesByServerImpl(subject, serverId, svcTypeId, pc);
        return valuePager.seek(toBePaged, pc);
    }
    
    private List<Service> findByServerAndTypeOrderName(Integer serverId, Integer svcTypeId) {
        //TODO
        return null;
    }
    
    private List<Service> findByServerOrderName(Integer serverId) {
        //TODO
        return null;
    }
    
    private List<Service> findByServerOrderType(Integer serverId) {
        //TODO
        return null;
    }

    private List<Service> getServicesByServerImpl(AuthzSubject subject, Integer serverId, Integer svcTypeId,
                                         PageControl pc) throws PermissionException,
        ServiceNotFoundException {
      

        List<Service> services;

        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_TYPE:
                services = findByServerOrderType(serverId);
                break;
            case SortAttribute.SERVICE_NAME:
            default:
                if (svcTypeId != null) {
                    services = findByServerAndTypeOrderName(serverId, svcTypeId);
                } else {
                    services = findByServerOrderName(serverId);
                }
                break;
        }
        // Reverse the list if descending
        if (pc != null && pc.isDescending()) {
            Collections.reverse(services);
        }

        return filterUnviewable(subject, services);
    }

    /**
     * Get service POJOs by server and type.
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByServer(AuthzSubject subject, Server server)
        throws PermissionException, ServiceNotFoundException {
        return filterUnviewable(subject, server.getServices());
    }

    @Transactional(readOnly = true)
    public Integer[] getServiceIdsByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException {
        

        List<Service> services;

        if (svcTypeId == null) {
            services = findByServerOrderType(serverId);
        } else {
            services = findByServerAndTypeOrderName(serverId, svcTypeId);
        }

        // Filter the unviewables
        List<Service> viewables = filterUnviewable(subject, services);

        Integer[] ids = new Integer[viewables.size()];
        Iterator<Service> it = viewables.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Service local = it.next();
            ids[i] = local.getId();
        }

        return ids;
    }
    
    /**
     * Get platform services (children of virtual servers)
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, PageControl pc)
        throws PlatformNotFoundException, PermissionException, ServiceNotFoundException {
        return getPlatformServices(subject, platId, null, pc);
    }
    
    private List<Service> findPlatformServicesOrderName(Integer platId, boolean asc) {
        //TODO
        return null;
    }

    /**
     * Get platform services (children of virtual servers) of a specified type
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, Integer typeId,
                                        PageControl pc) throws PlatformNotFoundException,
        PermissionException, ServiceNotFoundException {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);
        Collection<Service> allServices = findPlatformServicesOrderName(platId, pc
            .isAscending());
        return filterAndPage(allServices, subject, typeId, pc);
    }

    /**
     * Get platform service POJOs
     */
    @Transactional(readOnly = true)
    public Collection<Service> getPlatformServices(AuthzSubject subject, Integer platId)
        throws ServiceNotFoundException, PermissionException {
        Collection<Service> services = findPlatformServicesOrderName(platId, true);
        //TODO this used to return services and groups in same Collection!  Yikes!
        return filterUnviewable(subject, services);
    }


    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        //TODO used to return Services and ServiceClusters
        List<Service> services = getServicesByApplication(appId,  pc);
        return valuePager.seek(services, pc);
    }

    /**
     * @return A List of Service and ServiceCluster objects representing all of
     *         the services that the given subject is allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByApplication(AuthzSubject subject, Integer appId)
        throws PermissionException, ApplicationNotFoundException, ServiceNotFoundException {
        //TODO used to return a list w/both services and ServiceClusters.  Now returns just services
        return filterUnviewable(subject, getServicesByApplication(appId, PageControl.PAGE_ALL));
    }
    
    private List<Service> findByApplicationOrderSvcName(Integer appId, boolean asc) {
        //TODO
        return null;
    }
    
    private List<Service> findByApplicationOrderSvcType(Integer appId, boolean asc) {
        //TODO
        return null;
    }

    private List<Service> getServicesByApplication(Integer appId, PageControl pc)
        throws ApplicationNotFoundException {
        try {
            // we only look up the application to validate
            // the appId param
            ResourceGroup.findResourceGroup(appId);
        } catch (ObjectNotFoundException e) {
            //TODO this wouldn't happen
            throw new ApplicationNotFoundException(appId, e);
        }

        List<Service> appServiceCollection;

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_NAME:
            case SortAttribute.RESOURCE_NAME:
                appServiceCollection = findByApplicationOrderSvcName(appId, pc
                    .isAscending());
                break;
            case SortAttribute.SERVICE_TYPE:
                appServiceCollection = findByApplicationOrderSvcType(appId, pc
                    .isAscending());
                break;
            default:
                throw new IllegalArgumentException("Unsupported sort " + "attribute [" +
                                                   pc.getSortattribute() + "] on PageControl : " +
                                                   pc);
        }
      //TODO
//        List services = new ArrayList();
//        for (AppService appService : appServiceCollection) {
//            
//            //if (appService.isIsGroup()) {
//              //  services.add(appService.getResourceGroup());
//            //} else {
//                services.add(appService.getService());
//            //}
//        }
//        return services;
        return appServiceCollection;
    }

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                     PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        //TODO used to return ServiceClusterValue also
        return getServiceInventoryByApplication(subject, appId, null, pc);
    }

   

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                     Integer svcTypeId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        if (svcTypeId == null) {
            List<Service> services = getServicesByApplication( appId, pc);
            return filterAndPage(services, subject, null, pc);
        } else {
            //TODO
            return null;
            //return getUnflattenedServiceInventoryByApplication(subject, appId, svcTypeId, pc);
        }
    }

    public void updateServiceZombieStatus(AuthzSubject subject, Service svc, boolean zombieStatus)
        throws PermissionException {
        //TODO perm checks
        //permissionManager.checkModifyPermission(subject, svc.getEntityId());
        Resource resource = Resource.findResource(svc.getId());
        resource.setModifiedBy(subject.getName());
        resource.setProperty(AUTO_DISCOVERY_ZOMBIE,zombieStatus);
        resource.merge();
    }
    
    private boolean matchesValueObject(ServiceValue existing, Resource service) {
        //TODO from Service
        return true;
    }
    
    private void updateService(ServiceValue existing, Resource service) {
        //TODO from Service
    }

    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, UpdateException, AppdefDuplicateNameException,
        ServiceNotFoundException {
        permissionManager.checkModifyPermission(subject, existing.getEntityId());
        Resource service = Resource.findResource(existing.getId());

        existing.setModifiedBy(subject.getName());
        if (existing.getDescription() != null)
            existing.setDescription(existing.getDescription().trim());
        if (existing.getLocation() != null)
            existing.setLocation(existing.getLocation().trim());
        if (existing.getName() != null)
            existing.setName(existing.getName().trim());

        if (matchesValueObject(existing,service)) {
            log.debug("No changes found between value object and entity");
        } else {
            updateService(existing,service);
        }
        return toService(service);
    }

    public void updateServiceTypes(String plugin, ServiceTypeInfo[] infos)
    throws VetoException, NotFoundException {
    	final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        // First, put all of the infos into a Hash
        HashMap<String, ServiceTypeInfo> infoMap = new HashMap<String, ServiceTypeInfo>();
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
            names.add(infos[i].getName());
        }
        //TODO server type mess

//        List<ServerType> types = serverTypeDAO.findByName(names);
//        //HashMap<String, ServerType> serverTypes = new HashMap<String, ServerType>(types.size());
//        for (ServerType type : types) {
//            serverTypes.put(type.getName(), type);
//        }

        try {
            Collection<ResourceType> curServices = ResourceType.findByPlugin(plugin);
            //TODO this will be more than just ServiceTypes
            for (ResourceType serviceType : curServices) {

                if (log.isDebugEnabled()) {
                    log.debug("Begin updating ServiceTypeLocal: " + serviceType.getName());
                }

                ServiceTypeInfo sinfo = (ServiceTypeInfo) infoMap.remove(serviceType.getName());

                // See if this exists
                if (sinfo == null) {
                    deleteServiceType(toServiceType(serviceType), overlord);
                } else {
                    // Just update it
                    // XXX TODO MOVE THIS INTO THE ENTITY
                    if (!sinfo.getName().equals(serviceType.getName()))
                        serviceType.setName(sinfo.getName());

                    if (!sinfo.getDescription().equals(serviceType.getDescription()))
                        serviceType.setDescription(sinfo.getDescription());
                    //TODO internal?

//                    if (sinfo.getInternal() != serviceType.isIsInternal())
//                        serviceType.setIsInternal(sinfo.getInternal());

                    // Could be null if servertype was deleted/updated by plugin
                    ResourceType svrtype = serviceType.getResourceTypeTo(RelationshipTypes.SERVICE_TYPE);

                    // TODO Check server type
//                    if (!sinfo.getServerName().equals(svrtype.getName())) {
//                        // Lookup the server type
//                        if (null == (svrtype = serverTypes.get(sinfo.getServerName()))) {
//                            svrtype = serverTypeDAO.findByName(sinfo.getServerName());
//                            if (svrtype == null) {
//                                throw new NotFoundException("Unable to find server " +
//                                                            sinfo.getServerName() +
//                                                            " on which service '" +
//                                                            serviceType.getName() + "' relies");
//                            }
//                            serverTypes.put(svrtype.getName(), svrtype);
//                        }
//                        serviceType.setServerType(svrtype);
//                    }
                }
            }

            // Now create the left-overs
            final Set<String> creates = new HashSet<String>();
            for (final ServiceTypeInfo sinfo : infoMap.values()) {
                ResourceType servType=null;
                //TODO
//                if (null == (servType = serverTypes.get(sinfo.getServerName()))) {
//                    servType = serverTypeDAO.findByName(sinfo.getServerName());
//                    serverTypes.put(servType.getName(), servType);
//                }
                if (creates.contains(sinfo.getName())) {
                    continue;
                }
                creates.add(sinfo.getName());
                if (debug) watch.markTimeBegin("create");
                //TODO get the right servType
                createServiceType(sinfo, plugin, servType);
                if (debug) watch.markTimeEnd("create");
            }
        } finally {
            if (debug) log.debug(watch);
        }
    }

    public ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                          ServerType servType) throws NotFoundException {
        return createServiceType(sinfo, plugin, ResourceType.findResourceType(servType.getId()));
    }
    
    private ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                          ResourceType servType) throws NotFoundException {
        ResourceType serviceType = new ResourceType();
        serviceType.setName(sinfo.getName());
        serviceType.setDescription(sinfo.getDescription());
        //TODO set plugin, isInternal?
        serviceType.persist();
        servType.relateTo(serviceType, RelationshipTypes.SERVICE_TYPE);
        return toServiceType(serviceType);
    }

    public void deleteServiceType(ServiceType serviceType, AuthzSubject overlord)
        throws VetoException {
        ResourceType.findResourceType(serviceType.getId()).remove();
    }

    /**
     * A removeService method that takes a ServiceLocal. This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     */

    public void removeService(AuthzSubject subject, Service service) throws PermissionException,
        VetoException {
        //TODO perm check
        //permissionManager.checkRemovePermission(subject, aeid);
        resourceManager.removeResource(subject, Resource.findResource(service.getId()));
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * service type, the second element is the # of services of that type in the
     * inventory.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getServiceTypeCounts() {
        Collection<ResourceType> serviceTypes = getAllServiceResourceTypes();
        List<Object[]> counts = new ArrayList<Object[]>();
        for(ResourceType serviceType: serviceTypes) {
            counts.add(new Object[]{serviceType.getName(),serviceType.getResources().size()});
        }
        return counts;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

}
