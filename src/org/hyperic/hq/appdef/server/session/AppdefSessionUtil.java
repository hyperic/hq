/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.ConfigResponseDAO;
import org.hyperic.hq.dao.PlatformDAO;
import org.hyperic.hq.dao.PlatformTypeDAO;
import org.hyperic.hq.dao.ServiceDAO;
import org.hyperic.hq.dao.ServerDAO;
import org.hyperic.hq.dao.AgentTypeDAO;
import org.hyperic.hq.dao.AgentDAO;
import org.hyperic.hq.dao.ApplicationDAO;
import org.hyperic.hq.dao.ApplicationTypeDAO;
import org.hyperic.hq.dao.ServiceClusterDAO;
import org.hyperic.hq.dao.AIServerDAO;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.product.TypeInfo;

import javax.ejb.FinderException;

public abstract class AppdefSessionUtil {
    private AIQueueManagerLocal         aiqManagerLocal;
    private ConfigManagerLocal          configMgrL;
    private ResourceManagerLocal        rmLocal;
    private CPropManagerLocal           cpropLocal;

    protected CPropManagerLocal getCPropMgrLocal(){
        if(this.cpropLocal == null){
            this.cpropLocal = CPropManagerEJBImpl.getOne();
        }
        return this.cpropLocal;
    }

    protected ConfigManagerLocal getConfigMgrLocal() {
        if (configMgrL == null) {
            configMgrL = ConfigManagerEJBImpl.getOne();
        }
        return configMgrL;
    }

    protected ApplicationManagerLocal getApplicationMgrLocal() {
        return ApplicationManagerEJBImpl.getOne();
    }

    protected AppdefGroupManagerLocal getAppdefGroupManagerLocal() {
        return AppdefGroupManagerEJBImpl.getOne();
    }

    protected PlatformManagerLocal getPlatformMgrLocal() {
        return PlatformManagerEJBImpl.getOne();
    }

    protected ServerManagerLocal getServerMgrLocal() {
        return ServerManagerEJBImpl.getOne();
    }

    protected ServiceManagerLocal getServiceMgrLocal() {
        return ServiceManagerEJBImpl.getOne();
    }

    protected ResourceManagerLocal getResourceManager() {
        if (rmLocal == null) {
            rmLocal = ResourceManagerEJBImpl.getOne();
        } 
        return rmLocal;
    } 

    protected AIQueueManagerLocal getAIQManagerLocal() {
        if (aiqManagerLocal == null) {
            aiqManagerLocal = AIQueueManagerEJBImpl.getOne();
        }
        return aiqManagerLocal;
    }

    protected AgentDAO getAgentDAO() {
        return DAOFactory.getDAOFactory().getAgentDAO();
    }

    protected AgentTypeDAO getAgentTypeDAO() {
        return DAOFactory.getDAOFactory().getAgentTypeDAO();
    }

    protected ConfigResponseDAO getConfigResponseDAO() {
        return DAOFactory.getDAOFactory().getConfigResponseDAO();
    }

    protected ServiceClusterDAO getServiceClusterDAO() {
        return DAOFactory.getDAOFactory().getServiceClusterDAO();
    }

    protected PlatformDAO getPlatformDAO() {
        return DAOFactory.getDAOFactory().getPlatformDAO();
    }

    protected PlatformTypeDAO getPlatformTypeDAO() {
        return DAOFactory.getDAOFactory().getPlatformTypeDAO();
    }

    protected ServerDAO getServerDAO() {
        return DAOFactory.getDAOFactory().getServerDAO();
    }

    protected ServerTypeDAO getServerTypeDAO() {
        return DAOFactory.getDAOFactory().getServerTypeDAO();
    }

    protected ServiceTypeDAO getServiceTypeDAO() {
        return DAOFactory.getDAOFactory().getServiceTypeDAO();
    }

    protected ServiceDAO getServiceDAO() {
        return DAOFactory.getDAOFactory().getServiceDAO();
    }

    protected ApplicationTypeDAO getApplicationTypeDAO() {
        return DAOFactory.getDAOFactory().getApplicationTypeDAO();
    }

    protected ApplicationDAO getApplicationDAO() {
        return DAOFactory.getDAOFactory().getApplicationDAO();
    }

    protected AIServerDAO getAIServerDAO() {
        return DAOFactory.getDAOFactory().getAIServerDAO();
    }

    protected AppdefResourceTypeValue findResourceType(int appdefType,
                                                       int appdefTypeId)
        throws AppdefEntityNotFoundException
    {
        Integer id = new Integer(appdefTypeId);

        if(appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM){
            PlatformManagerLocal pmLocal;

            pmLocal = this.getPlatformMgrLocal();
            return pmLocal.findPlatformTypeValueById(id);
        } else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER){
            ServerManagerLocal smLocal;

            smLocal = this.getServerMgrLocal();
            try {
                return smLocal.findServerTypeById(id);
            } catch(FinderException exc){
                throw new ServerNotFoundException("Server type id=" +
                                                  appdefTypeId + 
                                                  " not found");
            }
        } else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
            ServiceManagerLocal vmLocal;

            vmLocal = this.getServiceMgrLocal();
            try {
                return vmLocal.findServiceTypeById(id);
            } catch(FinderException exc){
                throw new ServiceNotFoundException("Service type id=" +
                                                   appdefTypeId +
                                                   " not found");
            }
        } else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
            ApplicationManagerLocal amLocal;
            
            amLocal = this.getApplicationMgrLocal();
            try {
                return amLocal.findApplicationTypeById(id);
            } catch(FinderException exc){
                throw new ApplicationNotFoundException("App type id=" +
                                                       appdefTypeId + 
                                                       "not found");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:"+
                                               " " + appdefType);
        }
    }

    protected AppdefResourceType findResourceType(TypeInfo info) {
        int type = info.getType();

        if(type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM){
            return getPlatformTypeDAO().findByName(info.getName());
        } else if(type == AppdefEntityConstants.APPDEF_TYPE_SERVER){
            return getServerTypeDAO().findByName(info.getName());
        } else if(type == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
            return getServiceTypeDAO().findByName(info.getName());
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:" +
                                               " " + info);
        }
    }
}
