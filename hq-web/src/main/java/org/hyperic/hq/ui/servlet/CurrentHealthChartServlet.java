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

import java.rmi.RemoteException;
import java.util.List;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.image.chart.Chart;
import org.hyperic.image.chart.DataPointCollection;
import org.hyperic.image.chart.VerticalChart;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;

/**
 * <p>CurrentHealth chart servlet.  The default <code>imageWidth</code>
 * is 250 pixels.  The default <code>imageHeight</code> is 130 pixels.</p>
 *
 * <p>by default, this servlet will display an 8 column chart for the
 * past 8 hours at 1 hour intervals based on the metric category
 * returned by the <code>{@link getMetricCategory}()</code>.</p>
 *
 * <p>Additional parameters are as follows (any required parameters
 * are in <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                       </th></tr>
 * <tr><td> <i>eid</i>       </td><td> &lt;string or string[]&gt;  </td></tr>
 * <tr><td> ctype            </td><td> &lt;integer&gt;             </td></tr>
 * </table>
 *
 */
public abstract class CurrentHealthChartServlet extends VerticalChartServlet {
    /** Interval for metrics. */
    protected static final long INTERVAL = Constants.MINUTES * 30; // 1/2 hour

    /** Default image width. */
    public static final int IMAGE_WIDTH_DEFAULT = 200;
    /** Default image height. */
    public static final int IMAGE_HEIGHT_DEFAULT = 100;

    // member data
    private Log log =
        LogFactory.getLog(CurrentHealthChartServlet.class.getName());

    /**
     * Return the default <code>imageWidth</code>.
     */
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    /**
     * Return the default <code>imageHeight</code>.
     */
    protected int getDefaultImageHeight() {
        return IMAGE_HEIGHT_DEFAULT;
    }

    /**
     * Return the corresponding measurement category.
     *
     * @return <code>{@link org.hyperic.hq.measurement.MeasurementConstants.CAT_AVAILABILITY}</code> or
     *         <code>{@link org.hyperic.hq.measurement.MeasurementConstants.CAT_THROUGHPUT}</code> or
     *         <code>{@link org.hyperic.hq.measurement.MeasurementConstants.CAT_PERFORMANCE}</code> or
     *         <code>{@link org.hyperic.hq.measurement.MeasurementConstants.CAT_UTILIZATION}</code>
     */
    protected abstract String getMetricCategory();

    protected void initializeChart(Chart chart, HttpServletRequest request) {
        super.initializeChart(chart, request);
        chart.font = Chart.SMALL_FONT;
        chart.showFullLabels = false;
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle adding data to the chart, setting up the X and
     * Y axis labels, etc.
     *
     * @param request the HTTP request
     */
    protected void plotData(HttpServletRequest request, Chart chart, ChartDataBean dataBean)
        throws ServletException {
        AppdefEntityID[] eids = null;
        AppdefEntityTypeID ctype = null;
        try {
            eids = RequestUtils.getEntityIds(request);
        } catch (ParameterNotFoundException e) {
            /* platform auto-group */
        }
        
        try {
            ctype = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e) {
            /* non auto-group */
        }
        
        Integer tid = RequestUtils.getIntParameter(request, "tid");
        try {
            VerticalChart verticalChart = (VerticalChart) chart;

            long endTime = System.currentTimeMillis();
            long beginTime = endTime - (8l * Constants.HOURS);

            MeasurementBoss mb =
                Bootstrap.getBean(MeasurementBoss.class);
            
            List data = null;
            try {
                String user = RequestUtils.getStringParameter(request, "user");
                data = getData(user, mb, verticalChart, tid, eids, ctype,
                               beginTime, endTime);
            } catch (ParameterNotFoundException e) {
                int sessionID = RequestUtils.getSessionId(request).intValue();
                data = getData(sessionID, mb, verticalChart, tid, eids, ctype,
                               beginTime, endTime);
            }
            
            if (log.isDebugEnabled()) {
                log.debug("Got " + data.size() + " " + getMetricCategory()
                        + " metric data points.");
                if (log.isTraceEnabled()) {
                    log.debug("data=" + data);
                }
            }
            
            DataPointCollection chartData = chart.getDataPoints();
            chartData.addAll(data);
        } catch (MeasurementNotFoundException e) {
            if ( log.isDebugEnabled() ) // don't log internal category names PR 6417
                log.debug( "No " + getMetricCategory() + " metric found for: " +
                           StringUtil.arrayToString(eids) );
        } catch (AppdefEntityNotFoundException e) {
            if ( log.isDebugEnabled() )
                log.debug( "One or more AppdefEntityIDs invalid: " +
                           StringUtil.arrayToString(eids) );
        } catch (PermissionException e) {
            log.warn("Permission denied to view metric.");
        } catch (SessionNotFoundException e) {
            log.warn("Session not found.");
        } catch (SessionTimeoutException e) {
            log.warn("Session timeout.");
        } catch (RemoteException e) {
            log.warn("Unknown error.", e);
        } catch (TemplateNotFoundException e) {
            log.warn("Template " + tid + " not found", e);
        } catch (LoginException e) {
            log.warn("Unable to login user", e);
        } catch (ApplicationException e) {
            log.warn("Error looking measurement data to chart", e);
        } catch (ConfigPropertyException e) {
            log.warn("Configuration error", e);
        }
    }
    
    private List getData(String user, MeasurementBoss mb, VerticalChart chart,
                         Integer tid, AppdefEntityID[] eids,
                         AppdefEntityTypeID ctype, long beginTime, long endTime)
        throws LoginException, ApplicationException, RemoteException,
               ConfigPropertyException {
        Integer[] tids = new Integer[] { tid };
        
        List templates =
            mb.findMeasurementTemplates(user, tids, PageControl.PAGE_ALL);

        MeasurementTemplate tmpv = (MeasurementTemplate) templates.get(0);

        setChartUnits(chart, tmpv);

        if (null == ctype) {
            return mb.findMeasurementData(user, eids[0], tmpv, 
                                          beginTime, endTime, INTERVAL,
                                          true, PageControl.PAGE_ALL);
        } else {
            return mb.findAGMeasurementData(user, eids, tmpv, ctype,
                                            beginTime, endTime, INTERVAL,
                                            true, PageControl.PAGE_ALL);
        }
    }
    
    private List getData(int sessionID, MeasurementBoss mb, VerticalChart chart,
                         Integer tid, AppdefEntityID[] eids,
                         AppdefEntityTypeID ctype, long beginTime, long endTime)
        throws TemplateNotFoundException, SessionNotFoundException,
               SessionTimeoutException,
               AppdefEntityNotFoundException, MeasurementNotFoundException,
               PermissionException, RemoteException {
        Integer[] tids = new Integer[] { tid };
        
        List templates =
            mb.findMeasurementTemplates(sessionID, tids, PageControl.PAGE_ALL);

        MeasurementTemplate tmpv = (MeasurementTemplate) templates.get(0);

        if (log.isDebugEnabled())
            log.debug("template ID=" + tmpv.getId());

        setChartUnits(chart, tmpv);
        
        if (null == ctype) {
            return mb.findMeasurementData(sessionID, eids[0], tmpv,
                                          beginTime, endTime, INTERVAL,
                                          true, PageControl.PAGE_ALL);
        } else {
            return mb.findAGMeasurementData(sessionID, eids, tmpv, ctype,
                                            beginTime, endTime, INTERVAL,
                                            true, PageControl.PAGE_ALL);
        }
    }
    
    private void setChartUnits(VerticalChart chart,
                               MeasurementTemplate mt) {
        // override default / parsed units with the one from the metric
        int unitUnits = UnitsConvert.getUnitForUnit(mt.getUnits());
        int unitScale = UnitsConvert.getScaleForUnit(mt.getUnits());
        chart.setFormat(unitUnits, unitScale);
        int cumulativeTrend =
            getTrendForCollectionType(mt.getCollectionType());
        chart.setCumulativeTrend(cumulativeTrend);
    }
}

