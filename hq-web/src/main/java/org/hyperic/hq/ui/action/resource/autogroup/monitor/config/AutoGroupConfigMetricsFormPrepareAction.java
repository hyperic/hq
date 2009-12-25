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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.config;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricConfigSummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.config.ConfigMetricsFormPrepareAction;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This populates the AutoGroupConfigMetrics/Update metrics pages' request
 * attributes.
 */
public class AutoGroupConfigMetricsFormPrepareAction
    extends ConfigMetricsFormPrepareAction {

    private final Log log = LogFactory.getLog(AutoGroupConfigMetricsFormPrepareAction.class.getName());

    @Autowired
    public AutoGroupConfigMetricsFormPrepareAction(MeasurementBoss measurementBoss, AppdefBoss appdefBoss) {
        super(measurementBoss, appdefBoss);
    }

    /**
     * Retrieve different resource metrics and store them in various request
     * attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.trace("Preparing auto-group resource metrics action.");

        int sessionId = RequestUtils.getSessionId(request).intValue();

        // auto-group specific prepare actions here
        InventoryHelper helper = null;
        AppdefEntityID[] entityIds = null;
        AppdefEntityID typeHolder = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            // if we get this far, we are dealing with an auto-group
            // of servers or services

            // find the resource type of the autogrouped resources
            typeHolder = entityIds[0];
            helper = InventoryHelper.getHelper(typeHolder);
        } catch (ParameterNotFoundException e) {
            // if we get here, we are dealing with an auto-group of
            // platforms
            helper = new RootInventoryHelper(appdefBoss);
        }

        AppdefEntityTypeID childTypeId;
        try {
            childTypeId = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e1) {
            // must be an autogroup resource type
            // childTypeId = RequestUtils.getAutogroupResourceTypeId(request);
            // REMOVE ME?
            throw e1;
        }
        ServletContext ctx = getServlet().getServletContext();
        AppdefResourceType selectedType = helper.getChildResourceType(request, ctx, childTypeId);
        request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR, selectedType);

        AppdefEntityID appdefId = RequestUtils.getEntityId(request);

        int totalSize = 0;

        // check to see if monitoring is configured for this resource
        helper = InventoryHelper.getHelper(appdefId);
        boolean configEnabled = true;
        try {
            configEnabled = helper.isResourceConfigured(request, ctx, true);
        } finally {
            log.debug("config enabled: " + configEnabled);
        }
        request.setAttribute(Constants.MONITOR_ENABLED_ATTR, new Boolean(configEnabled));

        // obtain the different categories of measurements
        log.debug("obtaining metrics for resource " + appdefId + " autogroup type " + childTypeId);
        List<MetricConfigSummary> availMetrics = measurementBoss.findEnabledAGMeasurements(sessionId, appdefId,
            childTypeId, MeasurementConstants.CAT_AVAILABILITY, PageControl.PAGE_ALL);
        request.setAttribute(Constants.CAT_AVAILABILITY_METRICS_ATTR, availMetrics);
        totalSize += availMetrics.size();

        List<MetricConfigSummary> perfMetrics = measurementBoss.findEnabledAGMeasurements(sessionId, appdefId,
            childTypeId, MeasurementConstants.CAT_PERFORMANCE, PageControl.PAGE_ALL);
        request.setAttribute(Constants.CAT_PERFORMANCE_METRICS_ATTR, perfMetrics);
        totalSize += perfMetrics.size();

        List<MetricConfigSummary> throughMetrics = measurementBoss.findEnabledAGMeasurements(sessionId, appdefId,
            childTypeId, MeasurementConstants.CAT_THROUGHPUT, PageControl.PAGE_ALL);
        request.setAttribute(Constants.CAT_THROUGHPUT_METRICS_ATTR, throughMetrics);
        totalSize += throughMetrics.size();

        List<MetricConfigSummary> utilMetrics = measurementBoss.findEnabledAGMeasurements(sessionId, appdefId,
            childTypeId, MeasurementConstants.CAT_UTILIZATION, PageControl.PAGE_ALL);
        request.setAttribute(Constants.CAT_UTILIZATION_METRICS_ATTR, utilMetrics);
        totalSize += utilMetrics.size();

        // set total size as aggregate of all
        request.setAttribute(Constants.LIST_SIZE_ATTR, new Integer(totalSize));

        log.debug("Successfully completed preparing Config Metrics");

        return null;
    }
}
