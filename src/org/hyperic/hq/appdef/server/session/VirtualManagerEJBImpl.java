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
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.VirtualDAO;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 * @ejb:bean name="VirtualManager"
 *      jndi-name="ejb/appdef/VirtualManager"
 *      local-jndi-name="LocalVirtualManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class VirtualManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {

    private Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.session.VirtualManagerEJBImpl");

    private VirtualDAO getVirtualDAO() {
        return DAOFactory.getDAOFactory().getVirtualDAO();
    }

    /**
     * Find virtual platforms in a VM Process
     * @return a list of virtual platform values
     * @ejb:interface-method
     */
    public List findVirtualPlatformsByVM(AuthzSubjectValue subject, Integer vmId)
        throws PlatformNotFoundException, PermissionException {
        Collection platforms = getPlatformDAO().findVirtualByProcessId(vmId);
        List platVals = new ArrayList();
        for (Iterator it = platforms.iterator(); it.hasNext(); ) {
            Platform platform = (Platform) it.next();
            platVals.add(platform.getPlatformValue());
        }
        return platVals;
    }

    /**
     * Find virtual servers in a VM Process
     * @return a list of virtual server values
     * @ejb:interface-method
     */
    public List findVirtualServersByVM(AuthzSubjectValue subject, Integer vmId)
        throws ServerNotFoundException, PermissionException {
        Collection servers = getPlatformDAO().findVirtualByProcessId(vmId);
        List serverVals = new ArrayList();
        for (Iterator it = servers.iterator(); it.hasNext(); ) {
            Server server = (Server) it.next();
            serverVals.add(server.getServerValue());
        }
        return serverVals;

    }

    /**
     * Find virtual services in a VM Process
     * @return a list of virtual service values
     * @ejb:interface-method
     */
    public List findVirtualServicesByVM(AuthzSubjectValue subject, Integer vmId)
        throws ServiceNotFoundException, PermissionException {
        Collection services = getPlatformDAO().findVirtualByProcessId(vmId);
        List svcVals = new ArrayList();
        for (Iterator it = services.iterator(); it.hasNext(); ) {
            Service service = (Service) it.next();
            svcVals.add(service.getServiceValue());
        }
        return svcVals;
    }

    /**
     * Find virtual resources whose parent is the given physical ID
     * @return list of virtual resource values
     * @ejb:interface-method
     */
    public List findVirtualResourcesByPhysical(AuthzSubject subject,
                                               AppdefEntityID aeid)
        throws AppdefEntityNotFoundException, PermissionException {
        Collection appResources;
        switch (aeid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            appResources =
                getPlatformDAO().findVirtualByPhysicalId(aeid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            appResources =
                getServerDAO().findVirtualByPysicalId(aeid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            appResources =
                getServiceDAO().findVirtualByPysicalId(aeid.getId());
            break;
        default:
            throw new InvalidAppdefTypeException(
                "Appdef Entity Type: " + aeid.getType() +
                " does not support virtual resources");
        }
        
        List resourcesList = new ArrayList();
        for (Iterator it = appResources.iterator(); it.hasNext(); ) {
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
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void associateEntities(AuthzSubjectValue subj,
                                  Integer processId,
                                  AppdefEntityID[] aeids)
        throws FinderException {
        VirtualDAO dao = getVirtualDAO();
        
        try {
            ResourceManagerLocal resMan =
                ResourceManagerUtil.getLocalHome().create();
            
            for (int i = 0; i < aeids.length; i++) {
               String typeStr =
                   AppdefUtil.appdefTypeIdToAuthzTypeStr(aeids[i].getType());
               ResourceValue res =
                   resMan.findResourceByTypeAndInstanceId(typeStr,
                                                          aeids[i].getId());
               dao.createVirtual(res, processId);
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Associate an array of entities to a VM
     * @throws FinderException 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void associateToPhysical(AuthzSubjectValue subj,
                                    Integer physicalId,
                                    AppdefEntityID aeid)
        throws FinderException {
        Resource resource;
        switch (aeid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            resource =
                getPlatformDAO().findVirtualByInstanceId(aeid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            resource =
                getServerDAO().findVirtualByInstanceId(aeid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            resource =
                getServiceDAO().findVirtualByInstanceId(aeid.getId());
            break;
        default:
            throw new InvalidAppdefTypeException(
                "Cannot associate appdefType " + aeid.getType() +
                " to physical resource");
        }

        if (resource != null) {
            Virtual virt = getVirtualDAO().findByResource(resource.getId());
            virt.setPhysicalId(physicalId);
        }
        else {
            throw new FinderException(aeid.toString() +
                " is not registered as a virtual resource");
        }
    }

    public void ejbCreate() throws CreateException {}    
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
}
