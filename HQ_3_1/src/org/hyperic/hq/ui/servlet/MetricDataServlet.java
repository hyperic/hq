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
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.PageControl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

public class MetricDataServlet extends HttpServlet {

    private static final SimpleDateFormat _df
        = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Log _log = LogFactory.getLog(MetricDataServlet.class);
    MeasurementBoss _mboss;
    AppdefBoss _aboss;

    public void init() {
        _mboss = ContextUtils.getMeasurementBoss(getServletContext());
        _aboss = ContextUtils.getAppdefBoss(getServletContext());
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException
    {
        int sessionId  = RequestUtils.getSessionId(request).intValue();
        WebUser user = RequestUtils.getWebUser(request);

        // Load required eid and m attributes.
        AppdefEntityID id;
        Integer mid;
        try {
            mid = RequestUtils.getIntParameter(request, "metricId");
            id = RequestUtils.getEntityId(request);
        } catch (ParameterNotFoundException e) {
            throw new ServletException("No metric parameter given.");
        }

        // Optional ctype
        AppdefEntityTypeID typeId = null;
        try {
            typeId = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e) {
            // Ok
        }

        // Load time range
        Map prefs = user.getMetricRangePreference();
        Long end = (Long)prefs.get(MonitorUtils.END);
        Long begin = (Long)prefs.get(MonitorUtils.BEGIN);

        // The list of resources to generate data for
        ArrayList resources = new ArrayList();
        if (typeId != null) {
            PageList children;
            try {
                children = _aboss.findChildResources(sessionId, id, typeId,
                                                     PageControl.PAGE_ALL);
            } catch (Exception e) {
                throw new ServletException("Error finding child resources.", e);
            }
            resources.addAll(children);
        } else if (id.isGroup()) {
            AppdefGroupValue gval;
            try {
                gval = _aboss.findGroup(sessionId, id.getId());
            } catch (Exception e) {
                throw new ServletException("Error finding group=" + id, e);
            }

            for (Iterator i = gval.getAppdefGroupEntries().iterator();
                 i.hasNext(); ) {
                try {
                    AppdefResourceValue val =
                        _aboss.findById(sessionId,(AppdefEntityID)i.next());
                    resources.add(val);  
                } catch (Exception e) {
                    throw new ServletException("Error finding group members",
                                               e);
                }
            }

        } else if (id.isPlatform() || id.isServer() || id.isService()) {
            AppdefResourceValue val;
            try {
                val = _aboss.findById(sessionId, id);
                resources.add(val);
            } catch (Exception e) {
                throw new ServletException("Error finding id=" + id);
            }

        } else {
            // XXX: Applications?
            throw new ServletException("Unhandled entity=" + id);
        }

        // Load template
        MeasurementTemplateValue templ;
        try {
            DerivedMeasurementValue dm = _mboss.getMeasurement(sessionId, mid);
            templ = dm.getTemplate();
        } catch (Exception e) {
            throw new ServletException("Error looking up measurement.", e);
        }

        ArrayList rows = new ArrayList();
        for (Iterator i = resources.iterator(); i.hasNext(); ) {
            AppdefResourceValue rValue = (AppdefResourceValue)i.next();
            DerivedMeasurementValue dm;
            try {
                dm = _mboss.findMeasurement(sessionId, templ.getId(),
                                            rValue.getEntityId());
                PageList list = _mboss.findMeasurementData(sessionId, dm.getId(),
                                                           begin.longValue(),
                                                           end.longValue(),
                                                           PageControl.PAGE_ALL);
                for (Iterator j = list.iterator(); j.hasNext(); ) {
                    HighLowMetricValue metric = (HighLowMetricValue)j.next();
                    String dateString =
                        _df.format(new Date(metric.getTimestamp()));

                    RowData row = new RowData(dateString);
                    if (rows.contains(row)) {
                        row = (RowData)rows.get(rows.indexOf(row));
                        row.addData(metric.getValue());
                    } else {
                        row.addData(metric.getValue());
                        rows.add(row);
                    }
                }
            } catch (Exception e) {
                throw new ServletException("Error loading measurement data.",
                                           e);
            }
        }

        StringBuffer buf = new StringBuffer();

        // Print header
        for (Iterator i = resources.iterator(); i.hasNext(); ) {
            AppdefResourceValue val = (AppdefResourceValue)i.next();
            buf.append(",").append(val.getName());
        }

        // Print data
        buf.append("\n");
        for (Iterator i = rows.iterator(); i.hasNext(); ) {
            RowData row = (RowData)i.next();
            buf.append(row.getDate());
            List data = row.getData();
            for (Iterator j = data.iterator(); j.hasNext(); ) {
                Double metricdata = (Double)j.next();
                buf.append(",").append(metricdata);
            }
            buf.append("\n");
        }

        try {
            response.setContentType("text/csv");
            response.addHeader("Content-disposition",
                               "attachment; filename=" +
                               templ.getAlias() + ".csv");
            response.getOutputStream().write(buf.toString().getBytes());
        } catch (IOException e) {
            throw new ServletException("Error writing data to the client: ", e);
        }
    }

    private class RowData {

        private String _date;
        private List _data;

        protected RowData(String date) {
            _date = date;
            _data = new ArrayList();
        }

        protected void setDate(String date) {
            _date = date;
        }

        protected String getDate() {
            return _date;
        }

        protected void addData(double data) {
            _data.add(new Double(data));
        }

        protected List getData() {
            return _data;
        }

        public boolean equals(Object other) {
            return ((RowData)other).getDate().equals(getDate());
        }

        public int hashCode() {
            return getDate().hashCode();
        }
    }
}
