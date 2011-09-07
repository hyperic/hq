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

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>TilesAction</code> that retrieves metric data to facilitate display
 * of a current health page. Concrete subclasses provide methods to retrieve
 * resource-type-specific data.
 */
public class CurrentHealthAction
    extends TilesAction {

    protected final Log log = LogFactory.getLog(CurrentHealthAction.class.getName());
    protected MeasurementBoss measurementBoss;
    private final PageControl pc = new PageControl(0, Constants.DEFAULT_CHART_POINTS);
    
    @Autowired
    public CurrentHealthAction(MeasurementBoss measurementBoss) {
        super();
        this.measurementBoss = measurementBoss;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        AppdefResourceValue resource = RequestUtils.getResource(request);

        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }

        AppdefEntityID aeid = resource.getEntityId();

        ServletContext ctx = getServlet().getServletContext();
        // Check configuration
        InventoryHelper helper = InventoryHelper.getHelper(aeid);
        helper.isResourceConfigured(request, ctx, true);

        // Set the views
        setupViews(request, (IndicatorViewsForm) form, aeid.getAppdefKey());

        // Get the resource availability
        int sessionId = RequestUtils.getSessionId(request).intValue();

        WebUser user = RequestUtils.getWebUser(request);

        try {
            MeasurementTemplate mt = measurementBoss.getAvailabilityMetricTemplate(sessionId, aeid);

            Map<String, Object> pref = user.getMetricRangePreference(true);
            long begin = ((Long) pref.get(MonitorUtils.BEGIN)).longValue();
            long end = ((Long) pref.get(MonitorUtils.END)).longValue();
            long interval = TimeUtil.getInterval(begin, end, Constants.DEFAULT_CHART_POINTS);

            List<HighLowMetricValue> data = measurementBoss.findMeasurementData(sessionId, aeid, mt, begin, end,
                interval, true, pc);
            final AppdefEntityID[] aeids = new AppdefEntityID[]{aeid};
            double availAvg = measurementBoss.getAvailabilityAverage(aeids, begin, end);

            // Seems like sometimes Postgres does not average cleanly for
            // groups, and the value ends up being like 0.9999999999. We don't
            // want the insignificant amount to mess up our display.
            if (aeid.isGroup()) {
                for (MetricValue val : data) {
                    if (val.toString().equals("1")) {
                        val.setValue(1);
                    }
                }
            }

            request.setAttribute(Constants.CAT_AVAILABILITY_METRICS_ATTR, data);
            request.setAttribute(Constants.AVAIL_METRICS_ATTR, getFormattedAvailability(availAvg));
        } catch (MeasurementNotFoundException e) {
            // No utilization metric
            if(debug) {
                log.debug(MeasurementConstants.CAT_AVAILABILITY + " not found for " + aeid);
            }
        }

        if (debug) {
            log.debug("CurrentHealthAction.execute: " + watch);
        }
        return null;
    }

    protected String getFormattedAvailability(double values) {
        UnitNumber average = new UnitNumber(values, UnitsConstants.UNIT_PERCENTAGE);
        return UnitsFormat.format(average).toString();
    }

    protected String getDefaultViewName(HttpServletRequest request) {

        MessageResources res = getResources(request);
        return res.getMessage(Constants.DEFAULT_INDICATOR_VIEW);
    }

    protected void setupViews(HttpServletRequest request, IndicatorViewsForm ivf, String key) {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        String[] views;
        // Try to get the view names from user preferences
        try {
            String viewsPref = user.getPreference(Constants.INDICATOR_VIEWS + key);
            StringTokenizer st = new StringTokenizer(viewsPref, Constants.DASHBOARD_DELIMITER);

            views = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
                views[i] = st.nextToken();
        } catch (InvalidOptionException e) {
            views = new String[] { getDefaultViewName(request) };
        }

        ivf.setViews(views);

        String viewName = RequestUtils.getStringParameter(request, Constants.PARAM_VIEW, views[0]);

        // Make sure that the view name is one of the views
        boolean validated = false;
        for (int i = 0; i < views.length; i++) {
            if (validated = viewName.equals(views[i]))
                break;
        }

        if (!validated)
            viewName = views[0];

        ivf.setView(viewName);
    }
}
