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

package org.hyperic.hq.bizapp.server.session;

import java.rmi.RemoteException;
import javax.ejb.SessionContext;

import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefStatManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.AppdefStatManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.auth.shared.AuthManagerLocal;
import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.autoinventory.server.session.AutoinventoryManagerEJBImpl;
import org.hyperic.hq.bizapp.shared.AIBossLocal;
import org.hyperic.hq.bizapp.shared.AIBossUtil;
import org.hyperic.hq.bizapp.shared.AppdefBossLocal;
import org.hyperic.hq.bizapp.shared.AppdefBossUtil;
import org.hyperic.hq.bizapp.shared.AuthzBossLocal;
import org.hyperic.hq.bizapp.shared.AuthzBossUtil;
import org.hyperic.hq.bizapp.shared.ControlBossLocal;
import org.hyperic.hq.bizapp.shared.ControlBossUtil;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.bizapp.shared.MeasurementBossLocal;
import org.hyperic.hq.bizapp.shared.MeasurementBossUtil;
import org.hyperic.hq.bizapp.shared.ProductBossLocal;
import org.hyperic.hq.bizapp.shared.ProductBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.control.shared.ControlManagerLocal;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.control.server.session.ControlManagerEJBImpl;
import org.hyperic.hq.control.server.session.ControlScheduleManagerEJBImpl;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.ReportProcessorLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.TrackerManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.ReportProcessorEJBImpl;
import org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.TrackerManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.SRNManagerEJBImpl;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;

public abstract class BizappSessionEJB {

    protected SessionContext ctx;
    
    public EventsBossLocal getEventsBoss() {
        try {
            return EventsBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException();
        }
    }

    public MeasurementBossLocal getMeasurementBoss() {
        try {
            return MeasurementBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException();
        }
    }
    
    public ProductBossLocal getProductBoss() {
        try {
            return ProductBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException();
        }
    }    

    public AuthzBossLocal getAuthzBoss() {
        try {
            return AuthzBossUtil.getLocalHome().create();
        } catch (Exception exc) {
            throw new SystemException(exc);
        }
    }

    public AIBossLocal getAIBoss() {

        try {
            return AIBossUtil.getLocalHome().create();
        } catch (Exception exc) {
            throw new SystemException(exc);
        }
    }

    public AppdefBossLocal getAppdefBoss() {
        try {
            return AppdefBossUtil.getLocalHome().create();
        } catch (Exception exc) {
            throw new SystemException(exc);
        }
    }

    public ControlBossLocal getControlBoss() {
        try {
            return ControlBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public ReportProcessorLocal getReportProcessor() {
        return ReportProcessorEJBImpl.getOne();
    }

    public CPropManagerLocal getCPropManager() {
        return CPropManagerEJBImpl.getOne();
    }

    public ConfigManagerLocal getConfigManager() {
        return ConfigManagerEJBImpl.getOne();
    }

    public ServerConfigManagerLocal getServerConfigManager() {
        return ServerConfigManagerEJBImpl.getOne();
    }

    public ResourceManagerLocal getResourceManager() {
        return ResourceManagerEJBImpl.getOne();
    }

    public ResourceGroupManagerLocal getResourceGroupManager() {
        return ResourceGroupManagerEJBImpl.getOne();
    }

    public AppdefStatManagerLocal getAppdefStatManager() {
        return AppdefStatManagerEJBImpl.getOne();
    }    
    
    public AuthzSubjectManagerLocal getAuthzSubjectManager() {
        return AuthzSubjectManagerEJBImpl.getOne();
    }

    public AutoinventoryManagerLocal getAutoInventoryManager() {
        return AutoinventoryManagerEJBImpl.getOne();
    }

    public ServerManagerLocal getServerManager() {
        return ServerManagerEJBImpl.getOne();
    }

    public ServiceManagerLocal getServiceManager() {
        return ServiceManagerEJBImpl.getOne();
    }

    public PlatformManagerLocal getPlatformManager() {
        return PlatformManagerEJBImpl.getOne();
    }

    public ProductManagerLocal getProductManager() {
        return ProductManagerEJBImpl.getOne();
    }

    public TemplateManagerLocal getTemplateManager() {
        return TemplateManagerEJBImpl.getOne();
    }

    public MeasurementManagerLocal getMetricManager() {
        return MeasurementManagerEJBImpl.getOne();
    }

    public ApplicationManagerLocal getApplicationManager() {
        return ApplicationManagerEJBImpl.getOne();
    }
    
    public AgentManagerLocal getAgentManager() {
        return AgentManagerEJBImpl.getOne();
    }
    
    public AuthManagerLocal getAuthManager() {
        return AuthManagerEJBImpl.getOne();
    }

    public AvailabilityManagerLocal getAvailManager() {
        return AvailabilityManagerEJBImpl.getOne();
    }

    public DataManagerLocal getDataMan() {
        return DataManagerEJBImpl.getOne();
    }

    protected TrackerManagerLocal getTrackerManager() {
        return TrackerManagerEJBImpl.getOne();
    }

    public ControlManagerLocal getControlManager() {
        return ControlManagerEJBImpl.getOne();
    }

    public ControlScheduleManagerLocal getControlScheduleManager() {
        return ControlScheduleManagerEJBImpl.getOne();
    }

    public SRNManagerLocal getSrnManager() {
        return SRNManagerEJBImpl.getOne();
    }
    
    /**
     * Get the overlord. This method should be used by any bizapp session
     * bean which wants to call an authz bound method while bypassing the check
     * use with discretion.
     */
    protected AuthzSubject getOverlord() {
        return getAuthzSubjectManager().getOverlordPojo();
    }

    protected AuthzSubjectValue getOverlordVal() {
        return getOverlord().getAuthzSubjectValue();
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
}
