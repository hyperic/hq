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

import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.PageControl;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MetricDataServlet extends HttpServlet {

    private static final SimpleDateFormat _df
        = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MeasurementBoss _boss;

    public void init() {
        _boss = ContextUtils.getMeasurementBoss(getServletContext());
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException
    {
        int sessionId  = RequestUtils.getSessionId(request).intValue();
        WebUser user = RequestUtils.getWebUser(request);

        Integer mid;
        try {
            mid = RequestUtils.getIntParameter(request, "metricId");
        } catch (ParameterNotFoundException e) {
            throw new ServletException("No metric parameter given.");
        }

        Map prefs = user.getMetricRangePreference();
        Long end = (Long)prefs.get(MonitorUtils.END);
        Long begin = (Long)prefs.get(MonitorUtils.BEGIN);

        PageList list;
        try {
            list = _boss.findMeasurementData(sessionId, mid,
                                             begin.longValue(),
                                             end.longValue(),
                                             PageControl.PAGE_ALL);
        } catch (Exception e) {
            throw new ServletException("Error finding measurement data", e);
        }

        StringBuffer buf = new StringBuffer();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            HighLowMetricValue metric = (HighLowMetricValue)i.next();
            String dateString = _df.format(new Date(metric.getTimestamp()));
            buf.append(dateString)
                .append(",")
                .append(metric.getValue())
                .append("\n");
        }

        try {
            response.setContentType("text/csv");
            response.addHeader("Content-disposition",
                               "attachment; filename=data.csv");
            response.getOutputStream().write(buf.toString().getBytes());
        } catch (IOException e) {
            throw new ServletException("Error writing data to the client: ", e);
        }
    }
}
