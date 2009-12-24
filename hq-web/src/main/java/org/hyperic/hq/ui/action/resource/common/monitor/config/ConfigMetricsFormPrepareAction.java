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

package org.hyperic.hq.ui.action.resource.common.monitor.config;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This populates the ConfigMetrics/Update metrics pages' request attributes.
 */
public class ConfigMetricsFormPrepareAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ConfigMetricsFormPrepareAction.class);

    protected MeasurementBoss measurementBoss;
    protected AppdefBoss appdefBoss;

    @Autowired
    public ConfigMetricsFormPrepareAction(MeasurementBoss measurementBoss, AppdefBoss appdefBoss) {
        super();
        this.measurementBoss = measurementBoss;
        this.appdefBoss = appdefBoss;
    }

    /**
     * Retrieve different resource metrics and store them in various request
     * attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.trace("Preparing resource metrics action.");

        int sessionId = RequestUtils.getSessionId(request).intValue();

        PageControl pca = RequestUtils.getPageControl(request, "ps", "pn", "soa", "sca");
        PageControl pcp = RequestUtils.getPageControl(request, "ps", "pn", "sop", "scp");
        PageControl pct = RequestUtils.getPageControl(request, "ps", "pn", "sot", "sct");
        PageControl pcu = RequestUtils.getPageControl(request, "ps", "pn", "sou", "scu");

        /* begin page control disablement, see PR's 8244 && 7821 */
        pca.setPagesize(-1);
        pcp.setPagesize(-1);
        pct.setPagesize(-1);
        pcu.setPagesize(-1);
        /* end page control disablement */

        int totalSize = 0;

        List availMetrics, perfMetrics, throughMetrics, utilMetrics;
        try {
            AppdefEntityTypeID atid = new AppdefEntityTypeID(RequestUtils.getStringParameter(request,
                Constants.APPDEF_RES_TYPE_ID));

            AppdefResourceTypeValue resourceTypeVal = appdefBoss.findResourceTypeById(sessionId, atid);

            availMetrics = measurementBoss.findMeasurementTemplates(sessionId, atid,
                MeasurementConstants.CAT_AVAILABILITY, pca);
            perfMetrics = measurementBoss.findMeasurementTemplates(sessionId, atid,
                MeasurementConstants.CAT_PERFORMANCE, pcp);
            throughMetrics = measurementBoss.findMeasurementTemplates(sessionId, atid,
                MeasurementConstants.CAT_THROUGHPUT, pct);
            utilMetrics = measurementBoss.findMeasurementTemplates(sessionId, atid,
                MeasurementConstants.CAT_UTILIZATION, pcu);

            request.setAttribute(Constants.MONITOR_ENABLED_ATTR, Boolean.FALSE);
            request.setAttribute(Constants.RESOURCE_TYPE_ATTR, resourceTypeVal);
            request.setAttribute("section", AppdefEntityConstants.typeToString(resourceTypeVal.getAppdefType()));

        } catch (ParameterNotFoundException e) {
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            // check to see if monitoring is configured for this resource
            boolean configEnabled = true;
            try {
                // Check configuration
                InventoryHelper helper = InventoryHelper.getHelper(appdefId);
                configEnabled = helper.isResourceConfigured(request, getServlet().getServletContext(), true);
            } finally {
                log.debug("config enabled: " + configEnabled);
            }
            request.setAttribute(Constants.MONITOR_ENABLED_ATTR, Boolean.valueOf(configEnabled));

            // obtain the different categories of measurements
            log.debug("obtaining metrics for resource " + appdefId);
            availMetrics = measurementBoss.findEnabledMeasurements(sessionId, appdefId,
                MeasurementConstants.CAT_AVAILABILITY, pca);
            perfMetrics = measurementBoss.findEnabledMeasurements(sessionId, appdefId,
                MeasurementConstants.CAT_PERFORMANCE, pcp);
            throughMetrics = measurementBoss.findEnabledMeasurements(sessionId, appdefId,
                MeasurementConstants.CAT_THROUGHPUT, pct);
            utilMetrics = measurementBoss.findEnabledMeasurements(sessionId, appdefId,
                MeasurementConstants.CAT_UTILIZATION, pcu);
        }

        // finally set all the lists
        request.setAttribute(Constants.CAT_AVAILABILITY_METRICS_ATTR, availMetrics);
        totalSize += availMetrics.size();
        request.setAttribute(Constants.CAT_PERFORMANCE_METRICS_ATTR, perfMetrics);
        totalSize += perfMetrics.size();
        request.setAttribute(Constants.CAT_THROUGHPUT_METRICS_ATTR, throughMetrics);
        totalSize += throughMetrics.size();
        request.setAttribute(Constants.CAT_UTILIZATION_METRICS_ATTR, utilMetrics);
        totalSize += utilMetrics.size();

        // set total size as aggregate of all
        request.setAttribute(Constants.LIST_SIZE_ATTR, new Integer(totalSize));

        log.debug("Successfully completed preparing Config Metrics");

        return null;
    }
}
