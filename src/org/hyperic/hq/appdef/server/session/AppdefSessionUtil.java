/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.TypeInfo;

public abstract class AppdefSessionUtil {
    private AIQueueManager aiqManagerLocal;
    private ConfigManager configMgrL;
    private ResourceManager rmLocal;
    private CPropManager cpropLocal;
    // TODO: Remove protected accessor when all subclasses are converted
    protected AgentDAO agentDao = Bootstrap.getBean(AgentDAO.class);
    protected ApplicationDAO applicationDAO = Bootstrap.getBean(ApplicationDAO.class);
    protected ConfigResponseDAO configResponseDAO = Bootstrap.getBean(ConfigResponseDAO.class);
    protected PlatformDAO platformDao = Bootstrap.getBean(PlatformDAO.class);
    protected PlatformTypeDAO platformTypeDAO = Bootstrap.getBean(PlatformTypeDAO.class);
    protected ServerDAO serverDao = Bootstrap.getBean(ServerDAO.class);
    protected ServerTypeDAO serverTypeDAO = Bootstrap.getBean(ServerTypeDAO.class);
    protected ServiceTypeDAO serviceTypeDAO = Bootstrap.getBean(ServiceTypeDAO.class);
    protected ServiceDAO serviceDao = Bootstrap.getBean(ServiceDAO.class);

    protected CPropManager getCPropManager() {
        if (cpropLocal == null) {
            cpropLocal = CPropManagerImpl.getOne();
        }
        return cpropLocal;
    }

    protected ConfigManager getConfigManager() {
        if (configMgrL == null) {
            configMgrL = ConfigManagerImpl.getOne();
        }
        return configMgrL;
    }

    protected ApplicationManager getApplicationManager() {
        return ApplicationManagerImpl.getOne();
    }

    protected PlatformManagerLocal getPlatformManager() {
        return PlatformManagerEJBImpl.getOne();
    }

    protected ServerManagerLocal getServerManager() {
        return ServerManagerEJBImpl.getOne();
    }

    protected ServiceManagerLocal getServiceManager() {
        return ServiceManagerEJBImpl.getOne();
    }

    protected ResourceManager getResourceManager() {
        if (rmLocal == null) {
            rmLocal = ResourceManagerImpl.getOne();
        }
        return rmLocal;
    }

    protected AIQueueManager getAIQManagerLocal() {
        if (aiqManagerLocal == null) {
            aiqManagerLocal = AIQueueManagerImpl.getOne();
        }
        return aiqManagerLocal;
    }

    protected AgentDAO getAgentDAO() {
        return agentDao;
    }

    protected ConfigResponseDAO getConfigResponseDAO() {
        return configResponseDAO;
    }

    protected PlatformDAO getPlatformDAO() {
        return platformDao;
    }

    protected PlatformTypeDAO getPlatformTypeDAO() {
        return platformTypeDAO;
    }

    protected ServerDAO getServerDAO() {
        return serverDao;
    }

    protected ServerTypeDAO getServerTypeDAO() {
        return serverTypeDAO;
    }

    protected ServiceTypeDAO getServiceTypeDAO() {
        return serviceTypeDAO;
    }

    protected ServiceDAO getServiceDAO() {
        return serviceDao;
    }

    protected ApplicationDAO getApplicationDAO() {
        return applicationDAO;
    }

    protected AppdefResourceType findResourceType(int appdefType,
                                                  int appdefTypeId)
        throws AppdefEntityNotFoundException {
        Integer id = new Integer(appdefTypeId);

        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return getPlatformManager().findPlatformType(id);
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            try {
                return getServerManager().findServerType(id);
            } catch (ObjectNotFoundException exc) {
                throw new ServerNotFoundException("Server type id=" +
                                                  appdefTypeId +
                                                  " not found");
            }
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            try {
                return getServiceManager().findServiceType(id);
            } catch (ObjectNotFoundException exc) {
                throw new ServiceNotFoundException("Service type id=" +
                                                   appdefTypeId +
                                                   " not found");
            }
        } else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
            return getApplicationManager().findApplicationType(id);
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:" +
                                               " " + appdefType);
        }
    }

    protected AppdefResourceType findResourceType(TypeInfo info) {
        int type = info.getType();

        if (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return getPlatformTypeDAO().findByName(info.getName());
        } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            return getServerTypeDAO().findByName(info.getName());
        } else if (type == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            return getServiceTypeDAO().findByName(info.getName());
        } else {
            throw new IllegalArgumentException("Unrecognized appdef type:" +
                                               " " + info);
        }
    }
}
