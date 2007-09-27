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
import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.image.chart.AvailabilityChart;
import org.hyperic.image.chart.Chart;

/**
 * <p>Availability current health chart servlet.</p>
 *
 */
public class AvailHealthChartServlet extends CurrentHealthChartServlet {
    public AvailHealthChartServlet () {}

    /**
     * Create and return the chart.  This method will be called after
     * the parameters have been parsed.
     *
     * @return the newly created chart
     */
    protected Chart createChart(HttpServletRequest request,
                                ChartDataBean dataBean) {
        return new AvailabilityChart(getImageWidth(request),
                getImageHeight(request));
    }

    /**
     * Return the corresponding measurement category.
     *
     * @return <code>{@link org.hyperic.hq.measurement.MeasurementConstants.CAT_AVAILABILITY}</code>
     */
    protected String getMetricCategory() {
        return MeasurementConstants.CAT_AVAILABILITY;
    }
}

// EOF
