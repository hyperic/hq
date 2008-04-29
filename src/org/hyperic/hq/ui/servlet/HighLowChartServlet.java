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

package org.hyperic.hq.ui.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.image.chart.Chart;
import org.hyperic.image.chart.DataPointCollection;
import org.hyperic.image.chart.HighLowChart;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.pager.PageControl;

/**
 *
 * Display a high low chart
 */
public class HighLowChartServlet extends ChartServlet {

    private Log log = LogFactory.getLog(HighLowChartServlet.class.getName());
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.servlet.ChartServlet#createChart()
     */
    protected Chart createChart(HttpServletRequest request, ChartDataBean dataBean) {
        log.trace("plotting a high low chart");
        return new HighLowChart( getImageWidth(request), getImageHeight(request) );
    }

    /**
     * Initialize the chart.  This method will be called after the
     * parameters have been parsed and the chart has been created.
     *
     * @param chart the chart
     */
    protected void initializeChart(Chart chart, HttpServletRequest request) {
        super.initializeChart(chart, request);

        HighLowChart hiloChart = (HighLowChart) chart;
        hiloChart.setNumberDataSets(1);
        hiloChart.leftBorder = 0;
        hiloChart.rightLabelWidth = (int) (this.getImageWidth(request) * 0.1);
        hiloChart.columnWidth = 7;
    }


    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.servlet.ChartServlet#plotData(javax.servlet.http.HttpServletRequest)
     */
    protected void plotData(HttpServletRequest request, Chart chart, ChartDataBean dataBean)
        throws ServletException {
        // Make sure the entity and measurement IDs were passed in
        Integer tid = RequestUtils.getIntParameter(request, "tid");
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        
        ServletContext ctx = this.getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);
        WebUser user = RequestUtils.getWebUser(request);
        int sessionId = user.getSessionId().intValue();

        // set metric range defaults
        Map pref = user.getMetricRangePreference(true);
        Long begin = (Long) pref.get(MonitorUtils.BEGIN);
        Long end = (Long) pref.get(MonitorUtils.END);

        long interval = TimeUtil.getInterval(begin.longValue(),
                                             end.longValue(),
                                             Constants.DEFAULT_CHART_POINTS);

        PageControl pc = new PageControl(0, Constants.DEFAULT_CHART_POINTS);
        Collection data;
        try {
            try {
                // See if there are entities passed in
                AppdefEntityID[] eids = (AppdefEntityID[]) request.getSession()
                    .getAttribute(aeid.getAppdefKey() + ".entities");
                
                ArrayList entList = null;
                if (eids != null) {
                    entList = new ArrayList(Arrays.asList(eids));
                }

                if (entList != null) {
                    if (!RequestUtils.parameterExists(
                            request, Constants.CHILD_RESOURCE_TYPE_ID_PARAM) &&
                        aeid.getType() !=
                            AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                        // Not group or autogroup
                        entList.add(aeid);
                    }
                    
                    data = boss.findMeasurementData(
                        sessionId, tid, entList, begin.longValue(),
                        end.longValue(), interval, true, pc);
                }
                else {
                    AppdefEntityTypeID childTypeId =
                        RequestUtils.getChildResourceTypeId(request);
                    
                    data = boss.findMeasurementData(
                        sessionId, tid, aeid, childTypeId,
                        begin.longValue(), end.longValue(), interval, true, pc);
                }
            } catch (ParameterNotFoundException e) {
                data = boss.findMeasurementData(sessionId, tid, aeid,
                    begin.longValue(), end.longValue(), interval, true, pc);
            }
        } catch (Exception e) {
            throw new ServletException("Cannot fetch metric data: " + e, e);
        }
        
        HighLowChart hiloChart = (HighLowChart) chart;
        DataPointCollection bars = hiloChart.getDataPoints(0);
        bars.addAll(data);
    }

}
