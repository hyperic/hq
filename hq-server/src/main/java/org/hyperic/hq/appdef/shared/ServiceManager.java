/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.appdef.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ServiceManager.
 */
public interface ServiceManager {

    public Service createService(AuthzSubject subject, Server server, ServiceType type, String name, String desc,
                                 String location, Service parent) throws PermissionException;

    /**
     * Move a Service from one Platform to another.
     * @param subject The user initiating the move.
     * @param target The target Service to move.
     * @param destination The destination Platform to move this Service to.
     * @throws PermissionException If the passed user does not have permission
     *         to move the Service.
     * @throws VetoException If the operation canot be performed due to
     *         incompatible types.
     */
    public void moveService(AuthzSubject subject, Service target, Platform destination) throws VetoException,
        PermissionException;

    /**
     * Move a Service from one Server to another.
     * @param subject The user initiating the move.
     * @param target The target Service to move.
     * @param destination The destination Server to move this Service to.
     * @throws PermissionException If the passed user does not have permission
     *         to move the Service.
     * @throws VetoException If the operation canot be performed due to
     *         incompatible types.
     */
    public void moveService(AuthzSubject subject, Service target, Server destination) throws VetoException,
        PermissionException;

    /**
     * Create a Service which runs on a given server
     * @return The service id.
     */
    public Service createService(AuthzSubject subject, Integer serverId, Integer serviceTypeId, String name,
                                 String desc, String location) throws ValidationException, PermissionException,
        ServerNotFoundException, AppdefDuplicateNameException;

    /**
     * Get service IDs by service type.
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    public java.lang.Integer[] getServiceIds(AuthzSubject subject, Integer servTypeId) throws PermissionException;

    public List<ServiceValue> findServicesById(AuthzSubject subject, java.lang.Integer[] serviceIds)
        throws ServiceNotFoundException, PermissionException;

    /**
     * Find Service by Id.
     */
    public Service findServiceById(Integer id) throws ServiceNotFoundException;

    /**
     * Get Service by Id.
     * @return The Service identified by this id, or null if it does not exist.
     */
    public Service getServiceById(Integer id);

    /**
     * Get Service by Id and perform permission check.
     * @return The Service identified by this id.
     */
    public Service getServiceById(AuthzSubject subject, Integer id) throws ServiceNotFoundException,
        PermissionException;

    public List<Service> getServicesByAIID(Server server, String aiid);

    public Service getServiceByName(Server server, String name);

    public Service getServiceByName(Platform platform, String name);

    /**
     * Find a ServiceType by id
     */
    public ServiceType findServiceType(Integer id) throws org.hibernate.ObjectNotFoundException;

    /**
     * Find service type by name
     */
    public ServiceType findServiceTypeByName(String name);

    public Collection<Service> findDeletedServices();
    
    public PageList<ServiceTypeValue> getAllServiceTypes(AuthzSubject subject, PageControl pc);

    public PageList<ServiceTypeValue> getViewableServiceTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;

    public PageList<ServiceTypeValue> getServiceTypesByServerType(AuthzSubject subject, int serverTypeId);

    public PageList<ServiceTypeValue> findVirtualServiceTypesByPlatform(AuthzSubject subject, Integer platformId);

    public PageList<ServiceValue> getAllServices(AuthzSubject subject, PageControl pc) throws PermissionException, NotFoundException;

    /**
     * Fetch all services that haven't been assigned to a cluster and that
     * haven't been assigned to any applications.
     * @return A List of ServiceValue objects representing all of the unassigned
     *         services that the given subject is allowed to view.
     */
    public PageList<ServiceValue> getAllClusterAppUnassignedServices(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * Get services by server and type.
     */
    public PageList getServicesByServer(AuthzSubject subject, Integer serverId, PageControl pc)
        throws ServiceNotFoundException, ServerNotFoundException, PermissionException;

    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId,
                                                      PageControl pc) throws ServiceNotFoundException,
        PermissionException;

    /**
     * Get service POJOs by server and type.
     */
    public List getServicesByServer(AuthzSubject subject, Server server) throws PermissionException,
        ServiceNotFoundException;

    public Integer[] getServiceIdsByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException;

    public List<ServiceValue> getServicesByType(AuthzSubject subject, String svcName, boolean asc)
        throws PermissionException, org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;

    public PageList getServicesByService(AuthzSubject subject, Integer serviceId, PageControl pc)
        throws ServiceNotFoundException, PermissionException;

    /**
     * Get services by server.
     */
    public PageList<ServiceValue> getServicesByService(AuthzSubject subject, Integer serviceId, Integer svcTypeId,
                                                       PageControl pc) throws ServiceNotFoundException,
        PermissionException;

    /**
     * Get service IDs by service.
     */
    public java.lang.Integer[] getServiceIdsByService(AuthzSubject subject, Integer serviceId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException;

    public PageList getServicesByPlatform(AuthzSubject subject, Integer platId, PageControl pc)
        throws ServiceNotFoundException, org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException;

    /**
     * Get platform services (children of virtual servers)
     */
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, PageControl pc)
        throws org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException, ServiceNotFoundException;

    /**
     * Get platform services (children of virtual servers) of a specified type
     */
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, Integer typeId,
                                                      PageControl pc)
        throws org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException, ServiceNotFoundException;

    /**
     * Get {@link Service}s which are children of the server, and of the
     * specified type.
     */
    public List<Service> findServicesByType(Server server, ServiceType st);

    /**
     * Get platform service POJOs
     */
    public List<Service> findPlatformServicesByType(Platform p, ServiceType st);

    /**
     * Get platform service POJOs
     */
    public Collection getPlatformServices(AuthzSubject subject, Integer platId) throws ServiceNotFoundException,
        PermissionException;

    /**
     * Get platform services (children of virtual servers), mapped by type id of
     * a specified type
     */
    public Map getMappedPlatformServices(AuthzSubject subject, Integer platId, PageControl pc)
        throws org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException, ServiceNotFoundException;

    /**
     * Get services by platform.
     */
    public PageList<ServiceValue> getServicesByPlatform(AuthzSubject subject, Integer platId, Integer svcTypeId,
                                                        PageControl pc) throws ServiceNotFoundException,
        org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException;

    public PageList<AppdefResourceValue> getServicesByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException, PermissionException;

    public PageList getServicesByApplication(AuthzSubject subject, Integer appId, Integer svcTypeId, PageControl pc)
        throws PermissionException, org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException;

    public List getServicesByApplication(AuthzSubject subject, Integer appId) throws PermissionException,
        org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException;

    public PageList<AppdefResourceValue> getServiceInventoryByApplication(AuthzSubject subject, Integer appId,
                                                                          PageControl pc)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException, PermissionException;

    /**
     * Get all services by application. This is to only be used for the Evident
     * API.
     */
    public PageList getFlattenedServicesByApplication(AuthzSubject subject, Integer appId, Integer typeId,
                                                      PageControl pc)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException, PermissionException;

    public PageList getServiceInventoryByApplication(AuthzSubject subject, Integer appId, Integer svcTypeId,
                                                     PageControl pc)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException, PermissionException;

    /**
     * Get all service inventory by application, including those inside an
     * associated cluster
     * @param subject The subject trying to list services.
     * @param appId Application id.
     * @return A List of ServiceValue objects representing all of the services
     *         that the given subject is allowed to view.
     */
    public Integer[] getFlattenedServiceIdsByApplication(AuthzSubject subject, Integer appId)
        throws ServiceNotFoundException, PermissionException, org.hyperic.hq.appdef.shared.ApplicationNotFoundException;

    public void updateServiceZombieStatus(AuthzSubject subject, Service svc, boolean zombieStatus)
        throws PermissionException;

    public Service updateService(AuthzSubject subject, ServiceValue existing) throws PermissionException,
        org.hyperic.hq.appdef.shared.UpdateException, org.hyperic.hq.appdef.shared.AppdefDuplicateNameException,
        ServiceNotFoundException;

    public void updateServiceTypes(String plugin, org.hyperic.hq.product.ServiceTypeInfo[] infos) throws VetoException, NotFoundException;

    public void deleteServiceType(ServiceType serviceType, AuthzSubject overlord, ResourceGroupManager resGroupMan,
                                  ResourceManager resMan) throws VetoException;

    /**
     * A removeService method that takes a ServiceLocal. This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     */
    public void removeService(AuthzSubject subject, Service service) throws PermissionException, VetoException;

    public void handleResourceDelete(Resource resource);

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * service type, the second element is the # of services of that type in the
     * inventory.
     */
    public List<Object[]> getServiceTypeCounts();

    /**
     * Get the # of services within HQ inventory
     */
    public Number getServiceCount();

    ServiceCluster getServiceCluster(ResourceGroup group);

    ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin, ServerType servType)
    throws NotFoundException;

    public Collection<Service> getOrphanedServices();

}
