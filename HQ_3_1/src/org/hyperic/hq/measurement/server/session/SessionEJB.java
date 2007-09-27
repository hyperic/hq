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

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.EvaluationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.data.AggregateObjectMeasurementValue;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.express.ExpressionManager;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.product.shared.ProductManagerUtil;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;

/** 
 *This is the base class to Measurement Session EJB's
 */
public abstract class SessionEJB {
    private static final Log log = LogFactory.getLog(SessionEJB.class);
    private static final Log timingLog =
        LogFactory.getLog(MeasurementConstants.MEA_TIMING_LOG);

    protected static final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    // Error strings
    private final String ERR_START = "Begin and end times must be positive";
    private final String ERR_END   = "Start time must be earlier than end time";

    protected static MeasurementPluginManager mpm = null;

    private DataManagerLocal dataMan;
    private AgentManagerLocal agentMan;
    private SchedulerLocal scheduler;
    private ProductManagerLocal prodMan;
    private AuthzSubjectManagerLocal ssmLocal;
    private TemplateManagerLocal templateMan;
    private SRNManagerLocal srnManager;

    private InitialContext ic;

    protected BaselineDAO getBaselineDAO() {
        return new BaselineDAO(DAOFactory.getDAOFactory());
    }

    protected CategoryDAO getCategoryDAO() {
        return DAOFactory.getDAOFactory().getCategoryDAO();
    }

    protected DerivedMeasurementDAO getDerivedMeasurementDAO() {
        return DAOFactory.getDAOFactory().getDerivedMeasurementDAO();
    }

    protected MeasurementArgDAO getMeasurementArgDAO() {
        return DAOFactory.getDAOFactory().getMeasurementArgDAO();
    }

    protected MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return DAOFactory.getDAOFactory().getMeasurementTemplateDAO();
    }

    protected MetricProblemDAO getMetricProblemDAO() {
        return DAOFactory.getDAOFactory().getMetricProblemDAO();
    }
    
    protected MonitorableTypeDAO getMonitorableTypeDAO() {
        return DAOFactory.getDAOFactory().getMonitorableTypeDAO();
    }

    protected RawMeasurementDAO getRawMeasurementDAO() {
        return DAOFactory.getDAOFactory().getRawMeasurementDAO();
    }
    
    protected ScheduleRevNumDAO getScheduleRevNumDAO() {
        return DAOFactory.getDAOFactory().getScheduleRevNumDAO();
    }

    // Exposed accessor methods
    protected TemplateManagerLocal getTemplateMan(){
        if (templateMan == null) {
            try {
                this.templateMan = TemplateManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.templateMan;
    }

    protected AuthzSubjectManagerLocal getAuthzSubjectManager() {
        if (ssmLocal == null) {
            try {
                this.ssmLocal =
                    AuthzSubjectManagerUtil.getLocalHome().create();
            } catch (Exception exc) {
                throw new SystemException(exc);
            }
        }
        return this.ssmLocal;
    }

    protected DataManagerLocal getDataMan() {
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

    protected AgentManagerLocal getAgentMan() {
        if (agentMan == null) {
            try {
                this.agentMan = AgentManagerUtil.getLocalHome().create();
            } catch (Exception exc) {
                throw new SystemException(exc);
            }
        }
        return this.agentMan;
    }

    protected ProductManagerLocal getProductMan() {
        if (prodMan == null) {
            try {
                this.prodMan = ProductManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return this.prodMan;
    }

    protected MeasurementPluginManager getMPM() {
        if (mpm == null) {
            ProductManagerLocal ppm = this.getProductMan();

            try {
                mpm = (MeasurementPluginManager)
                    ppm.getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            } catch (PluginException e) {
                throw new SystemException("PluginException: " + e.getMessage(),
                                          e);
            }
        }
        return mpm;
    }

    protected SchedulerLocal getScheduler() {
        if (scheduler == null) {
            try {
                scheduler = SchedulerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return scheduler;
    }

    protected SRNManagerLocal getSRNManager() {
        if (srnManager == null) {
            try {
                srnManager = SRNManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return srnManager;
    }

    protected InitialContext getInitialContext() {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return ic;
    }

    /**
     * Utility to get measurement ID's that comprise the arguments
     * of a DerivedMeasurement
     */
    protected Integer[] getArgumentIds(DerivedMeasurement dm)
        throws FinderException {
        Collection mcol = getRawMeasurementDAO().
            findByDerivedMeasurement(dm.getId());

        // Now make an array the size of the raw measurements
        Integer[] rawIds = new Integer[mcol.size()];
        int ind = 0;
        for (Iterator i = mcol.iterator(); i.hasNext(); ind++) {
            RawMeasurement rm = (RawMeasurement)i.next();
            rawIds[ind] = rm.getId();
        }

        // XXX - need to reorder the IDs according to the template args
        return rawIds;
    }

    protected AgentValue getAgentConnection(AppdefEntityID id)
        throws MonitorAgentException {
        // Ask the AgentManager for the AgentConnection
        try {
            return this.getAgentMan().getAgent(id);
        } catch (AgentNotFoundException e) {
            throw new MonitorAgentException(e);
        }
    }

    protected AgentValue getAgentConnection(String agentToken)
        throws MonitorAgentException {
        // Ask the AgentManager for the AgentConnection
        try {
            return this.getAgentMan().getAgent(agentToken);
        } catch (AgentNotFoundException e) {
            throw new MonitorAgentException(e);
        }
    }

    private void checkPermission(AuthzSubjectValue subject,
                                 AppdefEntityID id,
                                 String resType,
                                 String opName)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(subject.getId(), resType, id.getId(), opName);
    }
    
    /**
     * Check for modify permission for a given resource
     */
    protected void checkModifyPermission(AuthzSubjectValue subject,
                                         AppdefEntityID id)
        throws PermissionException {
        String resType = null;
        String opName = null;

        int type = id.getType();
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resType = AuthzConstants.platformResType;
                opName = AuthzConstants.platformOpModifyPlatform;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resType = AuthzConstants.serverResType;
                opName = AuthzConstants.serverOpModifyServer;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resType = AuthzConstants.serviceResType;
                opName = AuthzConstants.serviceOpModifyService;
                break;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
        
        checkPermission(subject, id, resType, opName);
    }

    /**
     * Check for modify permission for a given resource
     */
    protected void checkDeletePermission(AuthzSubjectValue subject,
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
        
        checkPermission(subject, id, resType, opName);
    }
    
    protected void checkTimeArguments(long begin, long end)
        throws IllegalArgumentException {
        if (begin > end)
            throw new IllegalArgumentException(this.ERR_END);

        if (begin < 0)
            throw new IllegalArgumentException(this.ERR_START);
    }

    /**
     * Performs the evaluation of the expression.
     **/
    protected Double evaluateExpression(DerivedMeasurement measurement,
                                        Map dataMap)
        throws EvaluationException {

        long evalStart = System.currentTimeMillis();
        Double result = MeasurementConstants.EXPR_EVAL_RESULT_DEFAULT;
        HashMap expValues = new HashMap();

        // Iterate and build the instantJ properties
        int idx = 0;
        for (Iterator i = dataMap.keySet().iterator(); i.hasNext();) {
            // dataMap contains MeasurementValues only.
            Integer key = (Integer) i.next();
            MetricValue mv = (MetricValue) dataMap.get(key);
            if (mv instanceof AggregateObjectMeasurementValue)
                expValues.put(
                    MeasurementConstants.TEMPL_IDENTITY_PFX + (++idx),
                    ((AggregateObjectMeasurementValue) mv).getAggArray());
            else
                expValues.put(
                    MeasurementConstants.TEMPL_IDENTITY_PFX + (++idx),
                    mv.getObjectValue());

            if (log.isDebugEnabled())
                if (mv instanceof AggregateObjectMeasurementValue)
                    log.debug("InstantJ Properties to receive: " +
                        MeasurementConstants.TEMPL_IDENTITY_PFX + idx + ":" +
                        ((AggregateObjectMeasurementValue) mv).getAggArray());
                else
                    log.debug("InstantJ Properties to receive: " +
                        MeasurementConstants.TEMPL_IDENTITY_PFX + (idx) + ":" +
                        mv);
        }

        // Get the expression through the factory for caching
        try {
            ExpressionManager expMgr = ExpressionManager.getInstance();

            /* If the expression is not memory cache, then attempt to
               deserialize it from the database version. If it cannot
               be deserialized, then mfg a new one and db store the
               serialized object.
            */
            if (log.isDebugEnabled())
                log.debug("SessionEJB- evaluating expression");

            long expmgrstart = System.currentTimeMillis();
            byte[] expressionData =
                measurement.getTemplate().getExpressionData();
            result =
                ExpressionManager.getInstance().evaluate(
                    measurement.getId(),
                    measurement.getTemplate().getTemplate(),
                    expValues,
                    MeasurementConstants.EXPMGR_PACKAGE_IMPORTS,
                    expressionData);
            logTime("expressionEvaluate-expmgreval", expmgrstart);

            if (expressionData == null) {
                if (log.isDebugEnabled())
                    log.debug("SessionEJB- caching expression");

                measurement.getTemplate().setExpressionData(
                    expMgr.getExpressionBytes(measurement.getId()));
            }

        } catch (Exception exc) {
            throw new EvaluationException(exc);
        }
        logTime("evaluateExpression", evalStart);
        return result;
    }
    
    private void logTime(String method, long start) {
        if (timingLog.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            timingLog.debug("SesionEJB." + method + "() - " + end + "-" +
                            start + "=" + (end - start));
        }
    }

    protected void deleteMetricProblems(Integer[] mids) {
        getMetricProblemDAO().deleteByMetricIds(mids);
    }
}
