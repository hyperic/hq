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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An <code>Action</code> that retrieves data from the BizApp to
 * facilitate display of the various pages that provide metrics
 * summaries.
 */
public class CurrentMetricValuesAction extends BaseAction {

    protected static Log log =
        LogFactory.getLog(CurrentMetricValuesAction.class.getName());


    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond
     * to certain button clicks that alter the form display.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        AppdefEntityID[] entityIds = null;
        AppdefEntityTypeID ctype = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            ctype = RequestUtils.getChildResourceTypeId(request);
        }
        catch (ParameterNotFoundException e) {
            if (entityIds == null)  // Platform autogroup
                ctype = RequestUtils.getChildResourceTypeId(request);
        }

        Long begin = null, end = null;
        
        // get the "metric range" user pref
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Map range = user.getMetricRangePreference();
        if (range != null) {    
            begin = (Long) range.get(MonitorUtils.BEGIN);
            end = (Long) range.get(MonitorUtils.END);
        } else {
            log.error("no appropriate display range begin and end");
        }

        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);

        Map metrics;
        
        if (ctype == null) {
            metrics = boss.findMetrics(sessionId, entityIds,
                                       MeasurementConstants.FILTER_NONE, null,
                                       begin.longValue(), end.longValue(),
                                       false);
        }
        else {
            if (null == entityIds) {
                metrics = boss.findAGPlatformMetricsByType(sessionId, ctype,
                                                           begin.longValue(),
                                                           end.longValue(),
                                                           false);
            } else {
                metrics =
                    boss.findAGMetricsByType(sessionId, entityIds, ctype,
                                             MeasurementConstants.FILTER_NONE,
                                             null, begin.longValue(),
                                             end.longValue(), false);
            }
        }

        if (metrics != null) {
            MonitorUtils.formatMetrics(metrics, request.getLocale(),
                                       getResources(request));

            // Create an array list of map objects for the attributes
            ArrayList objects = new ArrayList();
            for (Iterator it = metrics.values().iterator(); it.hasNext(); ) {
                Collection metricList = (Collection) it.next();
                for (Iterator m = metricList.iterator(); m.hasNext();) {
                    MetricDisplaySummary mds = (MetricDisplaySummary) m.next();
                    HashMap values = new HashMap();
                    values.put("mid", mds.getTemplateId());
                    values.put("alertCount", new Integer(mds.getAlertCount()));
                    values.put("oobCount", new Integer(mds.getOobCount()));
                    values.put("min", mds.getMinMetric().toString());
                    values.put("average", mds.getAvgMetric());
                    values.put("max", mds.getMaxMetric());
                    values.put("last", mds.getLastMetric());
                    objects.add(values);
                }
            }
            request.setAttribute("objects", objects);
            request.setAttribute(Constants.AJAX_TYPE, Constants.AJAX_OBJECT);
            request.setAttribute(Constants.AJAX_ID, "metricsUpdater");
        } else {
            log.trace("no metrics were returned by getMetrics(...)");
        }

        return mapping.findForward(Constants.SUCCESS_URL);
    }
}
