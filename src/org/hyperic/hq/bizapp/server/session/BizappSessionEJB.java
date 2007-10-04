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

import javax.ejb.CreateException;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefStatManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefStatManagerUtil;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerUtil;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.CPropManagerUtil;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.auth.shared.AuthManagerLocal;
import org.hyperic.hq.auth.shared.AuthManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerUtil;
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
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.control.shared.ControlManagerLocal;
import org.hyperic.hq.control.shared.ControlManagerUtil;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.control.shared.ControlScheduleManagerUtil;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.ReportProcessorLocal;
import org.hyperic.hq.measurement.shared.ReportProcessorUtil;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.hyperic.hq.measurement.shared.TrackerManagerLocal;
import org.hyperic.hq.measurement.shared.TrackerManagerUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.product.shared.ProductManagerUtil;

public abstract class BizappSessionEJB {
    private AIBossLocal                    aibossLocal;
    private AppdefBossLocal                appdefbossLocal;
    private AuthzBossLocal                 authzbossLocal;
    private EventsBossLocal                eventsbossLocal;
    private MeasurementBossLocal           measurementBossLocal;
    private ProductBossLocal               productBossLocal;
    private ApplicationManagerLocal        appmLocal;
    private AgentManagerLocal              amLocal;
    private AppdefGroupManagerLocal        agmLocal;
    private AppdefStatManagerLocal         asmLocal;
    private AuthManagerLocal               authmLocal;
    private AutoinventoryManagerLocal      aimLocal;
    private CPropManagerLocal              cpmLocal;
    private ServerConfigManagerLocal       svrCfgMgrLocal;
    private ConfigManagerLocal             confmLocal;
    private DerivedMeasurementManagerLocal dmmLocal;
    private ProductManagerLocal            pmLocal;
    private RawMeasurementManagerLocal     rmmLocal;
    private PlatformManagerLocal           platformmLocal;
    private ServerManagerLocal             servermLocal;
    private ServiceManagerLocal            servicemLocal;
    private ResourceManagerLocal           resourcemLocal;
    private ResourceGroupManagerLocal      resourcegLocal;
    private AuthzSubjectManagerLocal       ssmLocal;
    private AuthzSubjectValue              overlord;
    private TemplateManagerLocal           tmLocal;
    private TrackerManagerLocal            trackerLocal;
    private ReportProcessorLocal           rpLocal;
    private DataManagerLocal               dataMan;
    private ControlBossLocal               controlBossLocal;
    private ControlManagerLocal            cmLocal;
    private ControlScheduleManagerLocal    controlScheduleManager;
    private SRNManagerLocal                srnManager;

    protected SessionContext ctx;
    
    public EventsBossLocal getEventsBoss() {
        if(eventsbossLocal == null) {
            try {
                eventsbossLocal = EventsBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return eventsbossLocal;
    }

    public MeasurementBossLocal getMeasurementBoss() {
        if(measurementBossLocal == null) {
            try {
                measurementBossLocal =
                    MeasurementBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return measurementBossLocal;
    }
    
    public ProductBossLocal getProductBoss() {
        if(productBossLocal == null) {
            try {
                productBossLocal = ProductBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return productBossLocal;
    }    
    
    public ReportProcessorLocal getReportProcessor() {
        if(rpLocal == null){
            try {
                rpLocal = ReportProcessorUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return rpLocal;
    }

    public CPropManagerLocal getCPropManager() {
        if(cpmLocal == null){
            try {
                cpmLocal = 
                    CPropManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return cpmLocal;
    }

    public ConfigManagerLocal getConfigManager() {
        if(confmLocal == null){
            try {
                confmLocal = 
                    ConfigManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return confmLocal;
    }

    public ServerConfigManagerLocal getServerConfigManager() {
        if(svrCfgMgrLocal == null){
            try {
                svrCfgMgrLocal = 
                    ServerConfigManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return svrCfgMgrLocal;
    }

    public ResourceManagerLocal getResourceManager() {
        if(resourcemLocal == null){
            try {
                resourcemLocal = 
                    ResourceManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return resourcemLocal;
    }

    public ResourceGroupManagerLocal getResourceGroupManager() {
        if(resourcegLocal == null){
            try {
                resourcegLocal = 
                    ResourceGroupManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return resourcegLocal;
    }

    public AppdefStatManagerLocal getAppdefStatManager() {
        if(asmLocal == null){
            try {
                asmLocal = 
                    AppdefStatManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return asmLocal;
    }    
    
    public AuthzSubjectManagerLocal getAuthzSubjectManager() {
        if(ssmLocal == null){
            try {
                ssmLocal = 
                    AuthzSubjectManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return ssmLocal;
    }

    public AuthzBossLocal getAuthzBoss() {
        if(authzbossLocal == null){
            try {
                authzbossLocal = AuthzBossUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return authzbossLocal;
    }

    public AutoinventoryManagerLocal getAutoInventoryManager() {
        if(aimLocal == null){
            try {
                aimLocal = 
                    AutoinventoryManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return aimLocal;
    }

    public ServerManagerLocal getServerManager() {
        if(servermLocal == null){
            try {
                servermLocal = ServerManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return servermLocal;
    }

    public ServiceManagerLocal getServiceManager() {
        if(servicemLocal == null){
            try {
                servicemLocal = ServiceManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return servicemLocal;
    }

    public PlatformManagerLocal getPlatformManager() {
        if(platformmLocal == null){
            try {
                platformmLocal = PlatformManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return platformmLocal;
    }

    public AppdefGroupManagerLocal getAppdefGroupManager() {
        if(agmLocal == null){
            try {
                agmLocal = AppdefGroupManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return agmLocal;
    }

    public ProductManagerLocal getProductManager() {
        if(pmLocal == null){
            try {
                pmLocal = ProductManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return pmLocal;
    }

    public TemplateManagerLocal getTemplateManager() {
        if(tmLocal == null){
            try {
                tmLocal = TemplateManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return tmLocal;
    }

    public DerivedMeasurementManagerLocal getMetricManager() {
        if(dmmLocal == null){
            try {
                dmmLocal = 
                    DerivedMeasurementManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return dmmLocal;
    }

    public RawMeasurementManagerLocal getRawMeasurementManager() {
        if(rmmLocal == null){
            try {
                rmmLocal = 
                    RawMeasurementManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return rmmLocal;
    }

    public AIBossLocal getAIBoss() {
        if(aibossLocal == null){
            try {
                aibossLocal = AIBossUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return aibossLocal;
    }
    
    public AppdefBossLocal getAppdefBoss() {
        if(appdefbossLocal == null){
            try {
                appdefbossLocal = AppdefBossUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return appdefbossLocal;
    }
    
    public ApplicationManagerLocal getApplicationManager() {
        if(appmLocal == null){
            try {
                appmLocal = ApplicationManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return appmLocal;
    }
    
    public AgentManagerLocal getAgentManager() {
        if(amLocal == null){
            try {
                amLocal = AgentManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return amLocal;
    }
    
    public AuthManagerLocal getAuthManager() {
        if(authmLocal == null){
            try {
                authmLocal = AuthManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return authmLocal;
    }

    public DataManagerLocal getDataMan() {
        if (dataMan == null) {
            try {
                dataMan = DataManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return dataMan;
    }

    protected TrackerManagerLocal getTrackerManager() {
        if (trackerLocal == null) {
            try {
                trackerLocal = TrackerManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }

        return trackerLocal;
    }

    /**
     * Get the overlord. This method should be used by any bizapp session
     * bean which wants to call an authz bound method while bypassing the check
     * use with discretion.
     */
    protected AuthzSubjectValue getOverlord() {
        if (overlord == null) {
            try {
                overlord = getAuthzSubjectManager().findOverlord();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return overlord;
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

    public ControlBossLocal getControlBoss() {
        if(controlBossLocal == null) {
            try {
                controlBossLocal = ControlBossUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return controlBossLocal;
    }

    public ControlManagerLocal getControlManager() {
        if(cmLocal == null){
            try {
                cmLocal = ControlManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return cmLocal;
    }

    public ControlScheduleManagerLocal getControlScheduleManager() {
        if (controlScheduleManager == null) {
            try {
                controlScheduleManager =
                    ControlScheduleManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return controlScheduleManager;
    }

    public SRNManagerLocal getSrnManager() {
        if (srnManager == null) {
            try {
                srnManager = SRNManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return srnManager;
    }
}
