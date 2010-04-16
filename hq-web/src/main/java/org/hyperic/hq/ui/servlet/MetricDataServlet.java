/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * The MetricDataServlet generates raw metric data in CVS format
 */
public class MetricDataServlet extends HttpServlet {

    private static final String CSV_DELIM = ",";

    private static final SimpleDateFormat _df
        = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Log _log = LogFactory.getLog(MetricDataServlet.class);

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
        List resources = new ArrayList();
        if (typeId != null) {
            try {
                resources.addAll(Bootstrap.getBean(AppdefBoss.class).findChildResources(sessionId, id, typeId,
                                                         PageControl.PAGE_ALL));
            } catch (Exception e) {
                throw new ServletException("Error finding child resources.", e);
            }
        } else if (id.isGroup()) {
            List entities;
            AppdefGroupValue gval;
            try {
                gval = Bootstrap.getBean(AppdefBoss.class).findGroup(sessionId, id.getId());
                entities = gval.getAppdefGroupEntries();
            } catch (Exception e) {
                throw new ServletException("Error finding group=" + id, e);
            }

            // HHQ-2865: Get list of checked resources to export.
            // The instanceIds request parameter is in javascript array format --> [...]
            String instanceIds = RequestUtils.getStringParameter(request, "instanceIds", "");            
            if (instanceIds.length() > 2) {
                try {
                    List requestedEntityIds = new ArrayList();
                    JSONArray arr = new JSONArray(instanceIds);
                    
                    for (int i = 0; i < arr.length(); i++) {                    
                        requestedEntityIds.add(
                            new AppdefEntityID(gval.getGroupEntType(),
                                               Integer.valueOf(arr.getInt(i))));
                    }
                    // Filter requested resource ids in case of invalid parameters
                    if (!requestedEntityIds.isEmpty()) {
                        entities.retainAll(requestedEntityIds);
                    }
                } catch (JSONException e) {
                    throw new ServletException("Error finding group resources.", e);
                }
            }
            
            for (Iterator i = entities.iterator(); i.hasNext();) {
                try {
                    resources.add(Bootstrap.getBean(AppdefBoss.class).findById(sessionId,
                                                  (AppdefEntityID) i.next()));  
                } catch (Exception e) {
                    throw new ServletException("Error finding group members",
                                               e);
                }
            }
        } else if (id.isPlatform() || id.isServer() || id.isService()) {
            AppdefResourceValue val;
            try {
                val = Bootstrap.getBean(AppdefBoss.class).findById(sessionId, id);
                resources.add(val);
            } catch (Exception e) {
                throw new ServletException("Error finding id=" + id);
            }

        } else {
            // XXX: Applications?
            throw new ServletException("Unhandled entity=" + id);
        }

        // Load template
        MeasurementTemplate templ;
        try {
            Measurement m = Bootstrap.getBean(MeasurementBoss.class).getMeasurement(sessionId, mid);
            templ = m.getTemplate();
        } catch (Exception e) {
            throw new ServletException("Error looking up measurement.", e);
        }

        List<RowData> rows = new ArrayList<RowData>();
        for (int i = 0; i < resources.size(); i++) {
            AppdefResourceValue rValue = (AppdefResourceValue) resources.get(i);
            try {
                List<HighLowMetricValue> list = null;
                try {
                    Measurement m = Bootstrap.getBean(MeasurementBoss.class).findMeasurement(sessionId, templ.getId(),rValue.getEntityId());
                    list = Bootstrap.getBean(MeasurementBoss.class).findMeasurementData(sessionId,m,begin.longValue(),end.longValue(), PageControl.PAGE_ALL);
                } catch (MeasurementNotFoundException mnfe) {
                    _log.debug(mnfe.getMessage());
                    // HHQ-3611: Measurement not found, so set data to an empty list
                    list = new ArrayList<HighLowMetricValue>(0);
                }
                List<RowData> hold = new ArrayList<RowData>();
                for (int j = 0; j < list.size(); j++) {
                    HighLowMetricValue metric = list.get(j);

                    RowData row = new RowData(new Date(metric.getTimestamp()));
                    if (rows.indexOf(row) > -1) {
                        row = (RowData) rows.remove(rows.indexOf(row));
                        row.addData(metric.getValue());
                    } else {
                        for (int f = 0; f < i; f++) {
                            row.addData(Double.NaN);
                        }
                        row.addData(metric.getValue());
                    }
                    // Move to hold list
                    hold.add(row);
                }
                
                // Go through the left-overs
                for (int j = 0; j < rows.size(); j++) {
                    RowData row = (RowData) rows.get(j);
                    row.addData(Double.NaN);
                    hold.add(row);
                }
                rows = hold;
            } catch (Exception e) {
                throw new ServletException("Error loading measurement data", e);
            }
        }

        StringBuffer buf = new StringBuffer();

        // Print header
        for (Iterator i = resources.iterator(); i.hasNext(); ) {
            AppdefResourceValue val = (AppdefResourceValue)i.next();
            buf.append(CSV_DELIM).append(val.getName());
        }

        // Print data, sorted from oldest to newest.
        Collections.sort(rows);
        
        buf.append("\n");
        for (int i = 0; i < rows.size(); i++) {
            RowData row = rows.get(i);
            buf.append(_df.format(row.getDate()));
            List<Double> data = row.getData();
            for (int j = 0; j < data.size(); j++) {
                Double metricdata = data.get(j);
                buf.append(CSV_DELIM);
                // Comparing to Double.NaN doesn't work
                if (!Double.isNaN(metricdata))
                    buf.append(metricdata);
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

    private class RowData implements Comparable {

        private Date _date;
        private List<Double> _data;

        protected RowData(Date date) {
            _date = date;
            _data = new ArrayList<Double>();
        }

        protected void setDate(Date date) {
            _date = date;
        }

        protected Date getDate() {
            return _date;
        }

        protected void addData(double data) {
            _data.add(new Double(data));
        }

        protected List<Double> getData() {
            return _data;
        }

        public boolean equals(Object other) {
            return ((RowData)other).getDate().equals(getDate());
        }

        public int hashCode() {
            return getDate().hashCode();
        }

        public int compareTo(Object o) {
            if (!(o instanceof RowData))
                return 0;
            
            return getDate().compareTo(((RowData) o).getDate());
        }
    }
}
