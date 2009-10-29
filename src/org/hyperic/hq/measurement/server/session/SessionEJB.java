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

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManager;

/**
 *This is the base class to Measurement Session EJB's
 */
public abstract class SessionEJB {
    protected static final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    // Error strings
    private final String ERR_START = "Begin and end times must be positive";
    private final String ERR_END   = "Start time must be earlier than end time";

    protected static MeasurementPluginManager measurementPluginManager = null;

    protected AvailabilityDataDAO availabilityDataDAO = Bootstrap.getBean(AvailabilityDataDAO.class);
    protected BaselineDAO baselineDAO = Bootstrap.getBean(BaselineDAO.class);
    protected CategoryDAO categoryDAO = Bootstrap.getBean(CategoryDAO.class); 
    protected MeasurementDAO measurementDAO = Bootstrap.getBean(MeasurementDAO.class);
    protected MeasurementTemplateDAO measurementTemplateDAO = Bootstrap.getBean(MeasurementTemplateDAO.class);
    protected MetricProblemDAO metricProblemDAO = Bootstrap.getBean(MetricProblemDAO.class);
    protected MonitorableTypeDAO monitorableTypeDAO = Bootstrap.getBean(MonitorableTypeDAO.class);
    protected ScheduleRevNumDAO scheduleRevNumDAO = Bootstrap.getBean(ScheduleRevNumDAO.class);

    private InitialContext _ic;

    protected BaselineDAO getBaselineDAO() {
        return baselineDAO;
    }

    protected CategoryDAO getCategoryDAO() {
        return categoryDAO;
    }

    protected AvailabilityDataDAO getAvailabilityDataDAO() {
        return availabilityDataDAO;
    }

    protected MeasurementDAO getMeasurementDAO() {
        return measurementDAO;
    }

    protected MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return measurementTemplateDAO;
    }

    protected MetricProblemDAO getMetricProblemDAO() {
        return metricProblemDAO;
    }

    protected MonitorableTypeDAO getMonitorableTypeDAO() {
        return monitorableTypeDAO;
    }

    protected ScheduleRevNumDAO getScheduleRevNumDAO() {
        return scheduleRevNumDAO;
    }
    
    private AgentManager getAgentManager() {
        return Bootstrap.getBean(AgentManager.class);
    }

    private ProductManager getProductManager() {
        return Bootstrap.getBean(ProductManager.class);
    }
    protected MeasurementPluginManager getMPM() {
        if (measurementPluginManager == null) {
            try {
                measurementPluginManager = (MeasurementPluginManager)
                    getProductManager().getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            } catch (PluginException e) {
                throw new SystemException("PluginException: " + e.getMessage(),
                                          e);
            }
        }
        return measurementPluginManager;
    }

    protected SRNManagerLocal getSRNManager() {
        return Bootstrap.getBean(SRNManagerLocal.class);
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
            return getAgentManager().getAgent(id);
        } catch (AgentNotFoundException e) {
            throw new MonitorAgentException(e);
        }
    }

    protected Agent getAgent(String agentToken)
        throws MonitorAgentException {
        // Ask the AgentManager for the AgentConnection
        try {
            return getAgentManager().getAgent(agentToken);
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
                opName = AuthzConstants.platformOpModifyPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpModifyServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpModifyService;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpModifyResourceGroup;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                                                     id.getType());
        }

        checkPermission(subjectId, id, id.getAuthzTypeName(), opName);
    }

  
    protected void checkTimeArguments(long begin, long end)
        throws IllegalArgumentException {
        if (begin > end)
            throw new IllegalArgumentException(this.ERR_END);

        if (begin < 0)
            throw new IllegalArgumentException(this.ERR_START);
    }

    protected void deleteMetricProblems(Collection<Integer> mids) {
        getMetricProblemDAO().deleteByMetricIds(mids);
    }

    protected Resource getResource(AppdefEntityID id) {
        return ResourceManagerImpl.getOne().findResource(id);
    }
}
