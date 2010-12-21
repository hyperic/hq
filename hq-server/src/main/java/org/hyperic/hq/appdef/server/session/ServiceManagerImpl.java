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
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.server.session.PluginDAO;
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

    private final Log log = LogFactory.getLog(ServiceManagerImpl.class);

    private static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_service";
    private Pager valuePager;
  
    private PermissionManager permissionManager;
    private ResourceManager resourceManager;
    private AuthzSubjectManager authzSubjectManager;
    private PluginDAO pluginDAO;
    private ServiceFactory serviceFactory;
    private ResourceGroupManager resourceGroupManager;

    @Autowired
    public ServiceManagerImpl(PermissionManager permissionManager,
                              ResourceManager resourceManager,
                              AuthzSubjectManager authzSubjectManager, PluginDAO pluginDAO,
                              ServiceFactory serviceFactory,
                              ResourceGroupManager resourceGroupManager) {
     
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.authzSubjectManager = authzSubjectManager;
        this.pluginDAO = pluginDAO;
        this.serviceFactory = serviceFactory;
        this.resourceGroupManager = resourceGroupManager;
    }
    
    private Resource create(AuthzSubject subject,ResourceType type, Resource parent, String name, String desc,
                                            String location) {
      
        // TODO perm check
        //permissionManager.checkPermission(subject, resourceManager
        //  .findResourceTypeByName(AuthzConstants.serverResType), server.getId(),
        //AuthzConstants.serverOpAddService);
        Resource s = new Resource();
        s.setName(name);
        s.setDescription(desc);
        s.setModifiedBy(subject.getName());
        s.setLocation(location);
        s.persist();
        s.setType(type);
        s.setProperty(ServiceFactory.AUTO_INVENTORY_IDENTIFIER,name);
        s.setProperty(ServiceFactory.AUTO_DISCOVERY_ZOMBIE,false);
        s.setProperty(ServiceFactory.SERVICE_RT,false);
        s.setProperty(ServiceFactory.END_USER_RT,false);
        //TODO abstract creationTime, modifiedTime, and sortName?
        s.setProperty(ServiceFactory.CREATION_TIME, System.currentTimeMillis());
        s.setProperty(ServiceFactory.MODIFIED_TIME,System.currentTimeMillis());
        s.setProperty(AppdefResource.SORT_NAME, name.toUpperCase());
        s.setOwner(subject);
        parent.relateTo(s, RelationshipTypes.SERVICE);
        return s;
   }
    
    /**
     * Create a Service which runs on a given server
     * @return The service id.
     */

    public Service createService(AuthzSubject subject, Integer parentId, Integer serviceTypeId,
                                 String name, String desc, String location)
        throws ValidationException, PermissionException, ServerNotFoundException,
        AppdefDuplicateNameException {
        Resource parent =resourceManager.findResourceById(parentId);
        ResourceType serviceType = resourceManager.findResourceTypeById(serviceTypeId);
        return serviceFactory.createService(create(subject, serviceType, parent ,name, desc, location));
    }

   
    private Collection<Service> findByServiceType(Integer serviceTypeId, boolean asc) {
        List<Service> services = new ArrayList<Service>();
        Set<Resource> serviceResources = resourceManager.findResourceTypeById(serviceTypeId).getResources();
        for(Resource serviceResource: serviceResources) {
            services.add(serviceFactory.createService(serviceResource));
        }
        Collections.sort(services, new Comparator<Service>() {
            public int compare(Service o1,Service o2) {
                return (o1.getSortName().compareTo(o2.getSortName()));
            }
        });
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

        try {
            Collection<Service> services = findByServiceType(servTypeId, true);
            if (services.size() == 0) {
                return new Integer[0];
            }
            List<Integer> serviceIds = new ArrayList<Integer>(services.size());

            
            Set<Integer> viewable = new HashSet<Integer>(getViewableServices(subject));
            // and iterate over the List to remove any item not in the
            // viewable list
            int i = 0;
            for (Iterator<Service> it = services.iterator(); it.hasNext(); i++) {
                Service service = it.next();
                if (viewable.contains(service.getId())) {
                    // add the item, user can see it
                    serviceIds.add(service.getId());
                }
            }
            return (Integer[]) serviceIds.toArray(new Integer[0]);
        } catch (NotFoundException e) {
             //There are no viewable servers
            return new Integer[0];
        }
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
        Resource serviceResource = resourceManager.findResourceById(id);
        if(serviceResource == null) {
            return null;
        }
        return serviceFactory.createService(serviceResource);
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
        //TODO perm check
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
        List<Service> aiidServices = new ArrayList<Service>();
        Resource serverResource = resourceManager.findResourceById(server.getId());
        Set<Resource> services = serverResource.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: services) {
            if(aiid.equals(service.getProperty(ServiceFactory.AUTO_INVENTORY_IDENTIFIER))) {
                aiidServices.add(serviceFactory.createService(service));
            }
        }
        return aiidServices;
    }

    /**
     * Find a ServiceType by id
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceType(Integer id) throws ObjectNotFoundException {
        return serviceFactory.createServiceType(resourceManager.findResourceTypeById(id));
    }

    /**
     * Find service type by name
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceTypeByName(String name) {
        return serviceFactory.createServiceType(resourceManager.findResourceTypeByName(name));
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
            serviceTypes.add(serviceFactory.createServiceType(serviceType));
        }
        return serviceTypes;
    }
    
    private Set<ResourceType> getAllServiceResourceTypes() {
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        Collection<ResourceType> platformTypes = resourceManager.findRootResourceType().
            getResourceTypesFrom(RelationshipTypes.PLATFORM);
        for(ResourceType platformType: platformTypes) {
            Collection<ResourceType> serverTypes = platformType.getResourceTypesFrom(RelationshipTypes.SERVER);
            for(ResourceType serverType: serverTypes) {
                resourceTypes.addAll(serverType.getResourceTypesFrom(RelationshipTypes.SERVICE));
            }
        }
        for(ResourceType platformType: platformTypes) {
            resourceTypes.addAll(platformType.getResourceTypesFrom(RelationshipTypes.SERVICE));
        }
        return resourceTypes;
    }
    
    private Collection<ServiceType> getServiceTypes(List<Integer> authzPks,boolean asc) {
       Set<ServiceType> serviceTypes = new HashSet<ServiceType>();
       for(Integer serviceId: authzPks) {
           serviceTypes.add(serviceFactory.createServiceType(resourceManager.findResourceById(serviceId).getType()));
       }
       final List<ServiceType> rtn = new ArrayList<ServiceType>(serviceTypes);
       Collections.sort(rtn, new AppdefNameComparator(asc));
       return rtn;

    }
    
    private Collection<ServiceType> findByServerTypeOrderName(Integer serverTypeId, boolean asc) {
        List<ServiceType> serviceTypes = new ArrayList<ServiceType>();
        ResourceType serverType = resourceManager.findResourceTypeById(serverTypeId);
        if(serverType ==  null) {
            return serviceTypes;
        }
        Collection<ResourceType> relatedServiceTypes = serverType.getResourceTypesFrom(RelationshipTypes.SERVICE);
        for(ResourceType serviceType:relatedServiceTypes) {
            serviceTypes.add(serviceFactory.createServiceType(serviceType));
        }
        if(asc) {
            Collections.sort(serviceTypes, new Comparator<ServiceType>() {
                public int compare(ServiceType o1,ServiceType o2) {
                    return (o1.getSortName().compareTo(o2.getSortName()));
                }
            });
        }else {
            Collections.sort(serviceTypes, new Comparator<ServiceType>() {
                public int compare(ServiceType o1,ServiceType o2) {
                    return (o2.getSortName().compareTo(o1.getSortName()));
                }
            });
        }
        return serviceTypes;
    }

    /**
     * @return List of ServiceTypeValues
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getViewableServiceTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // build the server types from the visible list of servers
     
        final List<Integer> authzPks = getViewableServices(subject);
        final Collection<ServiceType> serviceTypes = getServiceTypes(authzPks, true);

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
                Integer thisSvcTypeId = o.getServiceType().getId();
                // first, if they specified a server type, then filter on it
                if (!(thisSvcTypeId.equals(svcTypeId))) {
                    continue;
                }

                services.add(o);
            }
        } else {
            services.addAll(svcCol);
        }

        List<Service> toBePaged = filterUnviewable(subject, services);
        return valuePager.seek(toBePaged, pc);
    }


    private List<Service> filterUnviewable(AuthzSubject subject, Collection<Service> services)
        throws PermissionException, ServiceNotFoundException {
       
        List<Integer> viewableEntityIds;
        try {
            viewableEntityIds = getViewableServices(subject);
        } catch (NotFoundException e) {
            throw new ServiceNotFoundException("no viewable services for " + subject);
        }

        List<Service> retVal = new ArrayList<Service>();
        for (Service aService: services) {
           if (viewableEntityIds.contains(aService.getId())) {
               retVal.add(aService);
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
        List<Service> services = new ArrayList<Service>();
        Resource server = resourceManager.findResourceById(serverId);
        Set<Resource> serviceResources = server.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: serviceResources) {
            if(service.getType().getId().equals(svcTypeId)) {
                services.add(serviceFactory.createService(service));
            }
        }
        Collections.sort(services, new Comparator<Service>() {
            public int compare(Service o1,Service o2) {
                return (o1.getSortName().compareTo(o2.getSortName()));
            }
        });
        return services;
    }
    
    private List<Service> findByServerOrderName(Integer serverId) {
        List<Service> services = new ArrayList<Service>();
        Resource server = resourceManager.findResourceById(serverId);
        Set<Resource> serviceResources = server.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        Collections.sort(services, new Comparator<Service>() {
            public int compare(Service o1,Service o2) {
                return (o1.getSortName().compareTo(o2.getSortName()));
            }
        });
        return services;
    }
    
    private List<Service> findByServerOrderType(Integer serverId) {
        List<Service> services = new ArrayList<Service>();
        Resource server = resourceManager.findResourceById(serverId);
        Set<Resource> serviceResources = server.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        Collections.sort(services, new Comparator<Service>() {
            public int compare(Service o1,Service o2) {
                return (o1.getServiceType().getSortName().compareTo(o2.getServiceType().getSortName()));
            }
        });
        return services;
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
        Set<Resource> serviceResources = resourceManager.findResourceById(server.getId()).getResourcesFrom(RelationshipTypes.SERVICE);
        Set<Service> services = new HashSet<Service>();
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        return filterUnviewable(subject, services);
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
        List<Service> services = new ArrayList<Service>();
        Resource platform = resourceManager.findResourceById(platId);
        Set<Resource> serviceResources = platform.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        if(asc) {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o1.getSortName().compareTo(o2.getSortName()));
                }
            });
        }else {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o2.getSortName().compareTo(o1.getSortName()));
                }
            });
        }
        return services;
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
        return filterUnviewable(subject, services);
    }


    /**
     * @return A List of ServiceValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        List<Service> services = getServicesByApplication(appId,  pc);
        return valuePager.seek(services, pc);
    }
    
    
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByApplication(AuthzSubject subject, Integer appId,
                                                  Integer serviceTypeId, PageControl pc)
        throws PermissionException, ApplicationNotFoundException, ServiceNotFoundException {
        List<Service> services = getServicesByApplication(appId,  pc);
        return filterAndPage(services, subject, serviceTypeId, pc);
    }

    /**
     * @return A List of Service objects representing all of
     *         the services that the given subject is allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByApplication(AuthzSubject subject, Integer appId)
        throws PermissionException, ApplicationNotFoundException, ServiceNotFoundException {
        return filterUnviewable(subject, getServicesByApplication(appId, PageControl.PAGE_ALL));
    }
    
    private List<Service> findByApplicationOrderSvcName(Integer appId, boolean asc) {
        List<Service> services = new ArrayList<Service>();
        ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
        Set<Resource> serviceResources = app.getMembers();
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        if(asc) {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o1.getName().compareTo(o2.getName()));
                }
            });
        }else {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o2.getName().compareTo(o1.getName()));
                }
            });
        }
        return services;
    }
    
    private List<Service> findByApplicationOrderSvcType(Integer appId, boolean asc) {
        List<Service> services = new ArrayList<Service>();
        ResourceGroup app = resourceGroupManager.findResourceGroupById(appId);
        Set<Resource> serviceResources = app.getMembers();
        for(Resource service: serviceResources) {
            services.add(serviceFactory.createService(service));
        }
        if(asc) {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o1.getServiceType().getName().compareTo(o2.getServiceType().getName()));
                }
            });
        }else {
            Collections.sort(services, new Comparator<Service>() {
                public int compare(Service o1,Service o2) {
                    return (o2.getServiceType().getName().compareTo(o1.getServiceType().getName()));
                }
            });
        }
        return services;
    }

    private List<Service> getServicesByApplication(Integer appId, PageControl pc)
        throws ApplicationNotFoundException {
        // we only look up the application to validate
        // the appId param
        if(resourceGroupManager.findResourceGroupById(appId) == null) {
            throw new ApplicationNotFoundException(appId);
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
        return appServiceCollection;
    }

   
    public void updateServiceZombieStatus(AuthzSubject subject, Service svc, boolean zombieStatus)
        throws PermissionException {
        //TODO perm checks
        //permissionManager.checkModifyPermission(subject, svc.getEntityId());
        Resource resource = resourceManager.findResourceById(svc.getId());
        resource.setModifiedBy(subject.getName());
        resource.setProperty(ServiceFactory.AUTO_DISCOVERY_ZOMBIE,zombieStatus);
        resource.merge();
    }
    
    private void updateService(ServiceValue valueHolder, Resource service) {
        service.setProperty(ServiceFactory.AUTO_INVENTORY_IDENTIFIER,valueHolder.getAutoinventoryIdentifier());
        service.setDescription( valueHolder.getDescription() );
        service.setProperty(ServiceFactory.AUTO_DISCOVERY_ZOMBIE, valueHolder.getAutodiscoveryZombie() );
        service.setProperty(ServiceFactory.SERVICE_RT, valueHolder.getServiceRt() );
        service.setProperty(ServiceFactory.END_USER_RT, valueHolder.getEndUserRt() );
        service.setModifiedBy( valueHolder.getModifiedBy() );
        service.setLocation( valueHolder.getLocation() );
        service.setName( valueHolder.getName() );
        service.merge();
        Resource parent = service.getResourceTo(RelationshipTypes.SERVICE);
        if(valueHolder.getParent() != null && !(parent.getId().equals(valueHolder.getParent().getId()))) {
            service.removeRelationship(parent, RelationshipTypes.SERVICE);
            Resource newParent = resourceManager.findResourceById(valueHolder.getParent().getId());
            newParent.relateTo(service, RelationshipTypes.SERVICE);
        }
    }

    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, ServiceNotFoundException {
        //TODO perm check
        //permissionManager.checkModifyPermission(subject, existing.getEntityId());
        Resource service = resourceManager.findResourceById(existing.getId());
        if(service ==  null) {
            throw new ServiceNotFoundException(existing.getId());
        }

        existing.setModifiedBy(subject.getName());
        if (existing.getDescription() != null)
            existing.setDescription(existing.getDescription().trim());
        if (existing.getLocation() != null)
            existing.setLocation(existing.getLocation().trim());
        if (existing.getName() != null)
            existing.setName(existing.getName().trim());
        
        Service svc = serviceFactory.createService(service);
        if (svc.matchesValueObject(existing)) {
            log.debug("No changes found between value object and entity");
        } else {
            updateService(existing,service);
        }
        return svc;
    }

    public void updateServiceTypes(String plugin, ServiceTypeInfo[] infos)
    throws VetoException, NotFoundException {
    	final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        // First, put all of the infos into a Hash
        HashMap<String, ServiceTypeInfo> infoMap = new HashMap<String, ServiceTypeInfo>();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
        }
        
        try {
            Collection<ResourceType> serviceTypes = getAllServiceResourceTypes();
            Set<ResourceType> curServices = new HashSet<ResourceType>();
            for(ResourceType curResourceType: serviceTypes) {
                if(curResourceType.getPlugin().getName().equals(plugin)) {
                    curServices.add(curResourceType);
                }
            }
            
            for (ResourceType serviceType : curServices) {

                if (log.isDebugEnabled()) {
                    log.debug("Begin updating ServiceTypeLocal: " + serviceType.getName());
                }

                ServiceTypeInfo sinfo = (ServiceTypeInfo) infoMap.remove(serviceType.getName());

                // See if this exists
                if (sinfo == null) {
                    deleteServiceType(serviceFactory.createServiceType(serviceType), overlord);
                } else {
                    // Just update it
                    if (!sinfo.getName().equals(serviceType.getName())) {
                        serviceType.setName(sinfo.getName());
                        serviceType.merge();
                    }
                    if (!sinfo.getDescription().equals(serviceType.getDescription())) {
                        serviceType.setDescription(sinfo.getDescription());
                        serviceType.merge();
                    }

                    // Could be null if servertype was deleted/updated by plugin
                    ResourceType svrtype = serviceType.getResourceTypeTo(RelationshipTypes.SERVICE);

                    if (!sinfo.getServerName().equals(svrtype.getName())) {
                        // Lookup the new server type
                        ResourceType newServerType = resourceManager.findResourceTypeByName(sinfo.getServerName());
                        if (newServerType == null) {
                            throw new NotFoundException("Unable to find server " +
                                                            sinfo.getServerName() +
                                                            " on which service '" +
                                                            serviceType.getName() + "' relies");
                        }
                        serviceType.removeRelationship(svrtype, RelationshipTypes.SERVICE);
                        newServerType.relateTo(svrtype, RelationshipTypes.SERVICE);
                    }
                }
            }

            // Now create the left-overs
            final Set<String> creates = new HashSet<String>();
            for (final ServiceTypeInfo sinfo : infoMap.values()) {
                if (creates.contains(sinfo.getName())) {
                    continue;
                }
                ResourceType servType= resourceManager.findResourceTypeByName(sinfo.getServerName());
                if (servType == null) {
                    throw new NotFoundException("Unable to find server " +
                                                    sinfo.getServerName() +
                                                    " on which service '" +
                                                    sinfo.getName() + "' relies");
                }
                creates.add(sinfo.getName());
                if (debug) watch.markTimeBegin("create");
                createServiceType(sinfo, plugin, servType);
                if (debug) watch.markTimeEnd("create");
            }
        } finally {
            if (debug) log.debug(watch);
        }
    }

    public ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                          ServerType servType) throws NotFoundException {
        return createServiceType(sinfo, plugin, resourceManager.findResourceTypeById(servType.getId()));
    }
    
    public ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                         PlatformType platformType) throws NotFoundException {
       return createServiceType(sinfo, plugin, resourceManager.findResourceTypeById(platformType.getId()));
   }
    
    private ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                          ResourceType parentType) throws NotFoundException {
        ResourceType serviceType = new ResourceType();
        serviceType.setName(sinfo.getName());
        serviceType.setDescription(sinfo.getDescription());
        serviceType.persist();
       
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.AUTO_INVENTORY_IDENTIFIER));
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.CREATION_TIME));
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.MODIFIED_TIME));
        serviceType.addPropertyType(createServicePropertyType(AppdefResource.SORT_NAME));
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.AUTO_DISCOVERY_ZOMBIE));
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.END_USER_RT));
        serviceType.addPropertyType(createServicePropertyType(ServiceFactory.SERVICE_RT));
       
        serviceType.setPlugin(pluginDAO.findByName(plugin));
        parentType.relateTo(serviceType, RelationshipTypes.SERVICE);
        return serviceFactory.createServiceType(serviceType);
    }
    
    private PropertyType createServicePropertyType(String propName) {
        PropertyType propType = new PropertyType();
        propType.setName(propName);
        propType.setDescription(propName);
        propType.persist();
        return propType;
    }
    
    public void deleteServiceType(ServiceType serviceType, AuthzSubject overlord)
        throws VetoException {
        resourceManager.findResourceTypeById(serviceType.getId()).remove();
    }

    /**
     * A removeService method that takes a ServiceLocal. This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     */

    public void removeService(AuthzSubject subject, Service service) throws PermissionException,
        VetoException {
        removeService(subject,service.getId());
    }
    
    public void removeService(AuthzSubject subject, Integer serviceId) throws PermissionException,
    VetoException {
        //TODO perm check
        //permissionManager.checkRemovePermission(subject, aeid);
        resourceManager.removeResource(subject, resourceManager.findResourceById(serviceId));
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * service type, the second element is the # of services of that type in the
     * inventory.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getServiceTypeCounts() {
        Collection<ResourceType> serviceTypes = getAllServiceResourceTypes();
        List<ResourceType> orderedServiceTypes =  new ArrayList<ResourceType>(serviceTypes);
        Collections.sort(orderedServiceTypes, new Comparator<ResourceType>() {
            public int compare(ResourceType o1, ResourceType o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });
        List<Object[]> counts = new ArrayList<Object[]>();
        for(ResourceType serviceType: orderedServiceTypes) {
            counts.add(new Object[]{serviceType.getName(),(long)serviceType.getResources().size()});
        }
        return counts;
    }
      
    private Set<Resource> findAllServiceResources() {
        Set<Resource> resources = new HashSet<Resource>();
        Collection<Resource> platforms = resourceManager.findRootResource().
            getResourcesFrom(RelationshipTypes.PLATFORM);
        for(Resource platform: platforms) {
            Collection<Resource> servers = platform.getResourcesFrom(RelationshipTypes.SERVER);
            for(Resource server: servers) {
                resources.addAll(server.getResourcesFrom(RelationshipTypes.SERVICE));
            }
        }
        for(Resource platform: platforms) {
            resources.addAll(platform.getResourcesFrom(RelationshipTypes.SERVICE));
        }
        return resources;
    }
    
    /**
     * Get the scope of viewable services for a given user
     * @param whoami - the user
     * @return List of ServicePK's for which subject has
     *         AuthzConstants.serviceOpViewService
     */
    private List<Integer> getViewableServices(AuthzSubject whoami) throws PermissionException,
        NotFoundException {
        //TODO perm check
        //Operation op = getOperationByName(resourceManager
          //  .findResourceTypeByName(AuthzConstants.serviceResType),
            //AuthzConstants.serviceOpViewService);
        //List<Integer> idList = permissionManager.findOperationScopeBySubject(whoami, op.getId());
        ArrayList<Integer> idList = new ArrayList<Integer>();
        Set<Resource> services = findAllServiceResources();
        for(Resource service: services) {
            idList.add(service.getId());
        }
        return idList;
    }



    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

}
