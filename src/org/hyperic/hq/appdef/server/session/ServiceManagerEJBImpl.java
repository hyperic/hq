/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilter;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceValue;
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
import org.hyperic.hq.dao.ServiceDAO;
import org.hyperic.hq.dao.AppServiceDAO;
import org.hyperic.hq.dao.ConfigResponseDAO;
import org.hyperic.dao.DAOFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 * @ejb:bean name="ServiceManager"
 *      jndi-name="ejb/appdef/ServiceManager"
 *      local-jndi-name="LocalServiceManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ServiceManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {

    private Log log = LogFactory.getLog(ServiceManagerEJBImpl.class);

    private final String VALUE_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_service";
    private Pager valuePager = null;
    private final Integer APPDEF_RES_TYPE_UNDEFINED = new Integer(-1);
    private final int APPDEF_TYPE_GROUP_COMPAT_SVC =
        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;

    
    /**
     * @ejb:interface-method
     */
    public Service createService(AuthzSubject subject, Server server,
                                 ServiceType type, String name, 
                                 String desc, String location, Service parent) 
        throws PermissionException
    {
        name     = name.trim();
        desc     = desc == null ? "" : desc.trim();
        location = location == null ? "" : location.trim();
        
        Service service = getServiceDAO().create(type, server, name, desc, 
                                                 subject.getName(), location, 
                                                 subject.getName(), parent);
                   
        // Create the authz resource type.  This also does permission checking
        if (server.getServerType().isVirtual()) {
            createAuthzService(name, service.getId(), 
                               server.getPlatform().getId(), false, subject, 
                               type, server); 
        } else {
            createAuthzService(name, service.getId(), server.getId(),
                               true, subject, type, server);
        }

        ResourceCreatedZevent zevent =
            new ResourceCreatedZevent(subject.getAuthzSubjectValue(),
                                      service.getEntityId());
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
        return service;
    }

    /**
     * Create a Service which runs on a given server
     * @return ServiceValue - the saved value object
     * @exception CreateException - if it fails to add the service
     * @ejb:interface-method
     */
    public Integer createService(AuthzSubject subject, Integer serverId,
                                 Integer serviceTypeId, ServiceValue sValue)
        throws CreateException, ValidationException, PermissionException,
               ServerNotFoundException, AppdefDuplicateNameException
    {
        try {
            validateNewService(sValue);
            trimStrings(sValue);

            Server server = getServerMgrLocal().findServerById(serverId);
            ServiceType serviceType =
                getServiceTypeDAO().findById(serviceTypeId);
            sValue.setServiceType(serviceType.getServiceTypeValue());
            sValue.setOwner(subject.getName());
            sValue.setModifiedBy(subject.getName());
            Service service = getServiceDAO().createService(server, sValue);
            
            if (server.getServerType().isVirtual()) {
                // Look for the platform authorization
                createAuthzService(sValue.getName(), service.getId(),
                                   server.getPlatform().getId(), false,
                                   subject, serviceType, server);
            } else {
                // now add the authz resource
                createAuthzService(sValue.getName(), service.getId(), serverId,
                                   true, subject, serviceType, server);
            }

            // Add Service to parent collection
            Collection services = server.getServices();
            if (!services.contains(service)) {
                services.add(service);
            }

            // Send resource create event
            ResourceCreatedZevent zevent =
                new ResourceCreatedZevent(subject.getAuthzSubjectValue(),
                                          service.getEntityId());
            ZeventManager.getInstance().enqueueEventAfterCommit(zevent);

            return service.getId();
        } catch (PermissionException e) {
            // make sure that if there is a permission exception during
            // service creation, rollback the whole service creation process;
            // otherwise, there would be a EAM_SERVICE record without its
            // cooresponding EAM_RESOURCE record
            log.error("User: " + subject.getName() +
                      " can not add services to server: " + serverId);
            throw e;
        }
    }

    /**
     * Create the Authz service resource
     */
    private void createAuthzService(String serviceName, Integer serviceId,
                                    Integer parentId, boolean isServer,
                                    AuthzSubject subject, ServiceType st,
                                    Server server)
        throws PermissionException 
    {
        log.debug("Begin Authz CreateService");
        try {
            // check to see that the user has permission to addServices
            if (isServer) {
                // to the server in question
                checkPermission(subject, getServerResourceType(), parentId,
                                AuthzConstants.serverOpAddService);
            } else {
                // to the platform in question
                checkPermission(subject, getPlatformResourceType(), parentId,
                                AuthzConstants.platformOpAddServer);
            }

            ResourceManagerLocal rman = getResourceManager();
            ResourceType serviceProto = getServicePrototypeResourceType();
            Resource prototype = 
                rman.findResourcePojoByInstanceId(serviceProto, st.getId());
        
            Resource parent = rman.findResource(server.getEntityId());
            if (parent == null) {
                throw new SystemException("Unable to find parent server [id=" +
                                          server.getEntityId() + "]");
            }
            createAuthzResource(subject, getServiceResourceType(), prototype, 
                                serviceId, serviceName, parent);
        } catch(FinderException e) {
            throw new SystemException("Unable to find authz resource type", e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public ServiceValue[] findServicesByName(AuthzSubject subject, String name)
        throws ServiceNotFoundException, PermissionException
    {
        List serviceLocals = getServiceDAO().findByName(name);

        int numServices = serviceLocals.size();
        
        if (numServices == 0)
            throw new ServiceNotFoundException("Service: " + name +
                                               " not found");

        List services = new ArrayList();
        for (int i = 0; i < numServices; i++) {
            Service sLocal = (Service)serviceLocals.get(i);
            try {
                checkViewPermission(subject, sLocal.getEntityId());
                services.add(sLocal.getServiceValue());
            } catch (PermissionException e) {
                //Ok, won't be added to the list
            }
        }
        return (ServiceValue[])services.toArray(new ServiceValue[0]);
    }

    /**
     * Get service IDs by service type.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    public Integer[] getServiceIds(AuthzSubject subject,
                                  Integer servTypeId)
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
            Collection viewable = super.getViewableServices(subject);
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
    public PageList findServicesById(AuthzSubject subject,
                                     Integer[] serviceIds, 
                                     PageControl pc) 
        throws ServiceNotFoundException, PermissionException {
        // TODO paging... Not sure if its even needed.
        PageList serviceList = new PageList();
        for(int i = 0; i < serviceIds.length; i++) {
            serviceList.add(getServiceById(subject, serviceIds[i]));
        }
        serviceList.setTotalSize(serviceIds.length);
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
     * @ejb:interface-method
     */
    public Service getServiceByName(Server server, String name) {
        return getServiceDAO().findByName(server, name);
    }
    
    
    /**
     * Find ServiceTypeValue by Id.
     * @deprecated Use findServiceType instead.
     * @ejb:interface-method
     */
    public ServiceTypeValue findServiceTypeById(Integer id) 
        throws ObjectNotFoundException {
        return findServiceType(id).getServiceTypeValue();
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
    public ServiceTypeValue findServiceTypeByName(String name) 
        throws FinderException {

        ServiceType st = getServiceTypeDAO().findByName(name);
        if (st == null) {
            throw new FinderException("service type not found: "+ name);
        }
        
        return st.getServiceTypeValue();
    }

    /**
     * Find service type by name
     * @ejb:interface-method
     */
    public ServiceType findPojoServiceTypeByName(String name) { 
        return getServiceTypeDAO().findByName(name);
    }

    /**     
     * @return PageList of ServiceTypeValues
     * @ejb:interface-method
     */
    public PageList getAllServiceTypes(AuthzSubjectValue subject,
                                       PageControl pc)
    {
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
        Collection services;
        try {
            services = getViewableServices(subject, pc);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        Collection serviceTypes = filterResourceTypes(services);
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serviceTypes, pc);
    }

    /**
     * @ejb:interface-method
     */
    public PageList getServiceTypesByServerType(AuthzSubjectValue subject,
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
    public PageList findVirtualServiceTypesByPlatform(AuthzSubjectValue subject,
                                                      Integer platformId) {
        Collection serviceTypes = getServiceTypeDAO()
                .findVirtualServiceTypesByPlatform(platformId.intValue());
        if (serviceTypes.size() == 0) {
            return new PageList();
        }
        return valuePager.seek(serviceTypes, PageControl.PAGE_ALL);
    }

    /** 
     * @deprecated
     * @ejb:interface-method
     */
    public ServiceValue getServiceById(AuthzSubject subject, Integer id)
        throws ServiceNotFoundException, PermissionException {

        Service s = findServiceById(id);
        checkViewPermission(subject, s.getEntityId());
        return s.getServiceValue();
    }

    /**
     * @ejb:interface-method
     * @return A List of ServiceValue objects representing all of the
     * services that the given subject is allowed to view.
     */
    public PageList getAllServices(AuthzSubject subject, PageControl pc)
        throws FinderException, PermissionException {
            
        Collection toBePaged = new ArrayList();
        try {
            toBePaged = getViewableServices(subject, pc);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(toBePaged, pc);
    }

    /**
     * Get the scope of viewable services for a given user
     * @return List of ServiceLocals for which subject has 
     * AuthzConstants.serviceOpViewService
     */
    private Collection getViewableServices(AuthzSubject subject,
                                            PageControl pc)
        throws NamingException, FinderException, 
               PermissionException {
        Collection toBePaged = new ArrayList();
        // get list of pks user can view
        List authzPks = getViewableServices(subject);
        Collection services = null;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
        
        switch( pc.getSortattribute() ) {
            case SortAttribute.RESOURCE_NAME:
                if(pc != null) {
                    services =
                        getServiceDAO().findAll_orderName(pc.isAscending());
                }
                break;
            case SortAttribute.SERVICE_NAME:
                if(pc != null) {
                    services =
                        getServiceDAO().findAll_orderName(pc.isAscending());
                }
                break;
            case SortAttribute.CTIME:
                if(pc != null) {
                    services =
                        getServiceDAO().findAll_orderCtime(pc.isAscending());
                }
                break;
            default:
                services = getServiceDAO().findAll();
                break;
        }
        for(Iterator i = services.iterator(); i.hasNext();) {
            Service aService = (Service)i.next();
            // remove service if its not viewable
            if(authzPks.contains(aService.getId())) {
                toBePaged.add(aService);
            }
        }
        return toBePaged;
    }

    /**
     * Fetch all services that haven't been assigned to a cluster and that
     * haven't been assigned to any applications.
     * @return A List of ServiceValue objects representing all of the
     * unassigned services that the given subject is allowed to view.
     * @ejb:interface-method
     */
    public PageList getAllClusterAppUnassignedServices(AuthzSubject subject, 
        PageControl pc) throws FinderException, PermissionException {
        // get list of pks user can view
        List authzPks = getViewableServices(subject);
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
                    ServiceCluster cluster = (ServiceCluster)o;
                    thisSvcTypeId = cluster.getServiceType().getId();
                }                
                // first, if they specified a server type, then filter on it
                if (!(thisSvcTypeId.equals(svcTypeId)))
                    continue;
                services.add(o);
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
            else if (o instanceof ServiceCluster) {
                ServiceCluster aCluster = (ServiceCluster)o;
                AppdefEntityID clusterId = AppdefEntityID
                    .newGroupID(aCluster.getGroup().getId().intValue());
                if (viewableEntityIds != null &&
                    viewableEntityIds.contains(clusterId)) {
                    retVal.add(o);
                }
            }
        }
        return retVal;
    }

    /**
     * Get services by server and type.
     * @ejb:interface-method
     */
    public PageList getServicesByServer(AuthzSubject subject,
                                        Integer serverId, PageControl pc) 
        throws ServiceNotFoundException, ServerNotFoundException, 
               PermissionException {
        return getServicesByServer(subject, serverId, APPDEF_RES_TYPE_UNDEFINED,
                                   pc);
    }

    /**
     * @ejb:interface-method
     */
    public PageList getServicesByServer(AuthzSubject subject,
                                        Integer serverId, Integer svcTypeId,
                                        PageControl pc) 
        throws ServiceNotFoundException, PermissionException {
        if (svcTypeId == null)
            svcTypeId = APPDEF_RES_TYPE_UNDEFINED;

        List services;

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_TYPE:
            services =
                getServiceDAO().findByServer_orderType(serverId);
            break;
        case SortAttribute.SERVICE_NAME:
        default:
            if (svcTypeId != APPDEF_RES_TYPE_UNDEFINED) {
                services =
                    getServiceDAO().findByServerAndType_orderName(
                        serverId, svcTypeId);
            }
            else {
                services =
                    getServiceDAO().findByServer_orderName(
                        serverId);
            }
            break;
        }
        // Reverse the list if descending
        if (pc != null && pc.isDescending()) {
            Collections.reverse(services);
        }
            
        List toBePaged = filterUnviewable(subject, services);
        return valuePager.seek(toBePaged, pc);
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
    public PageList getServicesByService(AuthzSubject subject,
                                         Integer serviceId, Integer svcTypeId,
                                         PageControl pc) 
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
    public PageList getServicesByPlatform(AuthzSubject subject,
                                          Integer platId, PageControl pc) 
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
     * Get platform services (children of virtual servers)
     * of a specified type
     * @ejb:interface-method
     */
    public PageList getPlatformServices(AuthzSubject subject,
                                        Integer platId, 
                                        Integer typeId,
                                        PageControl pc)
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
     * @ejb:interface-method
     */
    public Collection getPlatformServices(AuthzSubject subject,
                                          Integer platId)
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
     * @ejb:interface-method
     */
    public PageList getServicesByPlatform(AuthzSubject subject,
                                          Integer platId, Integer svcTypeId,
                                          PageControl pc) 
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
                                             PageControl pc ) 
        throws PermissionException, ApplicationNotFoundException,
               ServiceNotFoundException {

        try {
            // we only look up the application to validate
            // the appId param
            getApplicationDAO().findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }

        Collection appServiceCollection;
        AppServiceDAO appServLocHome =
            DAOFactory.getDAOFactory().getAppServiceDAO();
        pc = PageControl.initDefaults (pc, SortAttribute.SERVICE_NAME);

        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME :
            appServiceCollection = appServLocHome
                .findByApplication_orderSvcName(appId,pc.isAscending());
            break;
        case SortAttribute.RESOURCE_NAME :
            appServiceCollection = appServLocHome
                .findByApplication_orderSvcName(appId,pc.isAscending());
            break;
        case SortAttribute.SERVICE_TYPE :
            appServiceCollection = appServLocHome
                .findByApplication_orderSvcType(appId,pc.isAscending());
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
                services.add(appService.getServiceCluster());
            } else {
                services.add(appService.getService());
            }
        }
        return filterAndPage(services, subject, svcTypeId, pc);
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

        Collection svcCollection = new ArrayList();
        Collection appSvcCollection = appLocal.getAppServices();
        Iterator it = appSvcCollection.iterator();
        while (it != null && it.hasNext()) {
            AppService appService = (AppService) it.next();

            if (appService.isIsGroup()) {
                svcCollection.addAll(
                    appService.getServiceCluster().getServices());
            } else {
                svcCollection.add(appService.getService());
            } 
        }

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
    public Integer[] getFlattenedServiceIdsByApplication(
        AuthzSubject subject, Integer appId) 
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
                    ServiceCluster cluster = (ServiceCluster) o;
                    AppdefEntityID groupId = 
                        AppdefEntityID.newGroupID(
                            cluster.getGroup().getId().intValue());
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
            getApplicationDAO().findById(appId);
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(appId, e);
        }
        // appServiceCollection = appLocal.getAppServices();

        pc = PageControl.initDefaults(pc, SortAttribute.SERVICE_NAME);

        appServLocHome = new AppServiceDAO(DAOFactory.getDAOFactory());
        switch (pc.getSortattribute()) {
        case SortAttribute.SERVICE_NAME :
        case SortAttribute.RESOURCE_NAME :
        case SortAttribute.NAME :
            // TODO: Not actually sorting
            appServiceCollection =
                appServLocHome.findByApplication_orderName(appId);
            break;
        case SortAttribute.SERVICE_TYPE :
            appServiceCollection =
                appServLocHome.findByApplication_orderType(appId,
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
                services.add(appService.getServiceCluster());
            } else {
                services.add(appService.getService());
            }
        }
        return services;
    }

    /**
     * Private method to validate a new ServiceValue object
     */
    private void validateNewService(ServiceValue sv)
        throws ValidationException {
        String msg = null;
        // first check if its new 
        if(sv.idHasBeenSet()) {
            msg = "This service is not new. It has id: " + sv.getId();
        }
        // else if(someotherthing)  ...

        // Now check if there's a msg set and throw accordingly
        if(msg != null) {
            throw new ValidationException(msg);
        }
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
        svc.setModifiedTime(new Long(System.currentTimeMillis()));
        svc.setAutodiscoveryZombie(zombieStatus);
    }
    
    /**
     * @ejb:interface-method
     */
    public Service updateService(AuthzSubject subject, ServiceValue existing)
        throws PermissionException, UpdateException, 
               AppdefDuplicateNameException, ServiceNotFoundException {
        Service service =
            getServiceDAO().findById(existing.getId());
        checkModifyPermission(subject, service.getEntityId());
        existing.setModifiedBy(subject.getName());
        existing.setMTime(new Long(System.currentTimeMillis()));
        trimStrings(existing);

        if(service.matchesValueObject(existing)) {
            log.debug("No changes found between value object and entity");
        } else {
            if(!existing.getName().equals(service.getName())) {
                Resource rv = getAuthzResource(existing.getEntityId());
                rv.setName(existing.getName());
            }

            service.updateService(existing);
        }
        return service;
    }

    /**
     * Change Service owner.
     *
     * @ejb:interface-method
     */
    public void changeServiceOwner(AuthzSubject who, Integer serviceId,
                                   AuthzSubject newOwner)
        throws FinderException, PermissionException, CreateException {
        // first lookup the service
        Service service = getServiceDAO().findById(serviceId);
        // check if the caller can modify this service
        checkModifyPermission(who, service.getEntityId());
        // now get its authz resource
        ResourceValue authzRes = getServiceResourceValue(serviceId);
        // change the authz owner
        getResourceManager().setResourceOwner(who, authzRes, newOwner);
        // update the owner field in the appdef table -- YUCK
        service.setOwner(newOwner.getName());
        service.setModifiedBy(who.getName());
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

        ServiceTypeDAO stLHome = getServiceTypeDAO();
        ResourceGroupManagerLocal resGroupMan = 
            ResourceGroupManagerEJBImpl.getOne();
        ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();
        
        try {
            Collection curServices = stLHome.findByPlugin(plugin);
            ServerTypeDAO stHome = getServerTypeDAO();
            
            for (Iterator i = curServices.iterator(); i.hasNext();) {
                ServiceType stlocal = (ServiceType) i.next();

                if (log.isDebugEnabled()) {
                    log.debug("Begin updating ServiceTypeLocal: " +
                              stlocal.getName());
                }

                ServiceTypeInfo sinfo =
                    (ServiceTypeInfo) infoMap.remove(stlocal.getName());

                // See if this exists
                if (sinfo == null) {
                    Integer typeId = AuthzConstants.authzServiceProto;
                    Resource proto = 
                        resMan.findResourcePojoByInstanceId(typeId,
                                                            stlocal.getId());
                    
                    try {
                        // Delete compatible groups of this type.
                        resGroupMan.removeGroupsCompatibleWith(proto);
                    
                        // Remove all services
                        for (Iterator svcIt = stlocal.getServices().iterator();
                             svcIt.hasNext(); ) {
                            Service svcLocal = (Service) svcIt.next();
                            removeService(overlord, svcLocal);
                        }           
                    } catch (PermissionException e) {
                        assert false :
                            "Overlord should not run into PermissionException";
                    }

                    stLHome.remove(stlocal);
                } else {
                    // Just update it
                    // XXX TODO MOVE THIS INTO THE ENTITY
                    if (!sinfo.getName().equals(stlocal.getName()))
                        stlocal.setName(sinfo.getName());
                        
                    if (!sinfo.getDescription().equals(
                        stlocal.getDescription()))
                        stlocal.setDescription(sinfo.getDescription());
                    
                    if (sinfo.getInternal() !=  stlocal.isIsInternal())
                        stlocal.setIsInternal(sinfo.getInternal());

                    // Could be null if servertype was deleted/updated by plugin
                    ServerType svrtype = stlocal.getServerType();

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
                                    stlocal.getName() +
                                    "' relies");
                            }
                            serverTypes.put(svrtype.getName(), svrtype);
                        }
                        stlocal.setServerType(svrtype);
                    }
                }
            }
            
            Resource prototype = 
                ResourceManagerEJBImpl.getOne().findRootResource();
            
            // Now create the left-overs
            for (Iterator i = infoMap.values().iterator(); i.hasNext();) {
                ServiceTypeInfo sinfo = (ServiceTypeInfo) i.next();

                ServiceType stype = stLHome.create(sinfo.getName(), plugin, 
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
            stLHome.getSession().flush();
        }
    }

    /**
     * Remove a Service from the inventory.
     *
     * @ejb:interface-method
     */
    public void removeService(AuthzSubject subj, Integer serviceId)
        throws RemoveException, FinderException, PermissionException,
               VetoException 
    {
        Service service = getServiceDAO().findById(serviceId);
        removeService(subj, service);
    }

    /**
     * A removeService method that takes a ServiceLocal.  This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     * @ejb:interface-method
     */
    public void removeService(AuthzSubject subject, Service service)
        throws RemoveException, FinderException, PermissionException, 
               VetoException  
    {
        AppdefEntityID aeid = service.getEntityId();

        checkRemovePermission(subject, service.getEntityId());
        Integer cid = service.getConfigResponseId();

        // Remove authz resource.
        removeAuthzResource(subject, aeid);

        // Remove service from parent Server's Services collection
        Server server = service.getServer();
        Collection services = server.getServices();
        for (Iterator i = services.iterator(); i.hasNext(); ) {
            Service s = (Service)i.next();
            if (s.equals(service)) {
                i.remove();
                break;
            }
        }

        // remove from appdef
        getServiceDAO().remove(service);

        // remove the config response
        if (cid != null) {
            try {
                ConfigResponseDAO cdao = getConfigResponseDAO();
                cdao.remove(cdao.findById(cid));
            } catch (ObjectNotFoundException e) {
                // OK, no config response, just log it
                log.warn("Invalid config ID " + cid);
            }
        }

        // remove custom properties
        deleteCustomProperties(aeid);
    }

    /**
     * Map a ResourceGroup to ServiceCluster, just temporary,
     * should be able to remove when done with the
     * ServiceCluster to ResourceGroup Migration
     * @ejb:interface-method
     */
    public ServiceCluster getServiceCluster(ResourceGroup group) {
        if (group == null) {
            return null;
        }
        ServiceCluster sc = new ServiceCluster();
        sc.setName(group.getName());
        sc.setDescription(group.getDescription());
        sc.setGroup(group);
        
        Collection resources = group.getResources();

        Set services = new HashSet(resources.size());
        ServiceDAO dao = DAOFactory.getDAOFactory().getServiceDAO();
        ServiceType st = null;
        String svcResType = AuthzConstants.serviceResType;
        for (Iterator i=resources.iterator(); i.hasNext();) {
            Resource resource = (Resource)i.next();
            // this should not be the case
            if (!resource.getResourceType().getName().equals(svcResType)) {
                continue;
            }
            Service service = dao.findById((Integer)resource.getId());
            if (st == null) {
                st = service.getServiceType();
            }
            services.add(service);
            service.setResourceGroup(sc.getGroup());
        }
        sc.setServices(services);
        
        if (st == null && group.getGroupType() != null) {
            st = DAOFactory.getDAOFactory().getServiceTypeDAO()
                    .findById(group.getGroupType());
        }
        
        if (st != null) {
            sc.setServiceType(st);
        }
        return sc;
    }
        
    private ServiceCluster getServiceCluster(AuthzSubject subj,
                                             ServiceClusterValue cluster,
                                             List serviceIdList)
        throws PermissionException, FinderException
    {
        ServiceCluster sc = new ServiceCluster();
        ResourceGroupManagerLocal rgMan = ResourceGroupManagerEJBImpl.getOne();
        rgMan.getResourceGroupsById(subj,
            (Integer[])serviceIdList.toArray(new Integer[0]),
            PageControl.PAGE_NONE);
        ResourceGroup rg = getResourceGroupDAO().findById(cluster.getGroupId());
        sc.setName(cluster.getName());
        sc.setDescription(cluster.getDescription());
        sc.setGroup(rg);

        Set services = new HashSet(serviceIdList.size());
        ServiceDAO dao = DAOFactory.getDAOFactory().getServiceDAO();
        ServiceType st = null;
        for (int i = 0; i < serviceIdList.size(); i++) {
            Service service = dao.findById((Integer) serviceIdList.get(i));
            if (st == null) {
                st = service.getServiceType();
            }
            services.add(service);
            service.setResourceGroup(sc.getGroup());
        }
        sc.setServices(services);
        
        if (st == null && cluster.getServiceType() != null) {
            st = DAOFactory.getDAOFactory().getServiceTypeDAO()
                    .findById(cluster.getServiceType().getId());
        }
        
        if (st != null) {
            sc.setServiceType(st);
        }
        return sc;
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

    private void trimStrings(ServiceValue service) {
        if (service.getDescription() != null)
            service.setDescription(service.getDescription().trim());
        if (service.getLocation() != null)
            service.setLocation(service.getLocation().trim());
        if (service.getName() != null)
            service.setName(service.getName().trim());
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
