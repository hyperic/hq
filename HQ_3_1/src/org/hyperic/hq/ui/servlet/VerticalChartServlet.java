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

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.image.chart.Chart;
import org.hyperic.image.chart.Trend;
import org.hyperic.image.chart.VerticalChart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Abstract base class for vertical charts.</p>
 *
 * <p>The chart servlet takes the following parameters (any applicable
 * defaults are in <b>bold</b> and required parameters are in
 * <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                       </th></tr>
 * <tr><td> collectionType   </td><td> &lt;integer <b>(0)</b>&gt;* </td></tr>
 * </table>
 *
 * <p>* Must be a valid value from <code>{@link
 * org.hyperic.hq.measurement.MeasurementConstants</code>.</p>
 *
 * @see org.hyperic.hq.measurement.MeasurementConstants
 */
public abstract class VerticalChartServlet extends ChartServlet {
    /** Request parameter for unit scale. */
    public static final String COLLECTION_TYPE_PARAM = "collectionType";

    // member data
    private Log log = LogFactory.getLog( VerticalChartServlet.class.getName() );
    private static ThreadLocal collectionType = new ThreadLocal(){
        protected Object initialValue(){
            return new Integer(0);
        }
    };

    public VerticalChartServlet () {}

    /**
     * Return the default <code>collectionType</code>.
     */
    protected int getDefaultCollectionType() {
        return MeasurementConstants.COLL_TYPE_DYNAMIC;
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        super.parseParameters(request);

        // cumulative trend
        collectionType.set(new Integer(parseIntParameter( request, COLLECTION_TYPE_PARAM,
                                            getDefaultCollectionType() )));
        _logParameters();
    }

    /**
     * Initialize the chart.  This method will be called after the
     * parameters have been parsed and the chart has been created.
     *
     * @param chart the chart
     */
    protected void initializeChart(Chart chart, HttpServletRequest request) {
        super.initializeChart(chart, request);

        VerticalChart verticalChart = (VerticalChart) chart;
        int cumulativeTrend = getTrendForCollectionType(((Integer)collectionType.get()).intValue());
        verticalChart.setCumulativeTrend(cumulativeTrend);
    }

    /**
     * Get the trend based on the collection type.  If the collection
     * type is invalid, it will return <code>TREND_NONE</code>.
     *
     * @param collectionType the collection type from <code>{@link
     * org.hyperic.hq.measurement.MeasurementConstants}</code>
     * @return the trend from <code>{@link
     * net.covalent.chart.Trend}</code>
     * @see org.hyperic.hq.measurement.MeasurementConstants
     * @see net.covalent.chart.Trend
     */
    protected int getTrendForCollectionType(int collectionType) {
        int trend = Trend.TREND_NONE;
        switch (collectionType) {
        case MeasurementConstants.COLL_TYPE_DYNAMIC:
        case MeasurementConstants.COLL_TYPE_STATIC:
            trend = Trend.TREND_NONE;
            break;
        case MeasurementConstants.COLL_TYPE_TRENDSUP:
            trend = Trend.TREND_UP;
            break;
        case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
            trend = Trend.TREND_DOWN;
            break;
        default:
            log.warn("Invalid collection type: " + collectionType);
            break;
        }
        return trend;
    }

    private void _logParameters() {
        if ( log.isDebugEnabled() ) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");sb.append("\t");
            sb.append(COLLECTION_TYPE_PARAM); sb.append(": "); sb.append(((Integer)collectionType.get()).intValue());
            log.debug( sb.toString() );
        }
    }
}

// EOF
