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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * An <code>Action</code> that retrieves data from the BizApp to
 * facilitate display of the various pages that provide metrics
 * summaries.
 */
public abstract class MetricsDisplayFormPrepareAction
    extends MetricsControlFormPrepareAction {

    protected static Log log =
        LogFactory.getLog(MetricsDisplayFormPrepareAction.class.getName());


    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond
     * to certain button clicks that alter the form display.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Long begin, Long end)
        throws Exception {

        MetricsDisplayForm displayForm = (MetricsDisplayForm) form;
        displayForm.setShowNumberCollecting(getShowNumberCollecting());
        displayForm.setShowBaseline(getShowBaseline());
        displayForm.setShowMetricSource(getShowMetricSource());

        AppdefEntityID[] entityIds = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
        }
        catch (ParameterNotFoundException e) {
            // this is fine until we finish migrating to eids
        }

        if (begin == null || end == null) {
            // get the "metric range" user pref
            WebUser user = RequestUtils.getWebUser(request);
            Map range = user.getMetricRangePreference();
            if (range != null) {    
                begin = (Long) range.get(MonitorUtils.BEGIN);
                end = (Long) range.get(MonitorUtils.END);
            } else {
                log.error("no appropriate display range begin and end");
            }
        }

        long filters = MeasurementConstants.FILTER_NONE;
        String keyword = null;
        
        if (displayForm.isFilterSubmitClicked()) {
            filters = displayForm.getFilters();
            keyword = displayForm.getKeyword();
        }

        Map metrics =
            getMetrics(request, entityIds, filters, keyword, begin, end,
                       displayForm.getShowAll());

        if (metrics != null) {
            Integer resourceCount =
                MonitorUtils.formatMetrics(metrics, request.getLocale(),
                                           getResources(request));

//            if (log.isTraceEnabled()) {
//                log.trace("Got formatted metrics");
//                MonitorUtils.traceMetricDisplaySummaryMap(log, metrics);
//            }

           request.setAttribute(Constants.NUM_CHILD_RESOURCES_ATTR, resourceCount);
           request.setAttribute(Constants.METRIC_SUMMARIES_ATTR, metrics);

           // populate the form
           displayForm.setupCategoryList(metrics);
           // prepareForm(request, displayForm);
        } else {
            log.trace("no metrics were returned by getMetrics(...)");
        }

        // Clear any compare metric workflow
        SessionUtils.clearWorkflow(request.getSession(),
                                   Constants.WORKFLOW_COMPARE_METRICS_NAME);
        
        return super.execute(mapping, form, request, response);
    }

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        return execute(mapping, form, request, response, null, null);
        
    }

    // ---------------------------------------------------- Protected Methods

    /**
     * Do we show the number collecting column on this page? The
     * default answer is no, but subclasses can specify otherwise.
     */
    protected Boolean getShowNumberCollecting() {
        return Boolean.FALSE;
    }

    /**
     * Do we show the baseline column on this page? The default answer
     * is no, but subclasses can specify otherwise.
     */
    protected Boolean getShowBaseline() {
        return Boolean.FALSE;
    }

    /**
     * Do we show the metric source column on this page? The default
     * answer is no, but subclasses can specify otherwise.
     */
    protected Boolean getShowMetricSource() {
        return Boolean.FALSE;
    }

    /**
     * Get from the Bizapp the set of metric summaries for the
     * specified entity that will be displayed on the page. Returns a
     * <code>Map</code> keyed by metric category.
     *
     * @param request the http request
     * @param entityId the entity id of the currently viewed resource
     * @param begin the time (in milliseconds since the epoch) that
     *  begins the timeframe for which the metrics are summarized
     * @param end the time (in milliseconds since the epoch) that
     *  ends the timeframe for which the metrics are summarized
     * @return Map keyed on the category (String), values are List's of 
     * MetricDisplaySummary beans
     */
    protected abstract Map getMetrics(HttpServletRequest request,
                                      AppdefEntityID entityId,
                                      long filters, String keyword,
                                      Long begin, Long end, boolean showAll)
        throws Exception;

    /**
     * Get from the Bizapp the set of metric summaries for the
     * specified entities that will be displayed on the page. Returns a
     * <code>Map</code> keyed by metric category.
     *
     * @param request the http request
     * @param entityId the entity id of the currently viewed resource
     * @param begin the time (in milliseconds since the epoch) that
     *  begins the timeframe for which the metrics are summarized
     * @param end the time (in milliseconds since the epoch) that
     *  ends the timeframe for which the metrics are summarized
     * @return Map keyed on the category (String), values are List's of 
     * MetricDisplaySummary beans
     */
    protected Map getMetrics(HttpServletRequest request,
                             AppdefEntityID[] entityIds,
                             long filters, String keyword,
                             Long begin, Long end, boolean showAll)
        throws Exception {
        // XXX when we finish migrating to eids, the other form of
        // getMetrics will go away and this one will become abstract
        // in the meantime, preserve friggin backwards compatibility
        if (entityIds != null && entityIds.length == 1)
            return getMetrics(request, entityIds[0], filters, keyword,
                              begin, end, showAll);
        return null;
    }
}
