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
import org.hyperic.hq.dao.ServiceTypeDAO;
import org.hyperic.hq.dao.ServerDAO;
import org.hyperic.hq.dao.ServerTypeDAO;
import org.hyperic.hq.dao.AgentTypeDAO;
import org.hyperic.hq.dao.AgentDAO;
import org.hyperic.hq.dao.ApplicationDAO;
import org.hyperic.hq.dao.ApplicationTypeDAO;
import org.hyperic.hq.dao.ServiceClusterDAO;
import org.hyperic.hq.dao.AIServerDAO;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIQueueManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocalHome;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocalHome;
import org.hyperic.hq.appdef.shared.ApplicationManagerUtil;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.CPropManagerUtil;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocalHome;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.TypeInfo;


import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;

public abstract class AppdefSessionUtil {
    private AIQueueManagerLocal         aiqManagerLocal;
    private ApplicationManagerLocalHome appMgrLHome;
    private AppdefGroupManagerLocalHome grpMgrLHome;
    private ConfigManagerLocal          configMgrL;
    private PlatformManagerLocalHome    platformMgrLHome;
    private ResourceManagerLocal        rmLocal;
    private ServerManagerLocalHome      serverMgrLHome;
    private ServiceManagerLocalHome     serviceMgrLHome;
    private CPropManagerLocal           cpropLocal;

    protected CPropManagerLocal getCPropMgrLocal(){
        if(this.cpropLocal == null){
            try {
                this.cpropLocal = CPropManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.cpropLocal;
    }

    protected ConfigManagerLocal getConfigMgrLocal() {
        try {
            if (configMgrL == null) {
                configMgrL = ConfigManagerUtil.getLocalHome().create();
            }
        } catch(Exception exc){
            throw new SystemException(exc);
        }
        return configMgrL;
    }

    protected AgentDAO getAgentDAO()
    {
        return DAOFactory.getDAOFactory().getAgentDAO();
    }

    protected AgentTypeDAO getAgentTypeDAO()
    {
        return DAOFactory.getDAOFactory().getAgentTypeDAO();
    }

    protected ConfigResponseDAO getConfigResponseDAO()
    {
        return DAOFactory.getDAOFactory().getConfigResponseDAO();
    }

    protected ServiceClusterDAO getServiceClusterDAO()
    {
        return DAOFactory.getDAOFactory().getServiceClusterDAO();
    }

    protected ApplicationManagerLocal getApplicationMgrLocal() {
        try {
            if (appMgrLHome == null) {
                appMgrLHome = ApplicationManagerUtil.getLocalHome();
            }
            return appMgrLHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    protected AppdefGroupManagerLocalHome getAppdefGroupManagerLocalHome() {
        try {
            if (grpMgrLHome == null) {
                grpMgrLHome = AppdefGroupManagerUtil.getLocalHome();
            }
            return grpMgrLHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    protected AppdefGroupManagerLocal getAppdefGroupManagerLocal() {
        try {
            return getAppdefGroupManagerLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    protected PlatformManagerLocal getPlatformMgrLocal() {
        try {
            if (platformMgrLHome == null) {
                platformMgrLHome = PlatformManagerUtil.getLocalHome();
            }
            return platformMgrLHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    protected ServerManagerLocal getServerMgrLocal() {
        try {
            if (serverMgrLHome == null) {
                serverMgrLHome = ServerManagerUtil.getLocalHome();
            }
            return serverMgrLHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    protected ServiceManagerLocal getServiceMgrLocal() {
        try {
            if (serviceMgrLHome == null) {
                serviceMgrLHome = ServiceManagerUtil.getLocalHome();
            }
            return serviceMgrLHome.create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get the local interace of the Resource Manager
     * @return resourceManagerLocal
     */
    protected ResourceManagerLocal getResourceManager()
    {
        if(rmLocal == null) {   
        	try {
        		rmLocal = ResourceManagerUtil.getLocalHome().create();
        	} catch (Exception e) {
        		throw new SystemException(e);
        	}
        } 
        return rmLocal;
    } 

    /**
     * Get the LocalHome reference for the PlatformObject
     */
    protected PlatformDAO getPlatformDAO() 
    {
        return DAOFactory.getDAOFactory().getPlatformDAO();
    }

    /**
     * Get the LocalHome reference for the PlatformTypeObject
     */
    protected PlatformTypeDAO getPlatformTypeDAO() 
    {
        return DAOFactory.getDAOFactory().getPlatformTypeDAO();
    }

    protected ServerDAO getServerDAO()
    {
        return DAOFactory.getDAOFactory().getServerDAO();
    }

    /**
     * Get the LocalHome reference for the ServerTypeObject
     * @return ServerTypeLocalHome
     */
    protected ServerTypeDAO getServerTypeDAO()
    {
        return DAOFactory.getDAOFactory().getServerTypeDAO();
    }

    /**
     * Get the LocalHome reference for the ServiceTypeObject
     * @return ServiceTypeDAO
     */
    protected ServiceTypeDAO getServiceTypeDAO()
    {
        return DAOFactory.getDAOFactory().getServiceTypeDAO();
    }

    protected ServiceDAO getServiceDAO() {
        return DAOFactory.getDAOFactory().getServiceDAO();
    }

    /**
     * Get the LocalHome reference for the ApplicationType
     * @return ApplicationTypeDAO
     */
    protected ApplicationTypeDAO getApplicationTypeDAO()
    {
        return DAOFactory.getDAOFactory().getApplicationTypeDAO();
    }

    /**
     * Get the LocalHome reference for the Application
     * @return ApplicationLocalHome
     */
    protected ApplicationDAO getApplicationDAO()
    {
        return DAOFactory.getDAOFactory().getApplicationDAO();
    }

    /**
     * Get the LocalHome reference for the AIServer 
     * @return AIServerLocalHome
     */
    protected AIServerDAO getAIServerDAO()
    {
        return DAOFactory.getDAOFactory().getAIServerDAO();
    }

    /**
     * Get the LocalHome reference for the AIQueueManager 
     * @return AIServerLocalHome
     */
    protected AIQueueManagerLocal getAIQManagerLocal()
        throws CreateException, NamingException 
    {
        if(aiqManagerLocal == null) {
            aiqManagerLocal = AIQueueManagerUtil.getLocalHome().create();
        }
        return aiqManagerLocal;
    }

    protected AppdefResourceTypeValue findResourceType(int appdefType,
                                                       int appdefTypeId)
        throws AppdefEntityNotFoundException
    {
        Integer id = new Integer(appdefTypeId);

        if(appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM){
            PlatformManagerLocal pmLocal;

            pmLocal = this.getPlatformMgrLocal();
            return pmLocal.findPlatformTypeById(id);
        } else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER){
            ServerManagerLocal smLocal;

            smLocal = this.getServerMgrLocal();
            try {
                return smLocal.findServerTypeById(id);
            } catch(FinderException exc){
                throw new ServerNotFoundException("server type id=" + 
                                                  appdefTypeId + 
                                                  " not found");
            }
        } else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
            ServiceManagerLocal vmLocal;

            vmLocal = this.getServiceMgrLocal();
            try {
                return vmLocal.findServiceTypeById(id);
            } catch(FinderException exc){
                throw new ServiceNotFoundException("service type id=" +
                                                   appdefTypeId +
                                                   " not found");
            }
        } else if(appdefType == 
                  AppdefEntityConstants.APPDEF_TYPE_APPLICATION)
        {
            ApplicationManagerLocal amLocal;
            
            amLocal = this.getApplicationMgrLocal();
            try {
                return amLocal.findApplicationTypeById(id);
            } catch(FinderException exc){
                throw new ApplicationNotFoundException("app type id=" +
                                                       appdefTypeId + 
                                                       "not found");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:"+
                                               " " + appdefType);
        }
    }

    protected AppdefResourceType findResourceType(TypeInfo info)
    {
        int type = info.getType();

        if(type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM){
            return getPlatformTypeDAO().findByName(info.getName());
        } else if(type == AppdefEntityConstants.APPDEF_TYPE_SERVER){
            return getServerTypeDAO().findByName(info.getName());
        } else if(type == AppdefEntityConstants.APPDEF_TYPE_SERVICE){
            return getServiceTypeDAO().findByName(info.getName());
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:"+
                                               " " + info);
        }
    }
}
