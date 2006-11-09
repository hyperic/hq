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
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.bizapp.shared.MeasurementBossLocal;
import org.hyperic.hq.bizapp.shared.MeasurementBossUtil;
import org.hyperic.hq.bizapp.shared.ProductBossLocal;
import org.hyperic.hq.bizapp.shared.ProductBossUtil;
import org.hyperic.hq.bizapp.shared.AuthzBossLocal;
import org.hyperic.hq.bizapp.shared.AuthzBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
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
    private DataManagerLocal               dataMan = null;

    protected SessionContext ctx;
    
    public EventsBossLocal getEventsBoss() {
        if(this.eventsbossLocal == null) {
            try {
                this.eventsbossLocal = EventsBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return this.eventsbossLocal;
    }
    
    public MeasurementBossLocal getMeasurementBoss() {
        if(this.measurementBossLocal == null) {
            try {
                this.measurementBossLocal = MeasurementBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return this.measurementBossLocal;
    }
    
    public ProductBossLocal getProductBoss() {
        if(this.productBossLocal == null) {
            try {
                this.productBossLocal = ProductBossUtil.getLocalHome().create();                
            } catch (Exception e) {
                throw new SystemException();
            }
        }
        return productBossLocal;
    }    
    
    public ReportProcessorLocal getReportProcessor() {
        if(this.rpLocal == null){
            try {
                this.rpLocal = ReportProcessorUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.rpLocal;
    }

    public CPropManagerLocal getCPropManager() {
        if(this.cpmLocal == null){
            try {
                this.cpmLocal = 
                    CPropManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.cpmLocal;
    }

    public ConfigManagerLocal getConfigManager() {
        if(this.confmLocal == null){
            try {
                this.confmLocal = 
                    ConfigManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.confmLocal;
    }

    public ServerConfigManagerLocal getServerConfigManager() {
        if(this.svrCfgMgrLocal == null){
            try {
                this.svrCfgMgrLocal = 
                    ServerConfigManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.svrCfgMgrLocal;
    }

    public ResourceManagerLocal getResourceManager() {
        if(this.resourcemLocal == null){
            try {
                this.resourcemLocal = 
                    ResourceManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.resourcemLocal;
    }

    public ResourceGroupManagerLocal getResourceGroupManager() {
        if(this.resourcegLocal == null){
            try {
                this.resourcegLocal = 
                    ResourceGroupManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.resourcegLocal;
    }

    public AppdefStatManagerLocal getAppdefStatManager() {
        if(this.asmLocal == null){
            try {
                this.asmLocal = 
                    AppdefStatManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.asmLocal;
    }    
    
    public AuthzSubjectManagerLocal getAuthzSubjectManager() {
        if(this.ssmLocal == null){
            try {
                this.ssmLocal = 
                    AuthzSubjectManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.ssmLocal;
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
        if(this.aimLocal == null){
            try {
                this.aimLocal = 
                    AutoinventoryManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.aimLocal;
    }

    public ServerManagerLocal getServerManager() {
        if(this.servermLocal == null){
            try {
                this.servermLocal = ServerManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.servermLocal;
    }

    public ServiceManagerLocal getServiceManager() {
        if(this.servicemLocal == null){
            try {
                this.servicemLocal = ServiceManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.servicemLocal;
    }

    public PlatformManagerLocal getPlatformManager() {
        if(this.platformmLocal == null){
            try {
                this.platformmLocal = PlatformManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.platformmLocal;
    }

    public AppdefGroupManagerLocal getGroupManager() {
        return getAppdefGroupManager();
    }

    public AppdefGroupManagerLocal getAppdefGroupManager() {
        if(this.agmLocal == null){
            try {
                this.agmLocal = AppdefGroupManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.agmLocal;
    }

    public ProductManagerLocal getProductManager() {
        if(this.pmLocal == null){
            try {
                this.pmLocal = ProductManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.pmLocal;
    }

    public TemplateManagerLocal getTemplateManager() {
        if(this.tmLocal == null){
            try {
                this.tmLocal = TemplateManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.tmLocal;
    }

    public DerivedMeasurementManagerLocal getDerivedMeasurementManager() {
        if(this.dmmLocal == null){
            try {
                this.dmmLocal = 
                    DerivedMeasurementManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.dmmLocal;
    }

    public RawMeasurementManagerLocal getRawMeasurementManager() {
        if(this.rmmLocal == null){
            try {
                this.rmmLocal = 
                    RawMeasurementManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.rmmLocal;
    }

    public AIBossLocal getAIBoss() {
        if(this.aibossLocal == null){
            try {
                this.aibossLocal = AIBossUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.aibossLocal;
    }
    
    public AppdefBossLocal getAppdefBoss() {
        if(this.appdefbossLocal == null){
            try {
                this.appdefbossLocal = AppdefBossUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.appdefbossLocal;
    }
    
    public ApplicationManagerLocal getApplicationManager() {
        if(this.appmLocal == null){
            try {
                this.appmLocal = ApplicationManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.appmLocal;
    }
    
    public AgentManagerLocal getAgentManager() {
        if(this.amLocal == null){
            try {
                this.amLocal = AgentManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.amLocal;
    }
    
    public AuthManagerLocal getAuthManager() {
        if(this.authmLocal == null){
            try {
                this.authmLocal = AuthManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.authmLocal;
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
        if (this.trackerLocal == null) {
            try {
                this.trackerLocal = TrackerManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }

        return this.trackerLocal;
    }

    /**
     * Get the overlord. This method should be used by any bizapp session
     * bean which wants to call an authz bound method while bypassing the check
     * use with discretion.
     */
    protected AuthzSubjectValue getOverlord() {
        if (overlord == null) {
            try {
                overlord = this.getAuthzSubjectManager().findOverlord();
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
}
