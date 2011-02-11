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

import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ServiceManager.
 */
public interface ServiceManager {

    /**
     * Create a Service which runs on a given server or platform
     * @return The service id.
     */
    public Service createService(AuthzSubject subject, Integer parentId, Integer serviceTypeId, String name,
                                 String desc, String location) throws ValidationException, PermissionException,
        ServerNotFoundException, AppdefDuplicateNameException;

    /**
     * Get service IDs by service type.
     * @param subject The subject trying to list service.
     * @param servTypeId service type id.
     * @return An array of service IDs.
     */
    public java.lang.Integer[] getServiceIds(AuthzSubject subject, Integer servTypeId) throws PermissionException;

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

    /**
     * @param server {@link Server}
     * @param aiid service autoinventory identifier
     * @return {@link List} of {@link Service}
     * This method also returns services of a virtual server (b/c it's AI related), 
     * while other getServicesByServer methods will not do that
     */
    public List<Service> getServicesByAIID(Server server, String aiid);

    /**
     * Find a ServiceType by id
     */
    public ServiceType findServiceType(Integer id) throws org.hibernate.ObjectNotFoundException;

    /**
     * Find service type by name
     */
    public ServiceType findServiceTypeByName(String name);
    
    public PageList<ServiceTypeValue> getAllServiceTypes(AuthzSubject subject, PageControl pc);
    
    PageList<Resource> getAllServiceResources(AuthzSubject subject, PageControl pc);

    public PageList<ServiceTypeValue> getViewableServiceTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;

    public PageList<ServiceTypeValue> getServiceTypesByServerType(AuthzSubject subject, int serverTypeId);
    
    PageList<ServiceTypeValue> getServiceTypesByPlatformType(AuthzSubject subject,
        Integer platformTypeId);

    /**
     * Get services by server and type.
     */
    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId, PageControl pc)
        throws ServiceNotFoundException, ServerNotFoundException, PermissionException;

    public PageList<ServiceValue> getServicesByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId,
                                                      PageControl pc) throws ServiceNotFoundException,
        PermissionException;

    /**
     * Get service POJOs by server and type.
     */
    public List<Service> getServicesByServer(AuthzSubject subject, Server server) throws PermissionException,
        ServiceNotFoundException;

    public Integer[] getServiceIdsByServer(AuthzSubject subject, Integer serverId, Integer svcTypeId)
        throws ServiceNotFoundException, PermissionException;

    /**
     * Get platform services
     */
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, PageControl pc)
        throws org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException, ServiceNotFoundException;

    /**
     * Get platform services of a specified type
     */
    public PageList<ServiceValue> getPlatformServices(AuthzSubject subject, Integer platId, Integer typeId,
                                                      PageControl pc)
        throws org.hyperic.hq.appdef.shared.PlatformNotFoundException, PermissionException, ServiceNotFoundException;   

    /**
     * Get platform service POJOs
     */
    public Collection<Service> getPlatformServices(AuthzSubject subject, Integer platId) throws ServiceNotFoundException,
        PermissionException;


    public PageList<ServiceValue> getServicesByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException, PermissionException;

    
    public List<Service> getServicesByApplication(AuthzSubject subject, Integer appId) throws PermissionException,
        org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException;
    
    PageList<ServiceValue> getServicesByApplication(AuthzSubject subject, Integer appId, Integer serviceTypeId, PageControl pc) throws PermissionException,
    org.hyperic.hq.appdef.shared.ApplicationNotFoundException, ServiceNotFoundException;

    public void updateServiceZombieStatus(AuthzSubject subject, Service svc, boolean zombieStatus)
        throws PermissionException;

    public Service updateService(AuthzSubject subject, ServiceValue existing) throws PermissionException,
        ServiceNotFoundException;
    
    public void updateServiceTypes(String plugin, org.hyperic.hq.product.ServiceTypeInfo[] infos) throws VetoException, NotFoundException;

    public void deleteServiceType(ServiceType serviceType, AuthzSubject overlord) throws VetoException;

    /**
     * A removeService method that takes a ServiceLocal. This is called by
     * ServerManager.removeServer when cascading a delete onto services.
     */
    public void removeService(AuthzSubject subject, Service service) throws PermissionException, VetoException;
    
    void removeService(AuthzSubject subject, Integer serviceId) throws PermissionException,
        VetoException;

    /**
     * @return A Map of service type to the # of services of that type in the
     * inventory.
     */
    public  Map<String,Integer> getServiceTypeCounts();
    
    Number getServiceCount();
    
    ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                  ResourceType parentType) throws NotFoundException;
    
    ServiceType createServiceType(ServiceTypeInfo sinfo, String plugin,
                                  String[] platformTypes) throws NotFoundException;

}
