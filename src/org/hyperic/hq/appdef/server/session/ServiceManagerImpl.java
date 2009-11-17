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
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
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
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.server.session.MeasurementManagerImpl;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.MapUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ServiceManagerImpl implements ServiceManager {

    private final Log log = LogFactory.getLog(ServiceManagerImpl.class);

    private static final String VALUE_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_service";
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
    
    
    
    @Autowired
    public ServiceManagerImpl(AppServiceDAO appServiceDAO, PermissionManager permissionManager, ServiceDAO serviceDAO,
                              ApplicationDAO applicationDAO, ConfigResponseDAO configResponseDAO,
                              ResourceManager resourceManager, ServerDAO serverDAO, ServerTypeDAO serverTypeDAO,
                              ServiceTypeDAO serviceTypeDAO, ResourceGroupManager resourceGroupManager,
                              CPropManager cpropManager) {
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
    }

    /**
     * 
     */
    public Service createService(AuthzSubject subject, Server server,
                                 ServiceType type, String name,  String desc,
                                 String location, Service parent)
        throws PermissionException
    {
        name     = name.trim();
        desc     = desc == null ? "" : desc.trim();
        location = location == null ? "" : location.trim();

        Service service = serviceDAO.create(type, server, name, desc,
                                                 subject.getName(), location,
                                                 subject.getName(), parent);
        // Create the authz resource type.  This also does permission checking
        createAuthzService(subject, service);

        // Add Service to parent collection
        server.getServices().add(service);

        ResourceCreatedZevent zevent =
            new ResourceCreatedZevent(subject, service.getEntityId());
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
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
     * user does not have permission to move the Service.
     * @throws org.hyperic.hq.common.VetoException If the operation canot be
     * performed due to incompatible types.
     *
     * 
     */
    public void moveService(AuthzSubject subject, Service target,
                            Platform destination)
        throws VetoException, PermissionException
    {
        ServerType targetType = target.getServer().getServerType();

        Server destinationServer = null;
        for (Iterator i = destination.getServers().iterator(); i.hasNext();) {
            Server s = (Server)i.next();
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
     * user does not have permission to move the Service.
     * @throws org.hyperic.hq.common.VetoException If the operation canot be
     * performed due to incompatible types.
     *
     * 
     */
    public void moveService(AuthzSubject subject, Service target,
                            Server destination)
        throws VetoException, PermissionException
    {
        try {
            // Permission checking on destination
            if (destination.getServerType().isVirtual()) {
                permissionManager.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.platformResType),
                                destination.getPlatform().getId(),
                                AuthzConstants.platformOpAddServer);
            } else {
                permissionManager.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.serverResType),
                                destination.getId(),
                                AuthzConstants.serverOpAddService);
            }

            // Permission check on target
            permissionManager.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.serviceResType),
                            target.getId(), AuthzConstants.serviceOpRemoveService);
        } catch (FinderException e) {
            // TODO: FinderException needs to be expelled from this class.
            throw new VetoException("Caught FinderException checking permission: " +
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
        MeasurementManagerImpl.getOne().disableMeasurements(subject,
                                                               target.getResource());

        // Reset Service parent id
        target.setServer(destination);

        // Add/Remove Server from Server collections
        target.getServer().getServices().remove(target);
        destination.addService(target);

        // Move Authz resource.
        resourceManager.moveResource(subject, target.getResource(),
                                          destination.getResource());

        // Flush to ensure the reschedule of metrics occurs
        DAOFactory.getDAOFactory().getCurrentSession().flush();

        // Reschedule metrics
        ResourceUpdatedZevent zevent =
            new ResourceUpdatedZevent(subject, target.getEntityId());
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }

    /**
     * Create a Service which runs on a given server
     * @return The service id.
     * 
     */
    public Service createService(AuthzSubject subject, Integer serverId,
                                 Integer serviceTypeId, String name,
                                 String desc, String location)
        throws CreateException, ValidationException, PermissionException,
               ServerNotFoundException, AppdefDuplicateNameException
    {
        Server server = serverDAO.findById(serverId);
        ServiceType serviceType = serviceTypeDAO.findById(serviceTypeId);
        return createService(subject, server, serviceType, name, desc,
                             location, null);
    }

    /**
     * Create the Authz service resource
     */
    private void createAuthzService(AuthzSubject subject, Service service)
        throws PermissionException
    {
        log.debug("Begin Authz CreateService");
        try {
            // check to see that the user has permission to addServices
            Server server = service.getServer();
            if (server.getServerType().isVirtual()) {
                // to the server in question
                permissionManager.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.platformResType),
                                server.getPlatform().getId(),
                                AuthzConstants.platformOpAddServer);
            } else {
                // to the platform in question
                permissionManager.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.serverResType),
                                server.getId(),
                                AuthzConstants.serverOpAddService);
            }

           
            ResourceType serviceProto = resourceManager.findResourceTypeByName(AuthzConstants.servicePrototypeTypeName);
            Resource prototype =
                resourceManager.findResourceByInstanceId(serviceProto,
                                              service.getServiceType().getId());

            Resource parent = resourceManager.findResource(server.getEntityId());
            if (parent == null) {
                throw new SystemException("Unable to find parent server [id=" +
                                          server.getEntityId() + "]");
            }
            Resource resource =  resourceManager.createResource(subject,
                resourceManager.findResourceTypeByName(AuthzConstants.serviceResType),
                                                    prototype,
                                                    service.getId(),
                                                    service.getName(), false,parent);
            service.setResource(resource);
        } catch(FinderException e) {
            throw new SystemException("Unable to find authz resource type", e);
        }
    }

    /**
     * Get service IDs by service type.
     * 
     *
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    public Integer[] getServiceIds(AuthzSubject subject, Integer servTypeId)
        throws PermissionException {
        
        try {
           
            Collection services = serviceDAO.findByType(servTypeId, true);
            if (services.size() == 0) {
                return new Integer[0];
            }
            List serviceIds = new ArrayList(services.size());

            // now get the list of PKs
            Set viewable = new HashSet(getViewableServices(subject));
            // and iterate over the ejbList to remove any item not in the
            // viewable list
            int i = 0;
            for (Iterator it = services.iterator(); it.hasNext(); i++) {
                Service aEJB = (Service) it.next();
                if (viewable.contains(aEJB.getId())) {
                    // add the item, user can see it
                    serviceIds.add(aEJB.getId());
                }
            }

            return (Integer[]) serviceIds.toArray(new Integer[0]);
        } catch (FinderException e) {
            // There are no viewable servers
            return new Integer[0];
        }
    }

    /**
     * @return List of ServiceValue objects
     * 
     */
    public List findServicesById(AuthzSubject subject, Integer[] serviceIds)
        throws ServiceNotFoundException, PermissionException {
        List serviceList = new ArrayList(serviceIds.length);
        for(int i = 0; i < serviceIds.length; i++) {
            Service s = findServiceById(serviceIds[i]);
            permissionManager.checkViewPermission(subject, s.getEntityId());
            serviceList.add(s.getServiceValue());
        }
        return serviceList;
    }

    /**
     * Find Service by Id.
     * 
     */
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
     * 
     * @return The Service identified by this id, or null if it does not exist.
     */
    public Service getServiceById(Integer id) {
        return serviceDAO.get(id);
    }

    /**
     * Get Service by Id and perform permission check.
     * 
     * 
     * @return The Service identified by this id.
     */
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
     * 
     */
    public List getServicesByAIID(Server server, String aiid) {
        return serviceDAO.getByAIID(server, aiid);
    }

    /**
     * @param server {@link Server}
     * @param name corresponds to the EAM_RESOURCE.sort_name column
     * 
     */
    public Service getServiceByName(Server server, String name) {
        return serviceDAO.findByName(server, name);
    }

    /**
     * 
     */
    public Service getServiceByName(Platform platform, String name) {
        return serviceDAO.findByName(platform, name);
    }

    /**
     * Find a ServiceType by id
     * 
     */
    public ServiceType findServiceType(Integer id)
        throws ObjectNotFoundException {
        return serviceTypeDAO.findById(id);
    }

    /**
     * Find service type by name
     * 
     */
    public ServiceType findServiceTypeByName(String name) {
        return serviceTypeDAO.findByName(name);
    }

    /**
     * 
     */
    public Collection findDeletedServices() {
        return serviceDAO.findDeletedServices();
    }

    /**
     * @return PageList of ServiceTypeValues
     * 
     */
    public PageList getAllServiceTypes(AuthzSubject subject, PageControl pc) {
        Collection serviceTypes = serviceTypeDAO.findAll();
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**
     * @return List of ServiceTypeValues
     * 
     */
    public PageList getViewableServiceTypes(AuthzSubject subject,
                                            PageControl pc)
        throws FinderException, PermissionException {
        // build the server types from the visible list of servers
        final List authzPks = getViewableServices(subject);
        final Collection serviceTypes =
            serviceDAO.getServiceTypes(authzPks, true);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**
     * 
     */
    public PageList getServiceTypesByServerType(AuthzSubject subject,
                                                int serverTypeId) {
        Collection serviceTypes =
            serviceTypeDAO.findByServerType_orderName(serverTypeId, true);
        if (serviceTypes.size() == 0) {
            return new PageList();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /**
     * 
     */
    public PageList findVirtualServiceTypesByPlatform(AuthzSubject subject,
                                                      Integer platformId) {
        Collection serviceTypes = serviceTypeDAO
                .findVirtualServiceTypesByPlatform(platformId.intValue());
        if (serviceTypes.size() == 0) {
            return new PageList();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /**
     * 
     * @return A List of ServiceValue objects representing all of the
     * services that the given subject is allowed to view.
     */
    public PageList getAllServices(AuthzSubject subject, PageControl pc)
        throws FinderException, PermissionException {
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(getViewableServices(subject, pc), pc);
    }

    /**
     * Get the scope of viewable services for a given user
     * @return List of ServiceLocals for which subject has
     * AuthzConstants.serviceOpViewService
     */
    private Collection getViewableServices(AuthzSubject subject, PageControl pc)
        throws FinderException, PermissionException {
        // get list of pks user can view
        final Collection authzPks = getViewableServices(subject);
        List services;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        switch( pc.getSortattribute() ) {
            case SortAttribute.RESOURCE_NAME:
                services = getServices(authzPks, pc);
                Collections.sort(
                    services, new AppdefNameComparator(pc.isAscending()));
                break;
            case SortAttribute.SERVICE_NAME:
                services = getServices(authzPks, pc);
                Collections.sort(
                    services, new AppdefNameComparator(pc.isAscending()));
                break;
            case SortAttribute.CTIME:
                services = getServices(authzPks, pc);
                Collections.sort(
                    services, new ServiceCtimeComparator(pc.isAscending()));
                break;
            default:
                services = getServices(authzPks, pc);
                break;
        }
        return services;
    }

    /**
     * Note: This method pulls all services from the EHCache one by one.
     * This should be faster than pulling everything from the DB since they
     * are in memory.
     */
    private List getServices(Collection authzPks, PageControl pc) {
        final List aeids = new ArrayList(authzPks);
        final List rtn = new ArrayList(authzPks.size());
        final int start = pc.getPageEntityIndex();
        final int end = (pc.getPagesize() == PageControl.SIZE_UNLIMITED) ?
            authzPks.size() : pc.getPagesize() + start;
        for (int i=start; i<end; i++) {
            final Integer aeid = (Integer)aeids.get(i);
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

    private class ServiceCtimeComparator implements Comparator {
        final boolean _asc;
        ServiceCtimeComparator(boolean ascending) {
            _asc = ascending;
        }
        public int compare(Object arg0, Object arg1) {
            if (!(arg0 instanceof AppdefResource) ||
                !(arg1 instanceof AppdefResource)) {
                    throw new ClassCastException();
            }
            final Long c0 = new Long(((AppdefResource)arg0).getCreationTime());
            final Long c1 = new Long(((AppdefResource)arg1).getCreationTime());
            return (_asc) ? c0.compareTo(c1) : c1.compareTo(c0);
        }
    }

    /**
     * Fetch all services that haven't been assigned to a cluster and that
     * haven't been assigned to any applications.
     * @return A List of ServiceValue objects representing all of the
     * unassigned services that the given subject is allowed to view.
     * 
     */
    public PageList getAllClusterAppUnassignedServices(AuthzSubject subject,
                                                       PageControl pc)
        throws FinderException, PermissionException {
        // get list of pks user can view
        Set authzPks = new HashSet(getViewableServices(subject));
        Collection services = null;
        Collection toBePaged = new ArrayList();
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        switch( pc.getSortattribute() ) {
            case SortAttribute.RESOURCE_NAME:
                if(pc != null) {
                    services = serviceDAO
                        .findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            case SortAttribute.SERVICE_NAME:
                if(pc != null) {
                    services = serviceDAO
                        .findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            default:
                services = serviceDAO
                    .findAllClusterAppUnassigned_orderName(true);
                break;
        }
        for(Iterator i = services.iterator(); i.hasNext();) {
            Service aService = (Service)i.next();
            // remove service if its not viewable
            if(authzPks.contains(aService.getId())) {
                toBePaged.add(aService);
            }
        }
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(toBePaged, pc);
    }

    private PageList filterAndPage(Collection svcCol,
                                   AuthzSubject subject,
                                   Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        List services = new ArrayList();
        // iterate over the services and only include those whose pk is
        // present in the viewablePKs list
        if (svcTypeId != null && svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
            for (Iterator it = svcCol.iterator(); it.hasNext(); ) {
                Object o = it.next();
                Integer thisSvcTypeId;

                if (o instanceof Service) {
                    thisSvcTypeId = ((Service)o).getServiceType().getId();
                } else {
                    ResourceGroup cluster = (ResourceGroup) o;
                    thisSvcTypeId =
                        cluster.getResourcePrototype().getInstanceId();
                }
                // first, if they specified a server type, then filter on it
                if (!(thisSvcTypeId.equals(svcTypeId)))
                    continue;

                services.add(o instanceof Service ? o :
                             getServiceCluster((ResourceGroup) o));
            }
        } else {
            services.addAll(svcCol);
        }

        List toBePaged = filterUnviewable(subject, services);
        return valuePager.seek(toBePaged, pc);
    }
    
    /**
     * @return {@link List} of {@link AppdefEntityID}s that represent the total
     * set of service inventory that the subject is authorized to see. This
     * includes all services as well as all clusters
     */
    protected List getViewableServiceInventory (AuthzSubject whoami)
        throws FinderException, PermissionException
    {
        List idList = getViewableServices(whoami);
        for (int i=0;i<idList.size();i++) {
            Integer pk = (Integer) idList.get(i);
            idList.set(i, AppdefEntityID.newServiceID(pk));
        }
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List viewableGroups = 
            pm.findOperationScopeBySubject(whoami,
                                           AuthzConstants.groupOpViewResourceGroup, 
                                           AuthzConstants.groupResourceTypeName);
        for (int i=0;i<viewableGroups.size();i++) {
            Integer gid = (Integer) viewableGroups.get(i);
            viewableGroups.set(i, AppdefEntityID.newGroupID(gid));
        }
        idList.addAll(viewableGroups);
        return idList;
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
            getOperationByName(resourceManager.findResourceTypeByName(AuthzConstants.serviceResType),
                               AuthzConstants.serviceOpViewService);
        List idList = 
            pm.findOperationScopeBySubject(whoami, op.getId());
        
        return new ArrayList(idList);
    }

    private List filterUnviewable(AuthzSubject subject, Collection services)
        throws PermissionException, ServiceNotFoundException {
        List viewableEntityIds;
        try {
            viewableEntityIds = getViewableServiceInventory(subject);
        } catch (FinderException e) {
            throw new ServiceNotFoundException(
                "no viewable services for " + subject);
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
                Service aService = (Service)o;
                if (viewableEntityIds != null &&
                    viewableEntityIds.contains(aService.getEntityId())) {
                    retVal.add(o);
                }
            }
            else if (o instanceof ResourceGroup) {
                ResourceGroup aCluster = (ResourceGroup)o;
                AppdefEntityID clusterId = AppdefEntityID
                    .newGroupID(aCluster.getId());
                if (viewableEntityIds != null &&
                    viewableEntityIds.contains(clusterId)) {
                    retVal.add(getServiceCluster(aCluster));
                }
            }
        }
        return retVal;
    }

    /**
     * Get services by server and type.
     * 
     */
    public PageList getServicesByServer(AuthzSubject subject, Integer serverId,
                                        PageControl pc)
        throws ServiceNotFoundException, ServerNotFoundException,
               PermissionException {
        return getServicesByServer(subject, serverId, null, pc);
    }

    /**
     * 
     */
    public PageList getServicesByServer(AuthzSubject subject, Integer serverId,
                                        Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        List toBePaged = getServicesByServerImpl(subject, serverId, svcTypeId,
                                                 pc);
        return valuePager.seek(toBePaged, pc);
    }

    private List getServicesByServerImpl(AuthzSubject subject, Integer serverId,
                                         Integer svcTypeId, PageControl pc)
        throws PermissionException, ServiceNotFoundException {
        if (svcTypeId == null)
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;

        List services;

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_TYPE:
            services = serviceDAO.findByServer_orderType(serverId);
            break;
        case SortAttribute.SERVICE_NAME:
        default:
            if (svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
                services = serviceDAO
                        .findByServerAndType_orderName(serverId, svcTypeId);
            }
            else {
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
     * 
     */
    public List getServicesByServer(AuthzSubject subject, Server server)
        throws PermissionException, ServiceNotFoundException {
        return filterUnviewable(subject, server.getServices());
    }

    /**
     * 
     */
    public Integer[] getServiceIdsByServer(AuthzSubject subject,
                                          Integer serverId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException {
        if (svcTypeId == null)
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;

        List services;

        if (svcTypeId == APPDEF_RES_TYPE_UNDEFINED) {
            services = serviceDAO.findByServer_orderType(serverId);
        }
        else {
            services = serviceDAO
                .findByServerAndType_orderName(serverId, svcTypeId);
        }

        // Filter the unviewables
        List viewables = filterUnviewable(subject, services);

        Integer[] ids = new Integer[viewables.size()];
        Iterator it = viewables.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Service local = (Service) it.next();
            ids[i] = local.getId();
        }

        return ids;
    }

    /**
     * 
     */
    public List getServicesByType(AuthzSubject subject, String svcName,
                                  boolean asc)
        throws PermissionException, InvalidAppdefTypeException {
        ServiceType st = serviceTypeDAO.findByName(svcName);
        if (st == null) {
            return new PageList();
        }

        try {
            Collection services = serviceDAO.findByType(st.getId(), asc);
            if (services.size() == 0) {
                return new PageList();
            }
            List toBePaged = filterUnviewable(subject, services);
            return valuePager.seek(toBePaged, PageControl.PAGE_ALL);
        } catch (ServiceNotFoundException e) {
            return new PageList();
        }
    }

    /**
     * 
     */
    public PageList getServicesByService(AuthzSubject subject,
                                         Integer serviceId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
        return getServicesByService(subject, serviceId,
                                         APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get services by server.
     * 
     */
    public PageList getServicesByService(AuthzSubject subject, Integer serviceId,
                                         Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PermissionException {
            // find any children
        Collection childSvcs =
            serviceDAO.findByParentAndType(serviceId, svcTypeId);
        return filterAndPage(childSvcs, subject, svcTypeId, pc);
    }

    /**
     * Get service IDs by service.
     * 
     */
    public Integer[] getServiceIdsByService(AuthzSubject subject,
                                            Integer serviceId,
                                            Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException {
        // find any children
        Collection childSvcs =
            serviceDAO.findByParentAndType(serviceId, svcTypeId);

        List viewables = filterUnviewable(subject, childSvcs);

        Integer[] ids = new Integer[viewables.size()];
        Iterator it = viewables.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Service local = (Service) it.next();
            ids[i] = local.getId();
        }

        return ids;
    }

    /**
     * 
     */
    public PageList getServicesByPlatform(AuthzSubject subject, Integer platId,
                                          PageControl pc)
        throws ServiceNotFoundException, PlatformNotFoundException,
               PermissionException {
        return getServicesByPlatform(subject, platId, APPDEF_RES_TYPE_UNDEFINED,
                                     pc);
    }

    /**
     * Get platform services (children of virtual servers)
     * 
     */
    public PageList getPlatformServices(AuthzSubject subject,
                                        Integer platId,
                                        PageControl pc)
        throws PlatformNotFoundException, PermissionException,
               ServiceNotFoundException {
        return getPlatformServices(subject, platId, APPDEF_RES_TYPE_UNDEFINED,
                                   pc);
    }

    /**
     * Get platform services (children of virtual servers) of a specified type
     *
     * 
     */
    public PageList getPlatformServices(AuthzSubject subject, Integer platId,
                                        Integer typeId, PageControl pc)
        throws PlatformNotFoundException, PermissionException,
               ServiceNotFoundException {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);
        Collection allServices = serviceDAO
            .findPlatformServices_orderName(platId, pc.isAscending());
        return filterAndPage(allServices, subject, typeId, pc);
    }

    /**
     * Get {@link Service}s which are children of the server, and of the
     * specified type.
     * 
     */
    public List findServicesByType(Server server, ServiceType st) {
        return serviceDAO.findByServerAndType_orderName(server.getId(),
                                                             st.getId());
    }

    /**
     * Get platform service POJOs
     * 
     */
    public List findPlatformServicesByType(Platform p, ServiceType st) {
        return serviceDAO.findPlatformServicesByType(p, st);
    }

    /**
     * Get platform service POJOs
     *
     * 
     */
    public Collection getPlatformServices(AuthzSubject subject, Integer platId)
        throws ServiceNotFoundException, PermissionException {
        Collection services =
            serviceDAO.findPlatformServices_orderName(platId, true);
        return filterUnviewable(subject, services);
    }

    /**
     * Get platform services (children of virtual servers), mapped by type id
     * of a specified type
     * 
     */
    public Map getMappedPlatformServices(AuthzSubject subject,
                                         Integer platId,
                                         PageControl pc)
        throws PlatformNotFoundException, PermissionException,
               ServiceNotFoundException
    {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        Collection allServices = serviceDAO
            .findPlatformServices_orderName(platId, pc.isAscending());
        HashMap retMap = new HashMap();

        // Map all services by type ID
        for (Iterator it = allServices.iterator(); it.hasNext(); ) {
            Service svc = (Service) it.next();
            Integer typeId = svc.getServiceType().getId();
            List addTo = (List)MapUtil.getOrCreate(retMap, typeId,
                                                   ArrayList.class);

            addTo.add(svc);
        }

        // Page the lists before returning
        for (Iterator it = retMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Integer typeId = (Integer) entry.getKey();
            List svcs = (List) entry.getValue();

            PageControl pcCheck =
                svcs.size() <= pc.getPagesize() ? PageControl.PAGE_ALL : pc;

            svcs = filterAndPage(svcs, subject, typeId, pcCheck);
            entry.setValue(svcs);
        }

        return retMap;
    }

    /**
     * Get services by platform.
     *
     * 
     */
    public PageList getServicesByPlatform(AuthzSubject subject, Integer platId,
                                          Integer svcTypeId, PageControl pc)
        throws ServiceNotFoundException, PlatformNotFoundException,
               PermissionException
    {
        Collection allServices;
        pc = PageControl.initDefaults(pc,SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME:
            allServices = serviceDAO
                .findByPlatform_orderName(platId, pc.isAscending());
            break;
        case SortAttribute.SERVICE_TYPE:
            allServices = serviceDAO
                .findByPlatform_orderType(platId, pc.isAscending());
            break;
        default:
            throw new IllegalArgumentException("Invalid sort attribute");
        }
        return filterAndPage(allServices, subject, svcTypeId, pc);
    }

    /**
     * 
     * @return A List of ServiceValue and ServiceClusterValue objects
     * representing all of the services that the given subject is allowed to view.
     */
    public PageList getServicesByApplication(AuthzSubject subject,
                                             Integer appId, PageControl pc )
        throws ApplicationNotFoundException, ServiceNotFoundException,
               PermissionException {
        return getServicesByApplication(subject, appId,
                                        APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * 
     * @return A List of ServiceValue and ServiceClusterValue objects
     * representing all of the services that the given subject is allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    public PageList getServicesByApplication(AuthzSubject subject,
                                             Integer appId, Integer svcTypeId,
                                             PageControl pc)
        throws PermissionException, ApplicationNotFoundException,
               ServiceNotFoundException {
        return filterAndPage(getServicesByApplication(appId, pc), subject,
                             svcTypeId, pc);
   }

    /**
     * 
     * @return A List of Service and ServiceCluster objects
     * representing all of the services that the given subject is allowed to view.
     * @throws ApplicationNotFoundException if the appId is bogus
     * @throws ServiceNotFoundException if services could not be looked up
     */
    public List getServicesByApplication(AuthzSubject subject, Integer appId)
        throws PermissionException, ApplicationNotFoundException,
               ServiceNotFoundException {
        return filterUnviewable(subject,
                                getServicesByApplication(appId,
                                                         PageControl.PAGE_ALL));
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

        Collection appServiceCollection;

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME:
        case SortAttribute.RESOURCE_NAME:
            appServiceCollection =
                appServiceDAO.findByApplication_orderSvcName(appId, pc.isAscending());
            break;
        case SortAttribute.SERVICE_TYPE:
            appServiceCollection =
                appServiceDAO.findByApplication_orderSvcType(appId, pc.isAscending());
            break;
        default:
            throw new IllegalArgumentException("Unsupported sort " +
                                               "attribute [" +
                                               pc.getSortattribute() +
                                               "] on PageControl : " + pc);
        }
        AppService appService;
        Iterator i = appServiceCollection.iterator();
        List services = new ArrayList();
        while ( i.hasNext() ) {
            appService = (AppService) i.next();
            if ( appService.isIsGroup() ) {
                services.add(appService.getResourceGroup());
            } else {
                services.add(appService.getService());
            }
        }
        return services;
    }

   /**
    * 
    * @return A List of ServiceValue and ServiceClusterValue objects
    * representing all of the services that the given subject is allowed to view.
    */
   public PageList getServiceInventoryByApplication(AuthzSubject subject,
                                                    Integer appId,
                                                    PageControl pc )
        throws ApplicationNotFoundException, ServiceNotFoundException,
               PermissionException {
        return getServiceInventoryByApplication(subject, appId,
                                                APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get all services by application.  This is to only be used for the
     * Evident API.
     * 
     */
    public PageList
        getFlattenedServicesByApplication(AuthzSubject subject, Integer appId,
                                          Integer typeId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException,
               PermissionException
    {
        if (typeId == null)
            typeId = APPDEF_RES_TYPE_UNDEFINED;

       

        Application appLocal;
        try {
            appLocal = applicationDAO.findById(appId);
        } catch(ObjectNotFoundException e){
            throw new ApplicationNotFoundException(appId, e);
        }

        Collection svcCollection = new ArrayList();
        Collection appSvcCollection = appLocal.getAppServices();
        Iterator it = appSvcCollection.iterator();
        while (it != null && it.hasNext()) {
            AppService appService = (AppService) it.next();

            if (appService.isIsGroup()) {
                svcCollection.addAll(getServiceCluster(
                    appService.getResourceGroup()).getServices());
            } else {
                svcCollection.add(appService.getService());
            }
        }

        return filterAndPage(svcCollection, subject, typeId, pc);
    }

    /**
     * 
     * @return A List of ServiceValue and ServiceClusterValue objects
     * representing all of the services that the given subject is allowed to view.
     */
    public PageList getServiceInventoryByApplication(AuthzSubject subject,
                                                     Integer appId,
                                                     Integer svcTypeId,
                                                     PageControl pc )
        throws ApplicationNotFoundException, ServiceNotFoundException,
               PermissionException {
        if (svcTypeId == null || svcTypeId.equals(APPDEF_RES_TYPE_UNDEFINED)) {
            List services = getUnflattenedServiceInventoryByApplication(
                    subject, appId, pc);
            return filterAndPage(services, subject, APPDEF_RES_TYPE_UNDEFINED,
                                 pc);
        } else {
            return getFlattenedServicesByApplication(subject, appId, svcTypeId,
                                                     pc);
        }
    }

    /**
     * Get all service inventory by application, including those inside an
     * associated cluster
     *
     * 
     *
     * @param subject The subject trying to list services.
     * @param appId Application id.
     * @return A List of ServiceValue objects representing all of the services
     *         that the given subject is allowed to view.
     */
    public Integer[] getFlattenedServiceIdsByApplication(AuthzSubject subject,
                                                         Integer appId)
        throws ServiceNotFoundException, PermissionException,
               ApplicationNotFoundException {

        List serviceInventory =
            getUnflattenedServiceInventoryByApplication(
                subject, appId, PageControl.PAGE_ALL);

        List servicePKs = new ArrayList();
        // flattening: open up all of the groups (if any) and get their services as well
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
                    AppdefEntityID groupId =
                        AppdefEntityID.newGroupID(cluster.getId());
                    // any authz resource filtering on the group members happens
                    // inside the group subsystem
                    try {
                        List memberIds = GroupUtil.getCompatGroupMembers(
                            subject, groupId, null, PageControl.PAGE_ALL);
                        for (Iterator memberIter = memberIds.iterator();
                             memberIter.hasNext(); ) {
                            AppdefEntityID memberEntId =
                                (AppdefEntityID) memberIter.next();
                            servicePKs.add(memberEntId.getId());
                        }
                    } catch (PermissionException e) {
                        // User not allowed to see this group
                        log.debug("User " + subject + " not allowed to view " +
                                  "group " + groupId);
                    }
                }
            }
        } catch (GroupNotCompatibleException e){
            throw new InvalidAppdefTypeException(
                "serviceInventory has groups that are not compatible", e);
        } catch (AppdefEntityNotFoundException e) {
            throw new ServiceNotFoundException("could not return all services",
                                               e);
        }

        return (Integer[]) servicePKs.toArray(
            new Integer[servicePKs.size()]);
    }

    private List getUnflattenedServiceInventoryByApplication(
        AuthzSubject subject, Integer appId, PageControl pc)
        throws ApplicationNotFoundException, ServiceNotFoundException {

        AppServiceDAO appServLocHome;
        List appServiceCollection;

        try {
            applicationDAO.findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }
        // appServiceCollection = appLocal.getAppServices();

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);


        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME :
        case SortAttribute.RESOURCE_NAME :
        case SortAttribute.NAME :
            // TODO: Not actually sorting
            appServiceCollection =
                appServiceDAO.findByApplication_orderName(appId);
            break;
        case SortAttribute.SERVICE_TYPE :
            appServiceCollection =
                appServiceDAO.findByApplication_orderType(appId,
                                                           pc.isAscending());
            break;
        default :
            throw new IllegalArgumentException(
                "Unsupported sort attribute [" + pc.getSortattribute() +
                "] on PageControl : " + pc);
        }

        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are
        // allowed to return.

        AppService appService;
        Iterator i = appServiceCollection.iterator();
        List services = new ArrayList();
        while (i.hasNext()) {
            appService = (AppService) i.next();
            if (appService.isIsGroup()) {
                services.add(appService.getResourceGroup());
            } else {
                services.add(appService.getService());
            }
        }
        return services;
    }

    /**
     * 
     */
    public void updateServiceZombieStatus(AuthzSubject subject, Service svc,
                                          boolean zombieStatus)
        throws PermissionException
    {
        permissionManager.checkModifyPermission(subject, svc.getEntityId());
        svc.setModifiedBy(subject.getName());
        svc.setAutodiscoveryZombie(zombieStatus);
    }

    /**
     * 
     */
    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, UpdateException,
               AppdefDuplicateNameException, ServiceNotFoundException {
        permissionManager.checkModifyPermission(subject, existing.getEntityId());
        Service service = serviceDAO.findById(existing.getId());

        existing.setModifiedBy(subject.getName());
        if (existing.getDescription() != null)
            existing.setDescription(existing.getDescription().trim());
        if (existing.getLocation() != null)
            existing.setLocation(existing.getLocation().trim());
        if (existing.getName() != null)
            existing.setName(existing.getName().trim());

        if(service.matchesValueObject(existing)) {
            log.debug("No changes found between value object and entity");
        } else {
            service.updateService(existing);
        }
        return service;
    }

    /**
     * 
     */
    public void updateServiceTypes(String plugin, ServiceTypeInfo[] infos)
        throws CreateException, FinderException, RemoveException,
               VetoException
    {
        AuthzSubject overlord =
            AuthzSubjectManagerImpl.getOne().getOverlordPojo();

        // First, put all of the infos into a Hash
        HashMap infoMap = new HashMap();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
        }

        HashMap serverTypes = new HashMap();

       
        ResourceGroupManager resGroupMan =
            ResourceGroupManagerImpl.getOne();
        ResourceManager resMan = ResourceManagerImpl.getOne();

        try {
            Collection curServices = serviceTypeDAO.findByPlugin(plugin);
           

            for (Iterator i = curServices.iterator(); i.hasNext();) {
                ServiceType serviceType = (ServiceType) i.next();

                if (log.isDebugEnabled()) {
                    log.debug("Begin updating ServiceTypeLocal: " +
                              serviceType.getName());
                }

                ServiceTypeInfo sinfo =
                    (ServiceTypeInfo) infoMap.remove(serviceType.getName());

                // See if this exists
                if (sinfo == null) {
                    deleteServiceType(serviceType, overlord, resGroupMan,
                                      resMan);
                } else {
                    // Just update it
                    // XXX TODO MOVE THIS INTO THE ENTITY
                    if (!sinfo.getName().equals(serviceType.getName()))
                        serviceType.setName(sinfo.getName());

                    if (!sinfo.getDescription().equals(
                        serviceType.getDescription()))
                        serviceType.setDescription(sinfo.getDescription());

                    if (sinfo.getInternal() !=  serviceType.isIsInternal())
                        serviceType.setIsInternal(sinfo.getInternal());

                    // Could be null if servertype was deleted/updated by plugin
                    ServerType svrtype = serviceType.getServerType();

                    // Check server type
                    if (svrtype == null ||
                        !sinfo.getServerName().equals(svrtype.getName())) {
                        // Lookup the server type
                        if (serverTypes.containsKey(sinfo.getServerName()))
                            svrtype = (ServerType)
                                serverTypes.get(sinfo.getServerName());
                        else {
                            svrtype = serverTypeDAO.findByName(sinfo.getServerName());
                            if (svrtype == null) {
                                throw new FinderException(
                                    "Unable to find server " +
                                    sinfo.getServerName() +
                                    " on which service '" +
                                    serviceType.getName() +
                                    "' relies");
                            }
                            serverTypes.put(svrtype.getName(), svrtype);
                        }
                        serviceType.setServerType(svrtype);
                    }
                }
            }

            Resource prototype =
                ResourceManagerImpl.getOne().findRootResource();

            // Now create the left-overs
            for (Iterator i = infoMap.values().iterator(); i.hasNext();) {
                ServiceTypeInfo sinfo = (ServiceTypeInfo) i.next();

                ServiceType stype = serviceTypeDAO.create(sinfo.getName(), plugin,
                                                   sinfo.getDescription(),
                                                   sinfo.getInternal());

                // Lookup the server type
                ServerType servTypeEJB;
                if (serverTypes.containsKey(sinfo.getServerName())) {
                    servTypeEJB = (ServerType)
                        serverTypes.get(sinfo.getServerName());
                } else {
                    servTypeEJB = serverTypeDAO.findByName(sinfo.getServerName());
                    serverTypes.put(servTypeEJB.getName(), servTypeEJB);
                }
                stype.setServerType(servTypeEJB);
                resourceManager.createResource(overlord, resourceManager.findResourceTypeByName(AuthzConstants.servicePrototypeTypeName),
                                    prototype, stype.getId(), stype.getName(),false,
                                    null);
            }
        } finally {
            serviceTypeDAO.getSession().flush();
        }
    }

    /**
     * 
     */
    public void deleteServiceType(ServiceType serviceType,
                                  AuthzSubject overlord,
                                  ResourceGroupManager resGroupMan,
                                  ResourceManager resMan)
        throws VetoException, RemoveException {
        Resource proto =
            resMan.findResourceByInstanceId(AuthzConstants.authzServiceProto,
                                            serviceType.getId());

        try {
            // Delete compatible groups of this type.
            resGroupMan.removeGroupsCompatibleWith(proto);

            // Remove all services
            for (Iterator svcIt = serviceType.getServices().iterator();
                 svcIt.hasNext(); ) {
                Service svcLocal = (Service) svcIt.next();
                removeService(overlord, svcLocal);
            }
        } catch (PermissionException e) {
            assert false :
                "Overlord should not run into PermissionException";
        }

       
        serviceTypeDAO.remove(serviceType);

        resMan.removeResource(overlord, proto);
    }
    
    /**
     * Map a ResourceGroup to ServiceCluster, just temporary, should be able to
     * remove when done with the ServiceCluster to ResourceGroup Migration
     */
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
     * A removeService method that takes a ServiceLocal.  This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     * 
     */
    public void removeService(AuthzSubject subject, Service service)
        throws RemoveException, PermissionException, VetoException
    {
        AppdefEntityID aeid = service.getEntityId();
        permissionManager.checkRemovePermission(subject, aeid);

        // Remove service from parent Server's Services collection
        Server server = service.getServer();
        if (server != null) {
            server.getServices().remove(service);
        }

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
    protected Operation getOperationByName(ResourceType rtV, String opName) throws PermissionException {
        Collection<Operation> ops = rtV.getOperations();
        for (Operation op : ops) {

            if (op.getName().equals(opName)) {
                return op;
            }
        }
        throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " + rtV.getName());
    }

    /**
     * 
     */
    public void handleResourceDelete(Resource resource) {
        serviceDAO.clearResource(resource);
    }

    /**
     * Returns a list of 2 element arrays.  The first element is the name of
     * the service type, the second element is the # of services of that
     * type in the inventory.
     *
     * 
     */
    public List getServiceTypeCounts() {
        return serviceDAO.getServiceTypeCounts();
    }

    /**
     * Get the # of services within HQ inventory
     * 
     */
    public Number getServiceCount() {
        return serviceDAO.getServiceCount();
    }


    public static ServiceManager getOne() {
        return Bootstrap.getBean(ServiceManager.class);
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
       
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        
    }

}
