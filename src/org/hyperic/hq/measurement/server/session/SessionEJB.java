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

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.hq.product.shared.ProductManagerLocal;

/** 
 *This is the base class to Measurement Session EJB's
 */
public abstract class SessionEJB {
    protected static final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    // Error strings
    private final String ERR_START = "Begin and end times must be positive";
    private final String ERR_END   = "Start time must be earlier than end time";

    protected static MeasurementPluginManager _mpm = null;

    private DataManagerLocal _dataMan;
    private AgentManagerLocal _agentMan;
    private ProductManagerLocal _prodMan;
    private AuthzSubjectManagerLocal _ssmLocal;
    private TemplateManagerLocal _templateMan;
    private SRNManagerLocal s_rnManager;

    private InitialContext _ic;

    protected BaselineDAO getBaselineDAO() {
        return new BaselineDAO(DAOFactory.getDAOFactory());
    }

    protected CategoryDAO getCategoryDAO() {
        return new CategoryDAO(DAOFactory.getDAOFactory());
    }

    protected AvailabilityDataDAO getAvailabilityDataDAO() {
        return new AvailabilityDataDAO(DAOFactory.getDAOFactory());
    }

    protected MeasurementDAO getMeasurementDAO() {
        return new MeasurementDAO(DAOFactory.getDAOFactory());
    }

    protected MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return new MeasurementTemplateDAO(DAOFactory.getDAOFactory());
    }

    protected MetricProblemDAO getMetricProblemDAO() {
        return new MetricProblemDAO(DAOFactory.getDAOFactory());
    }
    
    protected MonitorableTypeDAO getMonitorableTypeDAO() {
        return new MonitorableTypeDAO(DAOFactory.getDAOFactory());
    }

    protected ScheduleRevNumDAO getScheduleRevNumDAO() {
        return new ScheduleRevNumDAO(DAOFactory.getDAOFactory());
    }

    // Exposed accessor methods
    protected TemplateManagerLocal getTemplateMan(){
        if (_templateMan == null) {
            _templateMan = TemplateManagerEJBImpl.getOne();
        }
        return _templateMan;
    }

    protected AuthzSubjectManagerLocal getAuthzSubjectManager() {
        if (_ssmLocal == null) {
            _ssmLocal = AuthzSubjectManagerEJBImpl.getOne();
        }
        return _ssmLocal;
    }

    protected DataManagerLocal getDataMan() {
        if (_dataMan == null) {
            _dataMan = DataManagerEJBImpl.getOne();
        }
        return _dataMan;
    }

    protected AgentManagerLocal getAgentMan() {
        if (_agentMan == null) {
            _agentMan = AgentManagerEJBImpl.getOne();
        }
        return _agentMan;
    }

    protected ProductManagerLocal getProductMan() {
        if (_prodMan == null) {
            _prodMan = ProductManagerEJBImpl.getOne();
        }
        return _prodMan;
    }

    protected MeasurementPluginManager getMPM() {
        if (_mpm == null) {
            ProductManagerLocal ppm = this.getProductMan();

            try {
                _mpm = (MeasurementPluginManager)
                    ppm.getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            } catch (PluginException e) {
                throw new SystemException("PluginException: " + e.getMessage(),
                                          e);
            }
        }
        return _mpm;
    }

    protected SRNManagerLocal getSRNManager() {
        if (s_rnManager == null) {
            s_rnManager = SRNManagerEJBImpl.getOne();
        }
        return s_rnManager;
    }

    protected InitialContext getInitialContext() {
        if (_ic == null) {
            try {
                _ic = new InitialContext();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return _ic;
    }

    protected Agent getAgent(AppdefEntityID id)
        throws MonitorAgentException {
        // Ask the AgentManager for the AgentConnection
        try {
            return this.getAgentMan().getAgent(id);
        } catch (AgentNotFoundException e) {
            throw new MonitorAgentException(e);
        }
    }

    protected Agent getAgent(String agentToken)
        throws MonitorAgentException {
        // Ask the AgentManager for the AgentConnection
        try {
            return this.getAgentMan().getAgent(agentToken);
        } catch (AgentNotFoundException e) {
            throw new MonitorAgentException(e);
        }
    }

    private void checkPermission(Integer subjectId,
                                 AppdefEntityID id,
                                 String resType,
                                 String opName)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(subjectId, resType, id.getId(), opName);
    }
    
    /**
     * Check for modify permission for a given resource
     */
    protected void checkModifyPermission(Integer subjectId, AppdefEntityID id)
        throws PermissionException {
        String opName = null;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpMonitorPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpMonitorServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpMonitorService;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                                                     id.getType());
        }
        
        checkPermission(subjectId, id, id.getAuthzTypeName(), opName);
    }

    /**
     * Check for modify permission for a given resource
     */
    protected void checkDeletePermission(Integer subjectId,
                                         AppdefEntityID id)
        throws PermissionException {
        String resType = null;
        String opName = null;

        int type = id.getType();
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resType = AuthzConstants.platformResType;
                opName = AuthzConstants.platformOpRemovePlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resType = AuthzConstants.serverResType;
                opName = AuthzConstants.serverOpRemoveServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resType = AuthzConstants.serviceResType;
                opName = AuthzConstants.serviceOpRemoveService;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        
        checkPermission(subjectId, id, resType, opName);
    }
    
    protected void checkTimeArguments(long begin, long end)
        throws IllegalArgumentException {
        if (begin > end)
            throw new IllegalArgumentException(this.ERR_END);

        if (begin < 0)
            throw new IllegalArgumentException(this.ERR_START);
    }

    protected void deleteMetricProblems(Collection mids) {
        getMetricProblemDAO().deleteByMetricIds(mids);
    }

    protected Resource getResource(AppdefEntityID id) {
        return ResourceManagerEJBImpl.getOne().findResource(id);
    }
}
