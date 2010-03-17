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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.MapUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.hyperic.dao.DAOFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 * @ejb:bean name="ServiceManager"
 *      jndi-name="ejb/appdef/ServiceManager"
 *      local-jndi-name="LocalServiceManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="Required"
 */
public class ServiceManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {

    private Log log = LogFactory.getLog(ServiceManagerEJBImpl.class);

    private final String VALUE_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_service";
    private Pager valuePager = null;
    private final Integer APPDEF_RES_TYPE_UNDEFINED = new Integer(-1);
    
    /**
     * @ejb:interface-method
     */
    public Service createService(AuthzSubject subject, Server server,
                                 ServiceType type, String name,  String desc,
                                 String location, Service parent) 
        throws PermissionException
    {
        name     = name.trim();
        desc     = desc == null ? "" : desc.trim();
        location = location == null ? "" : location.trim();
        
        Service service = getServiceDAO().create(type, server, name, desc, 
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
     * @ejb:interface-method
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
     * @ejb:interface-method
     */
    public void moveService(AuthzSubject subject, Service target,
                            Server destination)
        throws VetoException, PermissionException
    {
        try {
            // Permission checking on destination
            if (destination.getServerType().isVirtual()) {
                checkPermission(subject, getPlatformResourceType(),
                                destination.getPlatform().getId(),
                                AuthzConstants.platformOpAddServer);
            } else {
                checkPermission(subject, getServerResourceType(),
                                destination.getId(),
                                AuthzConstants.serverOpAddService);
            }

            // Permission check on target
            checkPermission(subject, getServiceResourceType(),
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
        MeasurementManagerEJBImpl.getOne().disableMeasurements(subject,
                                                               target.getResource());

        // Reset Service parent id
        target.setServer(destination);

        // Add/Remove Server from Server collections
        target.getServer().getServices().remove(target);
        destination.addService(target);

        // Move Authz resource.
        getResourceManager().moveResource(subject, target.getResource(),
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
     * @ejb:interface-method
     */
    public Service createService(AuthzSubject subject, Integer serverId,
                                 Integer serviceTypeId, String name,
                                 String desc, String location)
        throws CreateException, ValidationException, PermissionException,
               ServerNotFoundException, AppdefDuplicateNameException
    {
        Server server = getServerDAO().findById(serverId);
        ServiceType serviceType = getServiceTypeDAO().findById(serviceTypeId);
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
                checkPermission(subject, getPlatformResourceType(),
                                server.getPlatform().getId(),
                                AuthzConstants.platformOpAddServer);
            } else {
                // to the platform in question
                checkPermission(subject, getServerResourceType(),
                                server.getId(),
                                AuthzConstants.serverOpAddService);
            }

            ResourceManagerLocal rman = getResourceManager();
            ResourceType serviceProto = getServicePrototypeResourceType();
            Resource prototype = 
                rman.findResourceByInstanceId(serviceProto,
                                              service.getServiceType().getId());
        
            Resource parent = rman.findResource(server.getEntityId());
            if (parent == null) {
                throw new SystemException("Unable to find parent server [id=" +
                                          server.getEntityId() + "]");
            }
            Resource resource = createAuthzResource(subject,
                                                    getServiceResourceType(),
                                                    prototype, 
                                                    service.getId(),
                                                    service.getName(), parent);
            service.setResource(resource);
        } catch(FinderException e) {
            throw new SystemException("Unable to find authz resource type", e);
        }
    }

    /**
     * Get service IDs by service type.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    public Integer[] getServiceIds(AuthzSubject subject, Integer servTypeId)
        throws PermissionException {
        ServiceDAO sLHome;
        try {
            sLHome = getServiceDAO();
            Collection services = sLHome.findByType(servTypeId, true);
            if (services.size() == 0) {
                return new Integer[0];
            }
            List serviceIds = new ArrayList(services.size());
         
            // now get the list of PKs
            Set viewable = new HashSet(super.getViewableServices(subject));
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
     * @ejb:interface-method
     */
    public List findServicesById(AuthzSubject subject, Integer[] serviceIds) 
        throws ServiceNotFoundException, PermissionException {
        List serviceList = new ArrayList(serviceIds.length);
        for(int i = 0; i < serviceIds.length; i++) {
            Service s = findServiceById(serviceIds[i]);
            checkViewPermission(subject, s.getEntityId());
            serviceList.add(s.getServiceValue());
        }
        return serviceList;
    }

    /**
     * Find Service by Id.
     * @ejb:interface-method
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
     * @ejb:interface-method
     * @return The Service identified by this id, or null if it does not exist.
     */
    public Service getServiceById(Integer id) {
        return getServiceDAO().get(id);
    }

    /**
     * Get Service by Id and perform permission check.
     * 
     * @ejb:interface-method
     * @return The Service identified by this id.
     */
    public Service getServiceById(AuthzSubject subject, Integer id)
        throws ServiceNotFoundException, PermissionException {
        
        Service service = findServiceById(id);
        checkViewPermission(subject, service.getEntityId());
        return service;
    }
    
    /**
     * @param server {@link Server}
     * @param aiid service autoinventory identifier
     * @return {@link List} of {@link Service}
     * @ejb:interface-method
     */
    public List getServicesByAIID(Server server, String aiid) {
        return getServiceDAO().getByAIID(server, aiid);
    }

    /**
     * @param server {@link Server}
     * @param name corresponds to the EAM_RESOURCE.sort_name column
     * @ejb:interface-method
     */
    public Service getServiceByName(Server server, String name) {
        return getServiceDAO().findByName(server, name);
    }
    
    /**
     * @ejb:interface-method
     */
    public Service getServiceByName(Platform platform, String name) {
        return getServiceDAO().findByName(platform, name);
    }
    
    /**
     * Find a ServiceType by id
     * @ejb:interface-method
     */
    public ServiceType findServiceType(Integer id)
        throws ObjectNotFoundException {
        return getServiceTypeDAO().findById(id); 
    }
    
    /**
     * Find service type by name
     * @ejb:interface-method
     */
    public ServiceType findServiceTypeByName(String name) { 
        return getServiceTypeDAO().findByName(name);
    }
    
    /**
     * @ejb.interface-method
     */
    public Collection findDeletedServices() {
        return getServiceDAO().findDeletedServices();
    }

    /**
     * @return PageList of ServiceTypeValues
     * @ejb:interface-method
     */
    public PageList getAllServiceTypes(AuthzSubject subject, PageControl pc) {
        Collection serviceTypes = getServiceTypeDAO().findAll();
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**     
     * @return List of ServiceTypeValues
     * @ejb:interface-method
     */
    public PageList getViewableServiceTypes(AuthzSubject subject,
                                            PageControl pc)
        throws FinderException, PermissionException {
        // build the server types from the visible list of servers
        final List authzPks = getViewableServices(subject);
        final Collection serviceTypes =
            getServiceDAO().getServiceTypes(authzPks, true);
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**
     * @ejb:interface-method
     */
    public PageList getServiceTypesByServerType(AuthzSubject subject,
                                                int serverTypeId) {
        Collection serviceTypes =
            getServiceTypeDAO().findByServerType_orderName(serverTypeId, true);
        if (serviceTypes.size() == 0) {
            return new PageList();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /**
     * @ejb:interface-method
     */
    public PageList findVirtualServiceTypesByPlatform(AuthzSubject subject,
                                                      Integer platformId) {
        Collection serviceTypes = getServiceTypeDAO()
                .findVirtualServiceTypesByPlatform(platformId.intValue());
        if (serviceTypes.size() == 0) {
            return new PageList();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /**
     * @ejb:interface-method
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
     * @ejb:interface-method
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
                    services = getServiceDAO()
                        .findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            case SortAttribute.SERVICE_NAME:
                if(pc != null) {
                    services = getServiceDAO()
                        .findAllClusterAppUnassigned_orderName(pc.isAscending());
                }
                break;
            default:
                services = getServiceDAO()
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
     * @ejb:interface-method
     */
    public PageList getServicesByServer(AuthzSubject subject, Integer serverId,
                                        PageControl pc) 
        throws ServiceNotFoundException, ServerNotFoundException, 
               PermissionException {
        return getServicesByServer(subject, serverId, null, pc);
    }

    /**
     * @ejb:interface-method
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
            services = getServiceDAO().findByServer_orderType(serverId);
            break;
        case SortAttribute.SERVICE_NAME:
        default:
            if (svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
                services = getServiceDAO()
                        .findByServerAndType_orderName(serverId, svcTypeId);
            }
            else {
                services = getServiceDAO().findByServer_orderName(serverId);
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
     * @ejb:interface-method
     */
    public List getServicesByServer(AuthzSubject subject, Server server) 
        throws PermissionException, ServiceNotFoundException {
        return filterUnviewable(subject, server.getServices());
    }

    /**
     * @ejb:interface-method
     */
    public Integer[] getServiceIdsByServer(AuthzSubject subject,
                                          Integer serverId, Integer svcTypeId) 
        throws ServiceNotFoundException, PermissionException {
        if (svcTypeId == null)
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;
 
        List services;
            
        if (svcTypeId == APPDEF_RES_TYPE_UNDEFINED) {
            services = getServiceDAO().findByServer_orderType(serverId);
        }
        else {
            services = getServiceDAO()
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
     * @ejb:interface-method
     */
    public List getServicesByType(AuthzSubject subject, String svcName,
                                  boolean asc) 
        throws PermissionException, InvalidAppdefTypeException {
        ServiceType st = getServiceTypeDAO().findByName(svcName);
        if (st == null) {
            return new PageList();
        }
    
        try {
            Collection services = getServiceDAO().findByType(st.getId(), asc);
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
     * @ejb:interface-method
     */
    public PageList getServicesByService(AuthzSubject subject,
                                         Integer serviceId, PageControl pc) 
        throws ServiceNotFoundException, PermissionException {
        return getServicesByService(subject, serviceId,
                                         APPDEF_RES_TYPE_UNDEFINED, pc);
    }
    
    /**
     * Get services by server.
     * @ejb:interface-method
     */
    public PageList getServicesByService(AuthzSubject subject, Integer serviceId,
                                         Integer svcTypeId, PageControl pc) 
        throws ServiceNotFoundException, PermissionException {
            // find any children
        Collection childSvcs =
            getServiceDAO().findByParentAndType(serviceId, svcTypeId);
        return filterAndPage(childSvcs, subject, svcTypeId, pc);
    }

    /**
     * Get service IDs by service.
     * @ejb:interface-method
     */
    public Integer[] getServiceIdsByService(AuthzSubject subject,
                                            Integer serviceId,
                                            Integer svcTypeId) 
        throws ServiceNotFoundException, PermissionException {
        // find any children
        Collection childSvcs =
            getServiceDAO().findByParentAndType(serviceId, svcTypeId);
        
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
     * @ejb:interface-method
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
     * @ejb:interface-method
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
     * @ejb:interface-method
     */
    public PageList getPlatformServices(AuthzSubject subject, Integer platId,
                                        Integer typeId, PageControl pc)
        throws PlatformNotFoundException, PermissionException, 
               ServiceNotFoundException {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);
        Collection allServices = getServiceDAO()
            .findPlatformServices_orderName(platId, pc.isAscending());
        return filterAndPage(allServices, subject, typeId, pc);
    }
    
    /**
     * Get {@link Service}s which are children of the server, and of the
     * specified type.
     * @ejb:interface-method
     */
    public List findServicesByType(Server server, ServiceType st) { 
        return getServiceDAO().findByServerAndType_orderName(server.getId(),
                                                             st.getId());
    }
    
    /**
     * Get platform service POJOs
     * @ejb:interface-method
     */
    public List findPlatformServicesByType(Platform p, ServiceType st) {
        return getServiceDAO().findPlatformServicesByType(p, st);
    }

    /**
     * Get platform service POJOs
     * 
     * @ejb:interface-method
     */
    public Collection getPlatformServices(AuthzSubject subject, Integer platId)
        throws ServiceNotFoundException, PermissionException {
        Collection services =
            getServiceDAO().findPlatformServices_orderName(platId, true);
        return filterUnviewable(subject, services);
    }
    
    /**
     * Get platform services (children of virtual servers), mapped by type id
     * of a specified type
     * @ejb:interface-method
     */
    public Map getMappedPlatformServices(AuthzSubject subject,
                                         Integer platId, 
                                         PageControl pc)
        throws PlatformNotFoundException, PermissionException, 
               ServiceNotFoundException
    {
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);
            
        Collection allServices = getServiceDAO()
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
     * @ejb:interface-method
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
            allServices = getServiceDAO()
                .findByPlatform_orderName(platId, pc.isAscending());
            break;
        case SortAttribute.SERVICE_TYPE:
            allServices = getServiceDAO()
                .findByPlatform_orderType(platId, pc.isAscending());
            break;
        default:
            throw new IllegalArgumentException("Invalid sort attribute");
        }
        return filterAndPage(allServices, subject, svcTypeId, pc);
    }

    /**
     * @ejb:interface-method
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
     * @ejb:interface-method
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
     * @ejb:interface-method
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
            getApplicationDAO().findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }

        Collection appServiceCollection;
        AppServiceDAO dao = new AppServiceDAO(DAOFactory.getDAOFactory());
        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME:
        case SortAttribute.RESOURCE_NAME:
            appServiceCollection =
                dao.findByApplication_orderSvcName(appId, pc.isAscending());
            break;
        case SortAttribute.SERVICE_TYPE:
            appServiceCollection =
                dao.findByApplication_orderSvcType(appId, pc.isAscending());
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
    * @ejb:interface-method
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
     * @ejb:interface-method
     */
    public PageList 
        getFlattenedServicesByApplication(AuthzSubject subject, Integer appId,
                                          Integer typeId, PageControl pc) 
        throws ApplicationNotFoundException, ServiceNotFoundException,
               PermissionException
    {
        if (typeId == null)
            typeId = APPDEF_RES_TYPE_UNDEFINED;

        ApplicationDAO appLocalHome = getApplicationDAO();

        Application appLocal;
        try {
            appLocal = appLocalHome.findById(appId);
        } catch(ObjectNotFoundException e){
            throw new ApplicationNotFoundException(appId, e);
        }

        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        Collection svcCollection = new ArrayList();
        if (debug) watch.markTimeBegin("getAppServices");
        Collection appSvcCollection = appLocal.getAppServices();
        if (debug) watch.markTimeEnd("getAppServices");
        Iterator it = appSvcCollection.iterator();
        while (it != null && it.hasNext()) {
            AppService appService = (AppService) it.next();

            if (appService.isIsGroup()) {
                if (debug) watch.markTimeBegin("getServiceCluster");
                ServiceCluster cluster =
                    getServiceCluster(appService.getResourceGroup());
                if (debug) watch.markTimeEnd("getServiceCluster");
                if (debug) watch.markTimeBegin("getServices");
                svcCollection.addAll(cluster.getServices());
                if (debug) watch.markTimeEnd("getServices");
            } else {
                svcCollection.add(appService.getService());
            } 
        }

        if (debug) log.debug(watch);
        return filterAndPage(svcCollection, subject, typeId, pc);
    }

    /**
     * @ejb:interface-method
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
     * @ejb:interface-method
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

        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        
        if (debug) watch.markTimeBegin("getUnflattenedServiceInventoryByApplication");
        List serviceInventory = getUnflattenedServiceInventoryByApplication(
            subject, appId, PageControl.PAGE_ALL);
        if (debug) watch.markTimeEnd("getUnflattenedServiceInventoryByApplication");
        
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
                        if (debug) watch.markTimeBegin("getCompatGroupMembers");
                        List memberIds = GroupUtil.getCompatGroupMembers(
                            subject, groupId, null, PageControl.PAGE_ALL);
                        if (debug) watch.markTimeEnd("getCompatGroupMembers");
                        for (Iterator it=memberIds.iterator(); it.hasNext(); ) {
                            AppdefEntityID memberEntId =
                                (AppdefEntityID) it.next();
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
        if (debug) log.debug(watch);
        return (Integer[]) servicePKs.toArray(new Integer[servicePKs.size()]);
    }

    private List getUnflattenedServiceInventoryByApplication(
        AuthzSubject subject, Integer appId, PageControl pc)
    throws ApplicationNotFoundException, ServiceNotFoundException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();

        try {
            getApplicationDAO().findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }
        // appServiceCollection = appLocal.getAppServices();

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        AppServiceDAO appServDAO = new AppServiceDAO(DAOFactory.getDAOFactory());
        Collection appServices = null;
        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME :
        case SortAttribute.RESOURCE_NAME :
        case SortAttribute.NAME :
            // TODO: Not actually sorting
            if (debug) watch.markTimeBegin("findByApplication_orderName");
            appServices = appServDAO.findByApplication_orderName(appId);
            if (debug) watch.markTimeEnd("findByApplication_orderName");
            break;
        case SortAttribute.SERVICE_TYPE :
            if (debug) watch.markTimeBegin("findByApplication_orderType");
            appServices = appServDAO.findByApplication_orderType(
                appId, pc.isAscending());
            if (debug) watch.markTimeEnd("findByApplication_orderType");
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

        List services = new ArrayList(appServices.size());
        for (Iterator it=appServices.iterator(); it.hasNext(); ) {
            AppService appService = (AppService) it.next();
            if (appService.isIsGroup()) {
                if (debug) watch.markTimeBegin("appService.getResourceGroup");
                services.add(appService.getResourceGroup());
                if (debug) watch.markTimeEnd("appService.getResourceGroup");
            } else {
                if (debug) watch.markTimeBegin("appService.getService");
                services.add(appService.getService());
                if (debug) watch.markTimeEnd("appService.getService");
            }
        }
        if (debug) log.debug(watch);
        return services;
    }

    /**
     * @ejb:interface-method
     */
    public void updateServiceZombieStatus(AuthzSubject subject, Service svc,
                                          boolean zombieStatus)
        throws PermissionException
    {
        checkModifyPermission(subject, svc.getEntityId());
        svc.setModifiedBy(subject.getName());
        svc.setAutodiscoveryZombie(zombieStatus);
    }
    
    /**
     * @ejb:interface-method
     */
    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, UpdateException, 
               AppdefDuplicateNameException, ServiceNotFoundException {
        checkModifyPermission(subject, existing.getEntityId());
        Service service = getServiceDAO().findById(existing.getId());
        
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
     * @ejb:interface-method
     */
    public void updateServiceTypes(String plugin, ServiceTypeInfo[] infos)
        throws CreateException, FinderException, RemoveException,
               VetoException
    {
        AuthzSubject overlord = 
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        
        // First, put all of the infos into a Hash
        HashMap infoMap = new HashMap();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
        }

        HashMap serverTypes = new HashMap();

        ServiceTypeDAO stDao = getServiceTypeDAO();
        ResourceGroupManagerLocal resGroupMan = 
            ResourceGroupManagerEJBImpl.getOne();
        ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();
        
        try {
            Collection curServices = stDao.findByPlugin(plugin);
            ServerTypeDAO stHome = getServerTypeDAO();
            
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
                            svrtype = stHome.findByName(sinfo.getServerName());
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
                ResourceManagerEJBImpl.getOne().findRootResource();
            
            // Now create the left-overs
            for (Iterator i = infoMap.values().iterator(); i.hasNext();) {
                ServiceTypeInfo sinfo = (ServiceTypeInfo) i.next();

                ServiceType stype = stDao.create(sinfo.getName(), plugin, 
                                                   sinfo.getDescription(),
                                                   sinfo.getInternal());
                
                // Lookup the server type
                ServerType servTypeEJB;
                if (serverTypes.containsKey(sinfo.getServerName())) {
                    servTypeEJB = (ServerType)
                        serverTypes.get(sinfo.getServerName());
                } else {
                    servTypeEJB = stHome.findByName(sinfo.getServerName());
                    serverTypes.put(servTypeEJB.getName(), servTypeEJB);
                }
                stype.setServerType(servTypeEJB);
                createAuthzResource(overlord, getServicePrototypeResourceType(),
                                    prototype, stype.getId(), stype.getName(),
                                    null); 
            }
        } finally {
            stDao.getSession().flush();
        }
    }

    /**
     * @ejb:interface-method
     */
    public void deleteServiceType(ServiceType serviceType,
                                  AuthzSubject overlord,
                                  ResourceGroupManagerLocal resGroupMan,
                                  ResourceManagerLocal resMan)
        throws VetoException, RemoveException {
        Resource proto = 
            resMan.findResourceByInstanceId(AuthzConstants.authzServiceProto,
                                            serviceType.getId());
        
        try {
            // Delete compatible groups of this type.
            resGroupMan.removeGroupsCompatibleWith(proto);
        
            // Remove all services
            Service[] services = (Service[])
                    serviceType.getServices().toArray(new Service[serviceType.getServices().size()]);
            for (int i = 0; i < services.length; i++) {
                removeService(overlord, services[i]);
            }
        } catch (PermissionException e) {
            assert false :
                "Overlord should not run into PermissionException";
        }

        ServiceTypeDAO dao = new ServiceTypeDAO(DAOFactory.getDAOFactory());
        dao.remove(serviceType);
        
        resMan.removeResource(overlord, proto);
    }

    /**
     * A removeService method that takes a ServiceLocal.  This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     * @ejb:interface-method
     */
    public void removeService(AuthzSubject subject, Service service)
        throws RemoveException, PermissionException, VetoException  
    {
        AppdefEntityID aeid = service.getEntityId();
        checkRemovePermission(subject, aeid);

        // Remove service from parent Server's Services collection
        Server server = service.getServer();
        if (server != null) {
            server.getServices().remove(service);
        }

        // Remove from ServiceType collection
        service.getServiceType().getServices().remove(service);

        final ConfigResponseDB config = service.getConfigResponse();

        // remove from appdef
        final ServiceDAO dao = getServiceDAO();
        dao.remove(service);

        // remove the config response
        if (config != null) {
            getConfigResponseDAO().remove(config);
        }

        // remove custom properties
        deleteCustomProperties(aeid);
        
        // Remove authz resource.
        removeAuthzResource(subject, aeid, service.getResource());

        dao.getSession().flush();
    }

    /**
     * @ejb:interface-method
     */
    public void handleResourceDelete(Resource resource) {
        getServiceDAO().clearResource(resource);
    }

    /**
     * Returns a list of 2 element arrays.  The first element is the name of
     * the service type, the second element is the # of services of that
     * type in the inventory.
     * 
     * @ejb:interface-method
     */
    public List getServiceTypeCounts() {
        return getServiceDAO().getServiceTypeCounts(); 
    }
    
    /**
     * Get the # of services within HQ inventory
     * @ejb:interface-method
     */
    public Number getServiceCount() {
        return getServiceDAO().getServiceCount();
    }
    

    public static ServiceManagerLocal getOne() {
        try {
            return ServiceManagerUtil.getLocalHome().create();    
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
