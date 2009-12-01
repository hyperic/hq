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

package org.hyperic.hq.bizapp.server.session;

import java.rmi.RemoteException;

import javax.ejb.SessionContext;

import org.hyperic.hq.appdef.server.session.AIQueueManagerImpl;
import org.hyperic.hq.appdef.server.session.AgentManagerImpl;
import org.hyperic.hq.appdef.server.session.AppdefStatManagerImpl;
import org.hyperic.hq.appdef.server.session.ApplicationManagerImpl;
import org.hyperic.hq.appdef.server.session.CPropManagerImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerImpl;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.auth.server.session.AuthManagerImpl;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.server.session.AutoinventoryManagerImpl;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerImpl;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.control.server.session.ControlManagerImpl;
import org.hyperic.hq.control.server.session.ControlScheduleManagerImpl;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerImpl;
import org.hyperic.hq.measurement.server.session.DataManagerImpl;
import org.hyperic.hq.measurement.server.session.MeasurementManagerImpl;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.hyperic.hq.measurement.server.session.SRNManagerImpl;
import org.hyperic.hq.measurement.server.session.TemplateManagerImpl;
import org.hyperic.hq.measurement.server.session.TrackerManagerImpl;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.ReportProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.server.session.ProductManagerImpl;
import org.hyperic.hq.product.shared.ProductManager;

public abstract class BizappSessionEJB {

    protected SessionContext ctx;
    
    public EventsBoss getEventsBoss() {
       return EventsBossImpl.getOne();
    }

    public MeasurementBoss getMeasurementBoss() {
        try {
            return Bootstrap.getBean(MeasurementBoss.class);
        } catch (Exception e) {
            throw new SystemException();
        }
    }
    
    public ProductBoss getProductBoss() {
        return ProductBossImpl.getOne();
    }    

    public AuthzBoss getAuthzBoss() {
        return Bootstrap.getBean(AuthzBoss.class);
    }

    public AIBoss getAIBoss() {

        try {
            return AIBossImpl.getOne();
        } catch (Exception exc) {
            throw new SystemException(exc);
        }
    }

    public AppdefBoss getAppdefBoss() {
       return Bootstrap.getBean(AppdefBoss.class);
    }

    public ControlBoss getControlBoss() {
        try {
            return ControlBossImpl.getOne();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public ReportProcessor getReportProcessor() {
        return ReportProcessorImpl.getOne();
    }

    public CPropManager getCPropManager() {
        return CPropManagerImpl.getOne();
    }

    public ConfigManager getConfigManager() {
        return ConfigManagerImpl.getOne();
    }

    public ServerConfigManager getServerConfigManager() {
        return ServerConfigManagerImpl.getOne();
    }

    public ResourceManager getResourceManager() {
        return ResourceManagerImpl.getOne();
    }

    public ResourceGroupManager getResourceGroupManager() {
        return ResourceGroupManagerImpl.getOne();
    }

    public AppdefStatManager getAppdefStatManager() {
        return AppdefStatManagerImpl.getOne();
    }    
    
    public AuthzSubjectManager getAuthzSubjectManager() {
        return AuthzSubjectManagerImpl.getOne();
    }

    public AutoinventoryManager getAutoInventoryManager() {
        return AutoinventoryManagerImpl.getOne();
    }

    public ServerManager getServerManager() {
        return ServerManagerImpl.getOne();
    }

    public ServiceManager getServiceManager() {
        return ServiceManagerImpl.getOne();
    }

    public PlatformManager getPlatformManager() {
        return PlatformManagerImpl.getOne();
    }

    public ProductManager getProductManager() {
        return ProductManagerImpl.getOne();
    }

    public TemplateManager getTemplateManager() {
        return TemplateManagerImpl.getOne();
    }

    public MeasurementManager getMetricManager() {
        return MeasurementManagerImpl.getOne();
    }

    public ApplicationManager getApplicationManager() {
        return ApplicationManagerImpl.getOne();
    }
    
    public AgentManager getAgentManager() {
        return AgentManagerImpl.getOne();
    }
    
    public AuthManager getAuthManager() {
        return AuthManagerImpl.getOne();
    }

    public AvailabilityManager getAvailManager() {
        return AvailabilityManagerImpl.getOne();
    }

    public DataManager getDataMan() {
        return DataManagerImpl.getOne();
    }

    protected TrackerManager getTrackerManager() {
        return TrackerManagerImpl.getOne();
    }

    public ControlManager getControlManager() {
        return ControlManagerImpl.getOne();
    }

    public ControlScheduleManager getControlScheduleManager() {
        return ControlScheduleManagerImpl.getOne();
    }

    public SRNManager getSrnManager() {
        return SRNManagerImpl.getOne();
    }
    
    /**
     * Get the overlord. This method should be used by any bizapp session
     * bean which wants to call an authz bound method while bypassing the check
     * use with discretion.
     */
    protected AuthzSubject getOverlord() {
        return getAuthzSubjectManager().getOverlordPojo();
    }

    public void setSessionContext(SessionContext aCtx) throws RemoteException {
        ctx = aCtx;
    }

    protected SessionContext getSessionContext() {
        return ctx;
    }

    /**
     * Generic method to force rollback of current transaction.
     * will not call rollback if the tx is already marked for rollback
     */
    protected void rollback() {
        if(!getSessionContext().getRollbackOnly()) {
            getSessionContext().setRollbackOnly();
        }
    }

    protected AIQueueManager getAIManager() {
        return AIQueueManagerImpl.getOne();
    }
}
