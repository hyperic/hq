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
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.MapUtil;
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
    private static final Integer APPDEF_RES_TYPE_UNDEFINED = new Integer(-1);
    private AppServiceDAO appServiceDAO;
    private PermissionManager permissionManager;
    private ServiceDAO serviceDAO;
    private ApplicationDAO applicationDAO;
    private ConfigResponseDAO configResponseDAO;
    private ResourceManager resourceManager;
    private ServerDAO serverDAO;
    private ServerTypeDAO serverTypeDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private ResourceGroupManager resourceGroupManager;
    private CPropManager cpropManager;
    private MeasurementManager measurementManager;
    private AuthzSubjectManager authzSubjectManager;
    private ZeventEnqueuer zeventManager;

    @Autowired
    public ServiceManagerImpl(AppServiceDAO appServiceDAO, PermissionManager permissionManager,
                              ServiceDAO serviceDAO, ApplicationDAO applicationDAO,
                              ConfigResponseDAO configResponseDAO, ResourceManager resourceManager,
                              ServerDAO serverDAO, ServerTypeDAO serverTypeDAO,
                              ServiceTypeDAO serviceTypeDAO,
                              ResourceGroupManager resourceGroupManager, CPropManager cpropManager,
                              MeasurementManager measurementManager,
                              AuthzSubjectManager authzSubjectManager, ZeventEnqueuer zeventManager) {
        this.appServiceDAO = appServiceDAO;
        this.permissionManager = permissionManager;
        this.serviceDAO = serviceDAO;
        this.applicationDAO = applicationDAO;
        this.configResponseDAO = configResponseDAO;
        this.resourceManager = resourceManager;
        this.serverDAO = serverDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.resourceGroupManager = resourceGroupManager;
        this.cpropManager = cpropManager;
        this.measurementManager = measurementManager;
        this.authzSubjectManager = authzSubjectManager;
        this.zeventManager = zeventManager;
    }

    public Service createService(AuthzSubject subject, Server server, ServiceType type,
                                 String name, String desc, String location, Service parent)
        throws PermissionException {
        name = name.trim();
        desc = desc == null ? "" : desc.trim();
        location = location == null ? "" : location.trim();

        Service service = serviceDAO.create(type, server, name, desc, subject.getName(), location,
            subject.getName(), parent);
        // Create the authz resource type. This also does permission checking
        createAuthzService(subject, service);

        // Add Service to parent collection
        server.getServices().add(service);

        ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, service.getEntityId());
        NewResourceEvent event = new NewResourceEvent(server.getResource().getId(),service.getResource());
        zeventManager.enqueueEventAfterCommit(event,2);
        zeventManager.enqueueEventAfterCommit(zevent);
        return service;
    }

    /**
     * Move a Service from one Platform to another.
     * 
     * @param subject The user initiating the move.
     * @param target The target Service to move.
     * @param destination The destination Platform to move this Service to.
     * 
     * @throws org.hyperic.hq.authz.shared.PermissionException If the passed
     *         user does not have permission to move the Service.
     * @throws org.hyperic.hq.common.VetoException If the operation canot be
     *         performed due to incompatible types.
     */

    public void moveService(AuthzSubject subject, Service target, Platform destination)
        throws VetoException, PermissionException {
        ServerType targetType = target.getServer().getServerType();

        Server destinationServer = null;
        for (Server s : destination.getServers()) {

            if (s.getServerType().equals(targetType)) {
                destinationServer = s;
                break;
            }
        }

        if (destinationServer == null) {
            throw new VetoException("Unable find applicable server on platform " +
                                    destination.getName() + " as destination for " +
                                    target.getName());
        }

        moveService(subject, target, destinationServer);
    }

    /**
     * Move a Service from one Server to another.
     * 
     * @param subject The user initiating the move.
     * @param target The target Service to move.
     * @param destination The destination Server to move this Service to.
     * 
     * @throws org.hyperic.hq.authz.shared.PermissionException If the passed
     *         user does not have permission to move the Service.
     * @throws org.hyperic.hq.common.VetoException If the operation canot be
     *         performed due to incompatible types.
     */

    public void moveService(AuthzSubject subject, Service target, Server destination)
        throws VetoException, PermissionException {
        try {
            // Permission checking on destination
            if (destination.getServerType().isVirtual()) {
                permissionManager.checkPermission(subject, resourceManager
                    .findResourceTypeByName(AuthzConstants.platformResType), destination
                    .getPlatform().getId(), AuthzConstants.platformOpAddServer);
            } else {
                permissionManager.checkPermission(subject, resourceManager
                    .findResourceTypeByName(AuthzConstants.serverResType), destination.getId(),
                    AuthzConstants.serverOpAddService);
            }

            // Permission check on target
            permissionManager.checkPermission(subject, resourceManager
                .findResourceTypeByName(AuthzConstants.serviceResType), target.getId(),
                AuthzConstants.serviceOpRemoveService);
        } catch (NotFoundException e) {
            throw new VetoException("Caught NotFoundException checking permission: " +
                                    e.getMessage()); // notgonnahappen
        }

        // Check arguments
        if (!target.getServiceType().getServerType().equals(destination.getServerType())) {
            throw new VetoException("Incompatible resources passed to move(), " +
                                    "cannot move service of type " +
                                    target.getServiceType().getName() + " to " +
                                    destination.getServerType().getName());

        }

        // Unschedule measurements
        measurementManager.disableMeasurements(subject, target.getResource());

        // Reset Service parent id
        target.setServer(destination);

        // Add/Remove Server from Server collections
        target.getServer().getServices().remove(target);
        destination.addService(target);

        // Move Authz resource.
        resourceManager.moveResource(subject, target.getResource(), destination.getResource());

        // Flush to ensure the reschedule of metrics occurs
        serviceDAO.getSession().flush();

        // Reschedule metrics
        ResourceUpdatedZevent zevent = new ResourceUpdatedZevent(subject, target.getEntityId());
        zeventManager.enqueueEventAfterCommit(zevent);
    }

    /**
     * Create a Service which runs on a given server
     * @return The service id.
     */

    public Service createService(AuthzSubject subject, Integer serverId, Integer serviceTypeId,
                                 String name, String desc, String location)
        throws ValidationException, PermissionException, ServerNotFoundException,
        AppdefDuplicateNameException {
        Server server = serverDAO.findById(serverId);
        ServiceType serviceType = serviceTypeDAO.findById(serviceTypeId);
        return createService(subject, server, serviceType, name, desc, location, null);
    }

    /**
     * Create the Authz service resource
     */
    private void createAuthzService(AuthzSubject subject, Service service)
        throws PermissionException {
        log.debug("Begin Authz CreateService");
        try {
            // check to see that the user has permission to addServices
            Server server = service.getServer();
            if (server.getServerType().isVirtual()) {
                // to the server in question
                permissionManager.checkPermission(subject, resourceManager
                    .findResourceTypeByName(AuthzConstants.platformResType), server.getPlatform()
                    .getId(), AuthzConstants.platformOpAddServer);
            } else {
                // to the platform in question
                permissionManager.checkPermission(subject, resourceManager
                    .findResourceTypeByName(AuthzConstants.serverResType), server.getId(),
                    AuthzConstants.serverOpAddService);
            }

            ResourceType serviceProto = resourceManager
                .findResourceTypeByName(AuthzConstants.servicePrototypeTypeName);
            Resource prototype = resourceManager.findResourceByInstanceId(serviceProto, service
                .getServiceType().getId());

            Resource parent = resourceManager.findResource(server.getEntityId());
            if (parent == null) {
                throw new SystemException("Unable to find parent server [id=" +
                                          server.getEntityId() + "]");
            }
            Resource resource = resourceManager.createResource(subject, resourceManager
                .findResourceTypeByName(AuthzConstants.serviceResType), prototype, service.getId(),
                service.getName(), false, parent);
            service.setResource(resource);
        } catch (NotFoundException e) {
            throw new SystemException("Unable to find authz resource type", e);
        }
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

            Collection<Service> services = serviceDAO.findByType(servTypeId, true);
            if (services.size() == 0) {
                return new Integer[0];
            }
            List<Integer> serviceIds = new ArrayList<Integer>(services.size());

            // now get the list of PKs
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
            // There are no viewable servers
            return new Integer[0];
        }
    }

    /**
     * @return List of ServiceValue objects
     */
    @Transactional(readOnly = true)
    public List<ServiceValue> findServicesById(AuthzSubject subject, Integer[] serviceIds)
        throws ServiceNotFoundException, PermissionException {
        List<ServiceValue> serviceList = new ArrayList<ServiceValue>(serviceIds.length);
        for (int i = 0; i < serviceIds.length; i++) {
            Service s = findServiceById(serviceIds[i]);
            permissionManager.checkViewPermission(subject, s.getEntityId());
            serviceList.add(s.getServiceValue());
        }
        return serviceList;
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
        return serviceDAO.get(id);
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
        permissionManager.checkViewPermission(subject, service.getEntityId());
        return service;
    }

    /**
     * @param server {@link Server}
     * @param aiid service autoinventory identifier
     * @return {@link List} of {@link Service}
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByAIID(Server server, String aiid) {
        return serviceDAO.getByAIID(server, aiid);
    }

    /**
     * @param server {@link Server}
     * @param name corresponds to the EAM_RESOURCE.sort_name column
     */
    @Transactional(readOnly = true)
    public Service getServiceByName(Server server, String name) {
        return serviceDAO.findByName(server, name);
    }

    @Transactional(readOnly = true)
    public Service getServiceByName(Platform platform, String name) {
        return serviceDAO.findByName(platform, name);
    }

    /**
     * Find a ServiceType by id
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceType(Integer id) throws ObjectNotFoundException {
        return serviceTypeDAO.findById(id);
    }

    /**
     * Find service type by name
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceTypeByName(String name) {
        return serviceTypeDAO.findByName(name);
    }

    @Transactional(readOnly = true)
    public Collection<Service> findDeletedServices() {
        return serviceDAO.findDeletedServices();
    }

    /**
     * @return PageList of ServiceTypeValues
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getAllServiceTypes(AuthzSubject subject, PageControl pc) {
        Collection<ServiceType> serviceTypes = serviceTypeDAO.findAll();
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**
     * @return List of ServiceTypeValues
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getViewableServiceTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // build the server types from the visible list of servers
        final List<Integer> authzPks = getViewableServices(subject);
        final Collection<ServiceType> serviceTypes = serviceDAO.getServiceTypes(authzPks, true);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> getServiceTypesByServerType(AuthzSubject subject,
                                                                  int serverTypeId) {
        Collection<ServiceType> serviceTypes = serviceTypeDAO.findByServerType_orderName(
            serverTypeId, true);
        if (serviceTypes.size() == 0) {
            return new PageList<ServiceTypeValue>();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> findVirtualServiceTypesByPlatform(AuthzSubject subject,
                                                                        Integer platformId) {
        Collection<ServiceType> serviceTypes = serviceTypeDAO
            .findVirtualServiceTypesByPlatform(platformId.intValue());
        if (serviceTypes.size() == 0) {
            return new PageList<ServiceTypeValue>();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /**
     * @return A List of ServiceValue objects representing all of the services
     *         that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getAllServices(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(getViewableServices(subject, pc), pc);
    }

    /**
     * Get the scope of viewable services for a given user
     * @return List of ServiceLocals for which subject has
     *         AuthzConstants.serviceOpViewService
     */
    private Collection<Service> getViewableServices(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // get list of pks user can view
        final Collection<Integer> authzPks = getViewableServices(subject);
        List<Service> services;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                services = getServices(authzPks, pc);
                Collections.sort(services, new AppdefNameComparator(pc.isAscending()));
                break;
            case SortAttribute.SERVICE_NAME:
                services = getServices(authzPks, pc);
                Collections.sort(services, new AppdefNameComparator(pc.isAscending()));
                break;
            case SortAttribute.CTIME:
                services = getServices(authzPks, pc);
                Collections.sort(services, new ServiceCtimeComparator(pc.isAscending()));
                break;
            default:
                services = getServices(authzPks, pc);
                break;
        }
        return services;
    }

    /**
     * Note: This method pulls all services from the EHCache one by one. This
     * should be faster than pulling everything from the DB since they are in
     * memory.
     */
    private List<Service> getServices(Collection<Integer> authzPks, PageControl pc) {
        final List<Integer> aeids = new ArrayList<Integer>(authzPks);
        final List<Service> rtn = new ArrayList<Service>(authzPks.size());
        final int start = pc.getPageEntityIndex();
        final int end = (pc.getPagesize() == PageControl.SIZE_UNLIMITED) ? authzPks.size() : pc
            .getPagesize() +
                                                                                             start;
        for (int i = start; i < end; i++) {
            final Integer aeid = aeids.get(i);
            try {
                final Service s = findServiceById(aeid);
                final Resource r = s.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                rtn.add(s);
            } catch (ServiceNotFoundException e) {
                log.debug(e.getMessage(), e);
            }
        }
        return rtn;
    }

    private class ServiceCtimeComparator implements Comparator<AppdefResource> {
        final boolean _asc;

        ServiceCtimeComparator(boolean ascending) {
            _asc = ascending;
        }

        public int compare(AppdefResource arg0, AppdefResource arg1) {

            final Long c0 = new Long(arg0.getCreationTime());
            final Long c1 = new Long(arg1.getCreationTime());
            return (_asc) ? c0.compareTo(c1) : c1.compareTo(c0);
        }
    }

    /**
     * Fetch all services that haven't been assigned to a cluster and that
     * haven't been assigned to any applications.
     * @return A List of ServiceValue objects representing all of the unassigned
     *         services that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getAllClusterAppUnassignedServices(AuthzSubject subject,
                                                                     PageControl pc)
        throws PermissionException, NotFoundException {
        // get list of pks user can view
        Set<Integer> authzPks = new HashSet<Integer>(getViewableServices(subject));
        Collection<Service> services = null;
        Collection<Service> toBePaged = new ArrayList<Service>();
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
                if (pc != null) {
                    services = serviceDAO.findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            case SortAttribute.SERVICE_NAME:
                if (pc != null) {
                    services = serviceDAO.findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            default:
                services = serviceDAO.findAllClusterAppUnassigned_orderName(true);
                break;
        }
        for (Service aService : services) {

            // remove service if its not viewable
            if (authzPks.contains(aService.getId())) {
                toBePaged.add(aService);
            }
        }
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(toBePaged, pc);
    }

    private PageList filterAndPage(Collection svcCol, AuthzSubject subject, Integer svcTypeId,
                                   PageControl pc) throws ServiceNotFoundException,
        PermissionException {
        List services = new ArrayList();
        // iterate over the services and only include those whose pk is
        // present in the viewablePKs list
        if (svcTypeId != null && svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
            for (Iterator it = svcCol.iterator(); it.hasNext();) {
                Object o = it.next();
                Integer thisSvcTypeId;

                if (o instanceof Service) {
                    thisSvcTypeId = ((Service) o).getServiceType().getId();
                } else {
                    ResourceGroup cluster = (ResourceGroup) o;
                    thisSvcTypeId = cluster.getResourcePrototype().getInstanceId();
                }
                // first, if they specified a server type, then filter on it
                if (!(thisSvcTypeId.equals(svcTypeId))) {
                    continue;
                }

                services.add(o instanceof Service ? o : getServiceCluster((ResourceGroup) o));
            }
        } else {
            services.addAll(svcCol);
        }

        List toBePaged = filterUnviewable(subject, services);
        return valuePager.seek(toBePaged, pc);
    }

    /**
     * @return {@link List} of {@link AppdefEntityID}s that represent the total
     *         set of service inventory that the subject is authorized to see.
     *         This includes all services as well as all clusters
     */
    protected List<AppdefEntityID> getViewableServiceInventory(AuthzSubject whoami)
        throws PermissionException, NotFoundException {
        List<Integer> idList = getViewableServices(whoami);
        List<AppdefEntityID> ids = new ArrayList<AppdefEntityID>();
        for (int i = 0; i < idList.size(); i++) {
            Integer pk = (Integer) idList.get(i);
            ids.add(AppdefEntityID.newServiceID(pk));
        }

        Collection<Integer> viewableGroups = permissionManager.findOperationScopeBySubject(whoami,
            AuthzConstants.groupOpViewResourceGroup, AuthzConstants.groupResourceTypeName);
        List<AppdefEntityID> groupIds = new ArrayList<AppdefEntityID>();
        for (Integer gid : viewableGroups) {
            groupIds.add(AppdefEntityID.newGroupID(gid));
        }
        ids.addAll(groupIds);
        return ids;
    }

    /**
     * Get the scope of viewable services for a given user
     * @param whoami - the user
     * @return List of ServicePK's for which subject has
     *         AuthzConstants.serviceOpViewService
     */
    protected List<Integer> getViewableServices(AuthzSubject whoami)
    throws PermissionException, NotFoundException {
        Operation op = getOperationByName(
            resourceManager.findResourceTypeByName(AuthzConstants.serviceResType),
            AuthzConstants.serviceOpViewService);
        Collection<Integer> idList = permissionManager.findOperationScopeBySubject(whoami, op.getId());
        return new ArrayList<Integer>(idList);
    }

    private List filterUnviewable(AuthzSubject subject, Collection services)
        throws PermissionException, ServiceNotFoundException {
        List<AppdefEntityID> viewableEntityIds;
        try {
            viewableEntityIds = getViewableServiceInventory(subject);
        } catch (NotFoundException e) {
            throw new ServiceNotFoundException("no viewable services for " + subject);
        }

        List retVal = new ArrayList();
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
                if (viewableEntityIds != null && viewableEntityIds.contains(aService.getEntityId())) {
                    retVal.add(o);
                }
            } else if (o instanceof ResourceGroup) {
                ResourceGroup aCluster = (ResourceGroup) o;
                AppdefEntityID clusterId = AppdefEntityID.newGroupID(aCluster.getId());
                if (viewableEntityIds != null && viewableEntityIds.contains(clusterId)) {
                    retVal.add(getServiceCluster(aCluster));
                }
            }
        }
        return retVal;
    }

    /**
     * Get services by server and type.
     */
    @Transactional(readOnly = true)
    public PageList getServicesByServer(AuthzSubject subject, Integer serverId, PageControl pc)
        throws ServiceNotFoundException, ServerNotFoundException, PermissionException {
        return getServicesByServer(subject, serverId, null, pc);
    }

    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId,
                                                      Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        List toBePaged = getServicesByServerImpl(subject, serverId, svcTypeId, pc);
        return valuePager.seek(toBePaged, pc);
    }

    private List getServicesByServerImpl(AuthzSubject subject, Integer serverId, Integer svcTypeId,
                                         PageControl pc) throws PermissionException,
        ServiceNotFoundException {
        if (svcTypeId == null) {
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;
        }

        List<Service> services;

        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_TYPE:
                services = serviceDAO.findByServer_orderType(serverId);
                break;
            case SortAttribute.SERVICE_NAME:
            default:
                if (svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
                    services = serviceDAO.findByServerAndType_orderName(serverId, svcTypeId);
                } else {
                    services = serviceDAO.findByServer_orderName(serverId);
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
    public List getServicesByServer(AuthzSubject subject, Server server)
        throws PermissionException, ServiceNotFoundException {
        return filterUnviewable(subject, server.getServices());
    }

    @Transactional(readOnly = true)
    public Integer[] getServiceIdsByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException {
        if (svcTypeId == null)
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;

        List<Service> services;

        if (svcTypeId == APPDEF_RES_TYPE_UNDEFINED) {
            services = serviceDAO.findByServer_orderType(serverId);
        } else {
            services = serviceDAO.findByServerAndType_orderName(serverId, svcTypeId);
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

    @Transactional(readOnly = true)
    public List<ServiceValue> getServicesByType(AuthzSubject subject, String svcName, boolean asc)
        throws PermissionException, InvalidAppdefTypeException {
        ServiceType st = serviceTypeDAO.findByName(svcName);
        if (st == null) {
            return new PageList<ServiceValue>();
        }

        try {
            Collection<Service> services = serviceDAO.findByType(st.getId(), asc);
            if (services.size() == 0) {
                return new PageList<ServiceValue>();
            }
            List<ServiceValue> toBePaged = filterUnviewable(subject, services);
            return valuePager.seek(toBePaged, PageControl.PAGE_ALL);
        } catch (ServiceNotFoundException e) {
            return new PageList<ServiceValue>();
        }
    }

    @Transactional(readOnly = true)
    public PageList getServicesByService(AuthzSubject subject, Integer serviceId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        return getServicesByService(subject, serviceId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get services by server.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByService(AuthzSubject subject, Integer serviceId,
                                                       Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        // find any children
        Collection<Service> childSvcs = serviceDAO.findByParentAndType(serviceId, svcTypeId);
        return filterAndPage(childSvcs, subject, svcTypeId, pc);
    }

    /**
     * Get service IDs by service.
     */
    @Transactional(readOnly = true)
    public Integer[] getServiceIdsByService(AuthzSubject subject, Integer serviceId,
                                            Integer svcTypeId) throws ServiceNotFoundException,
        PermissionException {
        // find any children
        Collection<Service> childSvcs = serviceDAO.findByParentAndType(serviceId, svcTypeId);

        List<Service> viewables = filterUnviewable(subject, childSvcs);

        Integer[] ids = new Integer[viewables.size()];
        Iterator<Service> it = viewables.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Service local = it.next();
            ids[i] = local.getId();
        }

        return ids;
    }

    @Transactional(readOnly = true)
    public PageList getServicesByPlatform(AuthzSubject subject, Integer platId, PageControl pc)
        throws ServiceNotFoundException, PlatformNotFoundException, PermissionException {
        return getServicesByPlatform(subject, platId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get platform services (children of virtual servers)
     */
    @Transactional(readOnly = true)
    public PageList getPlatformServices(AuthzSubject subject, Integer platId, PageControl pc)
        throws PlatformNotFoundException, PermissionException, ServiceNotFoundException {
        return getPlatformServices(subject, platId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get platform services (children of virtual servers) of a specified type
     */
    @Transactional(readOnly = true)
    public PageList getPlatformServices(AuthzSubject subject, Integer platId, Integer typeId,
                                        PageControl pc) throws PlatformNotFoundException,
        PermissionException, ServiceNotFoundException {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);
        Collection<Service> allServices = serviceDAO.findPlatformServices_orderName(platId, pc
            .isAscending());
        return filterAndPage(allServices, subject, typeId, pc);
    }

    /**
     * Get {@link Service}s which are children of the server, and of the
     * specified type.
     */
    @Transactional(readOnly = true)
    public List<Service> findServicesByType(Server server, ServiceType st) {
        return serviceDAO.findByServerAndType_orderName(server.getId(), st.getId());
    }

    /**
     * Get platform service POJOs
     */
    @Transactional(readOnly = true)
    public List<Service> findPlatformServicesByType(Platform p, ServiceType st) {
        return serviceDAO.findPlatformServicesByType(p, st);
    }

    /**
     * Get platform service POJOs
     */
    @Transactional(readOnly = true)
    public Collection getPlatformServices(AuthzSubject subject, Integer platId)
        throws ServiceNotFoundException, PermissionException {
        Collection<Service> services = serviceDAO.findPlatformServices_orderName(platId, true);
        return filterUnviewable(subject, services);
    }

    /**
     * Get platform services (children of virtual servers), mapped by type id of
     * a specified type
     */
    @Transactional(readOnly = true)
    public Map<Integer, List> getMappedPlatformServices(AuthzSubject subject, Integer platId,
                                                        PageControl pc)
        throws PlatformNotFoundException, PermissionException, ServiceNotFoundException {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        Collection<Service> allServices = serviceDAO.findPlatformServices_orderName(platId, pc
            .isAscending());
        HashMap<Integer, List> retMap = new HashMap<Integer, List>();

        // Map all services by type ID
        for (Service svc : allServices) {

            Integer typeId = svc.getServiceType().getId();
            List<Service> addTo = (List<Service>) MapUtil.getOrCreate(retMap, typeId,
                ArrayList.class);

            addTo.add(svc);
        }

        // Page the lists before returning
        for (Map.Entry<Integer, List> entry : retMap.entrySet()) {

            Integer typeId = entry.getKey();
            List svcs = entry.getValue();

            PageControl pcCheck = svcs.size() <= pc.getPagesize() ? PageControl.PAGE_ALL : pc;

            svcs = filterAndPage(svcs, subject, typeId, pcCheck);
            entry.setValue(svcs);
        }

        return retMap;
    }

    /**
     * Get services by platform.
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> getServicesByPlatform(AuthzSubject subject, Integer platId,
                                                        Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PlatformNotFoundException, PermissionException {
        Collection<Service> allServices;
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_NAME:
                allServices = serviceDAO.findByPlatform_orderName(platId, pc.isAscending());
                break;
            case SortAttribute.SERVICE_TYPE:
                allServices = serviceDAO.findByPlatform_orderType(platId, pc.isAscending());
                break;
            default:
                throw new IllegalArgumentException("Invalid sort attribute");
        }
        return filterAndPage(allServices, subject, svcTypeId, pc);
    }

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList getServicesByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        return getServicesByApplication(subject, appId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    @Transactional(readOnly = true)
    public PageList getServicesByApplication(AuthzSubject subject, Integer appId,
                                             Integer svcTypeId, PageControl pc)
        throws PermissionException, ApplicationNotFoundException, ServiceNotFoundException {
        return filterAndPage(getServicesByApplication(appId, pc), subject, svcTypeId, pc);
    }

    /**
     * @return A List of Service and ServiceCluster objects representing all of
     *         the services that the given subject is allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    @Transactional(readOnly = true)
    public List getServicesByApplication(AuthzSubject subject, Integer appId)
        throws PermissionException, ApplicationNotFoundException, ServiceNotFoundException {
        return filterUnviewable(subject, getServicesByApplication(appId, PageControl.PAGE_ALL));
    }

    private List getServicesByApplication(Integer appId, PageControl pc)
        throws ApplicationNotFoundException {
        try {
            // we only look up the application to validate
            // the appId param
            applicationDAO.findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }

        Collection<AppService> appServiceCollection;

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_NAME:
            case SortAttribute.RESOURCE_NAME:
                appServiceCollection = appServiceDAO.findByApplication_orderSvcName(appId, pc
                    .isAscending());
                break;
            case SortAttribute.SERVICE_TYPE:
                appServiceCollection = appServiceDAO.findByApplication_orderSvcType(appId, pc
                    .isAscending());
                break;
            default:
                throw new IllegalArgumentException("Unsupported sort " + "attribute [" +
                                                   pc.getSortattribute() + "] on PageControl : " +
                                                   pc);
        }

        List services = new ArrayList();
        for (AppService appService : appServiceCollection) {

            if (appService.isIsGroup()) {
                services.add(appService.getResourceGroup());
            } else {
                services.add(appService.getService());
            }
        }
        return services;
    }

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList getServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                     PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        return getServiceInventoryByApplication(subject, appId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get all services by application. This is to only be used for the Evident
     * API.
     */
    @Transactional(readOnly = true)
    public PageList getFlattenedServicesByApplication(AuthzSubject subject, Integer appId,
                                                      Integer typeId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        if (typeId == null)
            typeId = APPDEF_RES_TYPE_UNDEFINED;

        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();

        Application appLocal;
        try {
            appLocal = applicationDAO.findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }

        Collection<Service> svcCollection = new ArrayList<Service>();
        if (debug)
            watch.markTimeBegin("getAppServices");
        Collection<AppService> appSvcCollection = appLocal.getAppServices();
        if (debug)
            watch.markTimeEnd("getAppServices");

        for (AppService appService : appSvcCollection) {

            if (appService.isIsGroup()) {
                if (debug)
                    watch.markTimeBegin("getServiceCluster");
                ServiceCluster cluster = getServiceCluster(appService.getResourceGroup());
                if (debug)
                    watch.markTimeEnd("getServiceCluster");
                if (debug)
                    watch.markTimeBegin("getServices");
                svcCollection.addAll(cluster.getServices());
                if (debug)
                    watch.markTimeEnd("getServices");
            } else {
                svcCollection.add(appService.getService());
            }
        }

        if (debug)
            log.debug(watch);
        return filterAndPage(svcCollection, subject, typeId, pc);
    }

    /**
     * @return A List of ServiceValue and ServiceClusterValue objects
     *         representing all of the services that the given subject is
     *         allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList getServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                     Integer svcTypeId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException, PermissionException {
        if (svcTypeId == null || svcTypeId.equals(APPDEF_RES_TYPE_UNDEFINED)) {
            List services = getUnflattenedServiceInventoryByApplication(subject, appId, pc);
            return filterAndPage(services, subject, APPDEF_RES_TYPE_UNDEFINED, pc);
        } else {
            return getFlattenedServicesByApplication(subject, appId, svcTypeId, pc);
        }
    }

    /**
     * Get all service inventory by application, including those inside an
     * associated cluster
     * 
     * @param subject The subject trying to list services.
     * @param appId Application id.
     * @return A List of ServiceValue objects representing all of the services
     *         that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public Integer[] getFlattenedServiceIdsByApplication(AuthzSubject subject, Integer appId)
        throws ServiceNotFoundException, PermissionException, ApplicationNotFoundException {

        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug)
            watch.markTimeBegin("getUnflattenedServiceInventoryByApplication");
        List serviceInventory = getUnflattenedServiceInventoryByApplication(subject, appId,
            PageControl.PAGE_ALL);
        if (debug)
            watch.markTimeEnd("getUnflattenedServiceInventoryByApplication");

        List<Integer> servicePKs = new ArrayList<Integer>();
        // flattening: open up all of the groups (if any) and get their services
        // as well
        try {
            for (Iterator iter = serviceInventory.iterator(); iter.hasNext();) {
                Object o = iter.next();
                // applications can have both clusters and services
                if (o instanceof Service) {
                    Service service = (Service) o;
                    // servers will only have these
                    servicePKs.add(service.getId());
                } else {
                    // this only happens when entId is for an application and
                    // a cluster is bound to it
                    ResourceGroup cluster = (ResourceGroup) o;
                    AppdefEntityID groupId = AppdefEntityID.newGroupID(cluster.getId());
                    // any authz resource filtering on the group members happens
                    // inside the group subsystem
                    try {
                        if (debug)
                            watch.markTimeBegin("getCompatGroupMembers");
                        List<AppdefEntityID> memberIds = GroupUtil.getCompatGroupMembers(subject,
                            groupId, null, PageControl.PAGE_ALL);
                        if (debug)
                            watch.markTimeEnd("getCompatGroupMembers");
                        for (AppdefEntityID memberEntId : memberIds) {

                            servicePKs.add(memberEntId.getId());
                        }
                    } catch (PermissionException e) {
                        // User not allowed to see this group
                        log.debug("User " + subject + " not allowed to view " + "group " + groupId);
                    }
                }
            }
        } catch (GroupNotCompatibleException e) {
            throw new InvalidAppdefTypeException(
                "serviceInventory has groups that are not compatible", e);
        } catch (AppdefEntityNotFoundException e) {
            throw new ServiceNotFoundException("could not return all services", e);
        }
        if (debug)
            log.debug(watch);
        return (Integer[]) servicePKs.toArray(new Integer[servicePKs.size()]);
    }

    private List getUnflattenedServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                             PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();

        try {
            applicationDAO.findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        Collection<AppService> appServices = null;
        switch (pc.getSortattribute()) {
            case SortAttribute.SERVICE_NAME:
            case SortAttribute.RESOURCE_NAME:
            case SortAttribute.NAME:
                // TODO: Not actually sorting
                if (debug)
                    watch.markTimeBegin("findByApplication_orderName");
                appServices = appServiceDAO.findByApplication_orderName(appId);
                if (debug)
                    watch.markTimeEnd("findByApplication_orderName");
                break;
            case SortAttribute.SERVICE_TYPE:
                if (debug)
                    watch.markTimeBegin("findByApplication_orderType");
                appServices = appServiceDAO.findByApplication_orderType(appId, pc.isAscending());
                if (debug)
                    watch.markTimeEnd("findByApplication_orderType");
                break;
            default:
                throw new IllegalArgumentException("Unsupported sort attribute [" +
                                                   pc.getSortattribute() + "] on PageControl : " +
                                                   pc);
        }

        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are
        // allowed to return.

        List services = new ArrayList(appServices.size());
        for (AppService appService : appServices) {
            if (appService.isIsGroup()) {
                if (debug)
                    watch.markTimeBegin("appService.getResourceGroup");
                services.add(appService.getResourceGroup());
                if (debug)
                    watch.markTimeEnd("appService.getResourceGroup");
            } else {
                if (debug)
                    watch.markTimeBegin("appService.getService");
                services.add(appService.getService());
                if (debug)
                    watch.markTimeEnd("appService.getService");
            }
        }
        if (debug)
            log.debug(watch);
        return services;
    }

    public void updateServiceZombieStatus(AuthzSubject subject, Service svc, boolean zombieStatus)
        throws PermissionException {
        permissionManager.checkModifyPermission(subject, svc.getEntityId());
        svc.setModifiedBy(subject.getName());
        svc.setAutodiscoveryZombie(zombieStatus);
    }

    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, UpdateException, AppdefDuplicateNameException,
        ServiceNotFoundException {
        permissionManager.checkModifyPermission(subject, existing.getEntityId());
        Service service = serviceDAO.findById(existing.getId());
        String oldName = null;

        existing.setModifiedBy(subject.getName());
        if (existing.getDescription() != null)
            existing.setDescription(existing.getDescription().trim());
        if (existing.getLocation() != null)
            existing.setLocation(existing.getLocation().trim());
        if (existing.getName() != null){
            oldName = service.getName();
            existing.setName(existing.getName().trim());
        }

        Map<String, String> changedProps = service.changedProperties(existing);
        if (changedProps.isEmpty()) {
            log.debug("No changes found between value object and entity");
        } else {
            service.updateService(existing);
            Resource r = service.getResource();
            this.zeventManager.enqueueEventAfterCommit(
                    new ResourceContentChangedZevent(r.getId(),r.getName(), null, changedProps, oldName));
        }
        return service;
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

        List<ServerType> types = serverTypeDAO.findByName(names);
        HashMap<String, ServerType> serverTypes = new HashMap<String, ServerType>(types.size());
        for (ServerType type : types) {
            serverTypes.put(type.getName(), type);
        }

        try {
            Collection<ServiceType> curServices = serviceTypeDAO.findByPlugin(plugin);

            for (ServiceType serviceType : curServices) {

                if (log.isDebugEnabled()) {
                    log.debug("Begin updating ServiceTypeLocal: " + serviceType.getName());
                }

                ServiceTypeInfo sinfo = (ServiceTypeInfo) infoMap.remove(serviceType.getName());

                // See if this exists
                if (sinfo == null) {
                    deleteServiceType(serviceType, overlord, resourceGroupManager, resourceManager);
                } else {
                    // Just update it
                    // XXX TODO MOVE THIS INTO THE ENTITY
                    if (!sinfo.getName().equals(serviceType.getName()))
                        serviceType.setName(sinfo.getName());

                    if (!sinfo.getDescription().equals(serviceType.getDescription()))
                        serviceType.setDescription(sinfo.getDescription());

                    if (sinfo.getInternal() != serviceType.isIsInternal())
                        serviceType.setIsInternal(sinfo.getInternal());

                    // Could be null if servertype was deleted/updated by plugin
                    ServerType svrtype = serviceType.getServerType();

                    // Check server type
                    if (svrtype == null || !sinfo.getServerName().equals(svrtype.getName())) {
                        // Lookup the server type
                        if (null == (svrtype = serverTypes.get(sinfo.getServerName()))) {
                            svrtype = serverTypeDAO.findByName(sinfo.getServerName());
                            if (svrtype == null) {
                                throw new NotFoundException("Unable to find server " +
                                                            sinfo.getServerName() +
                                                            " on which service '" +
                                                            serviceType.getName() + "' relies");
                            }
                            serverTypes.put(svrtype.getName(), svrtype);
                        }
                        serviceType.setServerType(svrtype);
                    }
                }
            }

            // Now create the left-overs
            final ResourceType resType = resourceManager
                .findResourceTypeByName(AuthzConstants.servicePrototypeTypeName);
            final Resource rootResource = resourceManager.findRootResource();
            final Set<String> creates = new HashSet<String>();
            for (final ServiceTypeInfo sinfo : infoMap.values()) {
                ServerType servType;
                if (null == (servType = serverTypes.get(sinfo.getServerName()))) {
                    servType = serverTypeDAO.findByName(sinfo.getServerName());
                    serverTypes.put(servType.getName(), servType);
                }
                if (creates.contains(sinfo.getName())) {
                    continue;
                }
                creates.add(sinfo.getName());
                if (debug) watch.markTimeBegin("create");
                createServiceType(sinfo, plugin, servType, rootResource, resType);
                if (debug) watch.markTimeEnd("create");
            }
        } finally {
            if (debug) log.debug(watch);
        }
    }

    public ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin, ServerType servType)
        throws NotFoundException {
        Resource rootResource = resourceManager.findRootResource();
        return createServiceType(sinfo, plugin, servType, rootResource, resourceManager
            .findResourceTypeByName(AuthzConstants.servicePrototypeTypeName));
    }

    private ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                          ServerType servType, Resource rootResource,
                                          ResourceType serviceType) throws NotFoundException {
        ServiceType stype = serviceTypeDAO.create(sinfo.getName(), plugin, sinfo.getDescription(),
            sinfo.getInternal());
        stype.setServerType(servType);
        resourceManager.createResource(authzSubjectManager.getOverlordPojo(), serviceType,
            rootResource, stype.getId(), stype.getName(), false, null);
        return stype;
    }

    public void deleteServiceType(ServiceType serviceType, AuthzSubject overlord,
                                  ResourceGroupManager resGroupMan, ResourceManager resMan)
        throws VetoException {
        Resource proto = resMan.findResourceByInstanceId(AuthzConstants.authzServiceProto,
            serviceType.getId());

        try {
            // Delete compatible groups of this type.
            resGroupMan.removeGroupsCompatibleWith(proto);

            // Remove all services
            Service[] services = serviceType.getServices().toArray(
                new Service[serviceType.getServices().size()]);
            for (int i = 0; i < services.length; i++) {
                removeService(overlord, services[i]);
            }
        } catch (PermissionException e) {
            assert false : "Overlord should not run into PermissionException";
        }

        serviceTypeDAO.remove(serviceType);

        resMan.removeResource(overlord, proto);
    }

    /**
     * Map a ResourceGroup to ServiceCluster, just temporary, should be able to
     * remove when done with the ServiceCluster to ResourceGroup Migration
     */
    @Transactional(readOnly = true)
    public ServiceCluster getServiceCluster(ResourceGroup group) {
        if (group == null) {
            return null;
        }
        ServiceCluster sc = new ServiceCluster();
        sc.setName(group.getName());
        sc.setDescription(group.getDescription());
        sc.setGroup(group);

        Collection<Resource> resources = resourceGroupManager.getMembers(group);

        Set<Service> services = new HashSet<Service>(resources.size());

        ServiceType st = null;
        for (Resource resource : resources) {

            // this should not be the case
            if (!resource.getResourceType().getId().equals(AuthzConstants.authzService)) {
                continue;
            }
            Service service = serviceDAO.findById(resource.getInstanceId());
            if (st == null) {
                st = service.getServiceType();
            }
            services.add(service);
            service.setResourceGroup(sc.getGroup());
        }
        sc.setServices(services);

        if (st == null && group.getResourcePrototype() != null) {
            st = serviceTypeDAO.findById(group.getResourcePrototype().getInstanceId());
        }

        if (st != null) {
            sc.setServiceType(st);
        }
        return sc;
    }

    /**
     * A removeService method that takes a ServiceLocal. This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     */

    public void removeService(AuthzSubject subject, Service service) throws PermissionException,
        VetoException {
        AppdefEntityID aeid = service.getEntityId();
        permissionManager.checkRemovePermission(subject, aeid);

        // Remove service from parent Server's Services collection
        Server server = service.getServer();
        if (server != null) {
            server.getServices().remove(service);
        }

        // Remove from ServiceType collection
        service.getServiceType().getServices().remove(service);

        final ConfigResponseDB config = service.getConfigResponse();

        // remove from appdef

        serviceDAO.remove(service);

        // remove the config response
        if (config != null) {
            configResponseDAO.remove(config);
        }

        // remove custom properties
        cpropManager.deleteValues(aeid.getType(), aeid.getID());

        // Remove authz resource.
        resourceManager.removeAuthzResource(subject, aeid, service.getResource());

        serviceDAO.getSession().flush();
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

    public void handleResourceDelete(Resource resource) {
        resource.setResourceType(null);
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * service type, the second element is the # of services of that type in the
     * inventory.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getServiceTypeCounts() {
        return serviceDAO.getServiceTypeCounts();
    }

    /**
     * Get the # of services within HQ inventory
     */
    @Transactional(readOnly = true)
    public Number getServiceCount() {
        return serviceDAO.getServiceCount();
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

    public Collection<Service> getOrphanedServices() {
        return serviceDAO.getOrphanedServices();
    }

}
