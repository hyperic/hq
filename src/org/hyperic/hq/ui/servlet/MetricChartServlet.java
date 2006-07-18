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

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.image.chart.Chart;
import org.hyperic.image.chart.ColumnChart;
import org.hyperic.image.chart.DataPointCollection;
import org.hyperic.image.chart.EventPointCollection;
import org.hyperic.image.chart.LineChart;
import org.hyperic.image.chart.VerticalChart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Extends ChartServlet to graph one or more metrics.  By default,
 * <code>showPeak</code>, <code>showHighRange</code>,
 * <code>showValues</code>, <code>showAverage</code>,
 * <code>showLowRange</code>, <code>showLow</code> and
 * <code>showBaseline</code> are all true.</p>
 *
 * <p>Additional parameters are as follows (any required parameters
 * are in <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key                     </th><th> value                   </th></tr>
 * <tr><td> <i>chartDataKey</i>     </td><td> &lt;string&gt;          </td></tr>
 * <tr><td> showEvents              </td><td> (<b>false</b> | true)   </td></tr>
 * </table>
 *
 * <p>The <code>chartDataKey</code> will be used to retrieve the chart
 * data from the session.  Once it is pulled, it will be removed from
 * the session.</p>
 *
 */
public class MetricChartServlet extends VerticalChartServlet {
    /** Request parameter for the chart data key session attribute. */
    public static final String CHART_DATA_KEY_PARAM = "chartDataKey";

    /** Request parameter for whether or not to show control actions. */
    public static final String SHOW_EVENTS_PARAM = "showEvents";

    // member data
    private Log log = LogFactory.getLog( MetricChartServlet.class.getName() );
    private String chartDataKey;
    private boolean showEvents;
    private boolean plotLineChart;
    private ChartDataBean dataBean;

    public MetricChartServlet () {}

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        super.parseParameters(request);

        // chart data key
        chartDataKey = parseRequiredStringParameter(request, CHART_DATA_KEY_PARAM);
        log.debug("Get from session: " + chartDataKey);
        // We will actually set a flag here to determine whether we
        // should draw a LineChart or a column chart.  If we are
        // charting just one set of data / event points, we'll plot a
        // ColumnChart.  Otherwise we'll plot a LineChart.
        HttpSession session = request.getSession();
        dataBean = (ChartDataBean) session.getAttribute(chartDataKey);
        session.removeAttribute(chartDataKey);
        
        List dataPointsList = dataBean.getDataPoints();
        plotLineChart = (dataPointsList.size() > 1);

        // chart flags
        showEvents = parseBooleanParameter( request, SHOW_EVENTS_PARAM,
                                            getDefaultShowEvents() );
        _logParameters();
    }

    /**
     * Create and return the chart.  This method will be called after
     * the parameters have been parsed.
     *
     * @return the newly created chart
     */
    protected Chart createChart() {
        if (plotLineChart) {
            log.trace("plotting a line chart");
            return new LineChart( getImageWidth(), getImageHeight() );
        } else {
            log.trace("plotting a column chart");
            return new ColumnChart( getImageWidth(), getImageHeight() );
        }
    }

    /**
     * Initialize the chart.  This method will be called after the
     * parameters have been parsed and the chart has been created.
     *
     * @param chart the chart
     */
    protected void initializeChart(Chart chart) {
        super.initializeChart(chart);

        VerticalChart verticalChart = (VerticalChart) chart;
        verticalChart.showEvents = showEvents;
        verticalChart.showRightLabels = false;
        verticalChart.rightLabelWidth = (int) (getImageWidth() * 0.1);
        verticalChart.xLabelsSkip = 5;
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle adding data to the chart, setting up the X and
     * Y axis labels, etc.
     *
     * @param request the HTTP request
     */
    protected void plotData(HttpServletRequest request, Chart chart)
        throws ServletException {
        VerticalChart veritcalChart = (VerticalChart) chart;
        
        List dataPointsList = dataBean.getDataPoints();
        List eventsPointsList = dataBean.getEventPoints();
        // make sure they're the same size
        if ( dataPointsList.size() == eventsPointsList.size() ) {
            if ( log.isDebugEnabled() ) {
                log.debug("got " + dataPointsList.size() + " set(s) of data / event points.");
            }
        } else {
            throw new ServletException
                ("Number of data point sets and number of event point sets must be the same.");
        }
        veritcalChart.setNumberDataSets( dataPointsList.size() );
        int i=0;
        Iterator it = dataPointsList.iterator();
        Iterator jt = eventsPointsList.iterator();
        while ( it.hasNext() && jt.hasNext() ) {
            // data points
            List data = (List)it.next();
            log.trace("plotting " + data.size() + " data points");
            DataPointCollection chartData = chart.getDataPoints(i);
            chartData.addAll(data);

            // events
            List events = (List)jt.next();
            log.trace("plotting " + events.size() + " event points");
            EventPointCollection chartEvents = chart.getEventPoints(i);
            chartEvents.addAll(events);

            // increment
            ++i;
        }

        request.getSession().removeAttribute(chartDataKey);
    }

    /**
     * Return the default <code>showPeak</code>.
     */
    protected boolean getDefaultShowPeak() {
        return true;
    }

    /**
     * Return the default <code>showHighRange</code>.
     */
    protected boolean getDefaultShowHighRange() {
        return true;
    }

    /**
     * Return the default <code>showValues</code>.
     */
    protected boolean getDefaultShowValues() {
        return true;
    }

    /**
     * Return the default <code>showAverage</code>.
     */
    protected boolean getDefaultShowAverage() {
        return true;
    }

    /**
     * Return the default <code>showLowRange</code>.
     */
    protected boolean getDefaultShowLowRange() {
        return true;
    }

    /**
     * Return the default <code>showLow</code>.
     */
    protected boolean getDefaultShowLow() {
        return true;
    }

    /**
     * Return the default <code>showBaseline</code>.
     */
    protected boolean getDefaultShowBaseline() {
        return true;
    }

    /**
     * Return the default <code>showEvents</code>.
     */
    protected boolean getDefaultShowEvents() {
        return true;
    }


    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if ( log.isDebugEnabled() ) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");sb.append("\t");
            sb.append(CHART_DATA_KEY_PARAM); sb.append(": "); sb.append(chartDataKey);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_EVENTS_PARAM); sb.append(": "); sb.append(showEvents);
            log.debug( sb.toString() );
        }
    }
}

// EOF
