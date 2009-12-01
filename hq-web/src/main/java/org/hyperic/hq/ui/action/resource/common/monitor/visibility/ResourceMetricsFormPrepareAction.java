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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An <code>Action</code> that retrieves data from the BizApp to
 * facilitate display of the <em><Resource> Metrics</em> pages.
 * 
 * This is used for "Platform Metrics",  "Server Metrics"  and the 
 * various service metrcs pages. 
 */
public class ResourceMetricsFormPrepareAction
    extends MetricsDisplayFormPrepareAction {

    protected static Log log =
        LogFactory.getLog(ResourceMetricsFormPrepareAction.class.getName());

    // ---------------------------------------------------- Protected Methods

    /**
     * Do we show the baseline column on this page? The answer is no (for now).
     */
    protected Boolean getShowBaseline() {
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
    protected Map getMetrics(HttpServletRequest request,
                             AppdefEntityID entityId,
                             long filters, String keyword,
                             Long begin, Long end, boolean showAll)
    throws Exception {
        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);

        if (log.isTraceEnabled())
            log.trace("finding metric summaries for resource [" + entityId +
                      "] for range " + begin + ":" + end + " filters value: " +
                      filters + " and keyword: " + keyword);

        AppdefEntityID[] entIds = new AppdefEntityID[] { entityId };
        Map metrics =
            boss.findMetrics(sessionId, entIds, filters, keyword,
                             begin.longValue(), end.longValue(), showAll);
        
//        if (log.isTraceEnabled())
//            MonitorUtils.traceMetricDisplaySummaryMap(log, metrics);

        return metrics;
    }
}
