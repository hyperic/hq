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
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.VirtualManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Server objects in appdef and their
 * relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class VirtualManagerImpl implements VirtualManager {

    private VirtualDAO virtualDAO;

    private PlatformDAO platformDAO;

    private ServerDAO serverDAO;

    private ServiceDAO serviceDAO;

    private ResourceManager resourceManager;

    @Autowired
    public VirtualManagerImpl(VirtualDAO virtualDAO, PlatformDAO platformDAO, ServerDAO serverDAO,
                              ServiceDAO serviceDAO, ResourceManager resourceManager) {
        this.virtualDAO = virtualDAO;
        this.platformDAO = platformDAO;
        this.serverDAO = serverDAO;
        this.serviceDAO = serviceDAO;
        this.resourceManager = resourceManager;
    }

    /**
     * Find virtual platforms in a VM Process
     * @return a list of virtual platform values
     * 
     */
    @Transactional(readOnly=true)
    public List<PlatformValue> findVirtualPlatformsByVM(AuthzSubject subject, Integer vmId)
        throws PlatformNotFoundException, PermissionException {
        Collection<Platform> platforms = platformDAO.findVirtualByProcessId(vmId);
        List<PlatformValue> platVals = new ArrayList<PlatformValue>();
        for (Platform platform : platforms) {
            platVals.add(platform.getPlatformValue());
        }
        return platVals;
    }

    /**
     * Find virtual servers in a VM Process
     * @return a list of virtual server values
     * 
     */
    @Transactional(readOnly=true)
    public List<ServerValue> findVirtualServersByVM(AuthzSubject subject, Integer vmId) throws ServerNotFoundException,
        PermissionException {
        Collection<Server> servers = serverDAO.findVirtualByProcessId(vmId);
        List<ServerValue> serverVals = new ArrayList<ServerValue>();
        for (Server server : servers) {
            serverVals.add(server.getServerValue());
        }
        return serverVals;

    }

    /**
     * Find virtual services in a VM Process
     * @return a list of virtual service values
     * 
     */
    @Transactional(readOnly=true)
    public List<ServiceValue> findVirtualServicesByVM(AuthzSubject subject, Integer vmId)
        throws ServiceNotFoundException, PermissionException {
        Collection<Service> services = serviceDAO.findVirtualByProcessId(vmId);
        List<ServiceValue> svcVals = new ArrayList<ServiceValue>();
        for (Service service : services) {
            svcVals.add(service.getServiceValue());
        }
        return svcVals;
    }

    /**
     * Find virtual resources whose parent is the given physical ID
     * @return list of virtual resource values
     * 
     */
    @Transactional(readOnly=true)
    public List<AppdefResourceValue> findVirtualResourcesByPhysical(AuthzSubject subject, AppdefEntityID aeid)
        throws AppdefEntityNotFoundException, PermissionException {
        Collection<AppdefResource> appResources = new ArrayList<AppdefResource>();
        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                appResources.addAll(platformDAO.findVirtualByPhysicalId(aeid.getId()));
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                appResources.addAll(serverDAO.findVirtualByPysicalId(aeid.getId()));
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                appResources.addAll(serviceDAO.findVirtualByPysicalId(aeid.getId()));
                break;
            default:
                throw new InvalidAppdefTypeException("Appdef Entity Type: " + aeid.getType() +
                                                     " does not support virtual resources");
        }

        List<AppdefResourceValue> resourcesList = new ArrayList<AppdefResourceValue>();
        for (Iterator<AppdefResource> it = appResources.iterator(); it.hasNext();) {
            switch (aeid.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    resourcesList.add(((Platform) it.next()).getPlatformValue());
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    resourcesList.add(((Server) it.next()).getServerValue());
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    resourcesList.add(((Service) it.next()).getServiceValue());
                    break;
                default:
                    break;
            }
        }

        return resourcesList;
    }

    /**
     * Associate an array of entities to a VM
     * 
     * 
     */
    public void associateEntities(AuthzSubject subj, Integer processId, AppdefEntityID[] aeids)  {

        for (int i = 0; i < aeids.length; i++) {
            String typeStr = AppdefUtil.appdefTypeIdToAuthzTypeStr(aeids[i].getType());
            Resource res = resourceManager.findResourceByTypeAndInstanceId(typeStr, aeids[i].getId());
            virtualDAO.createVirtual(res, processId);
        }

    }

    /**
     * Associate an array of entities to a VM
     * @throws NotFoundException
     * 
     * 
     */
    public void associateToPhysical(AuthzSubject subj, Integer physicalId, AppdefEntityID aeid) throws NotFoundException {
        Resource resource;
        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resource = platformDAO.findVirtualByInstanceId(aeid.getId());
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resource = serverDAO.findVirtualByInstanceId(aeid.getId());
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resource = serviceDAO.findVirtualByInstanceId(aeid.getId());
                break;
            default:
                throw new InvalidAppdefTypeException("Cannot associate appdefType " + aeid.getType() +
                                                     " to physical resource");
        }

        if (resource != null) {
            Virtual virt = virtualDAO.findByResource(resource.getId());
            virt.setPhysicalId(physicalId);
        } else {
            throw new NotFoundException(aeid.toString() + " is not registered as a virtual resource");
        }
    }
}
