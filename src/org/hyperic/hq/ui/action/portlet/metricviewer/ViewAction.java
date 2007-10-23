/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.metricviewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.units.FormattedNumber;
import org.json.JSONObject;

/**
 * This action class is used by the Metric Viewer portlet.  It's main
 * use is to generate the JSON objects required for display into the UI.
 */
public class ViewAction extends BaseAction {

    private static Log _log = LogFactory.getLog(ViewAction.class);

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        AuthzBoss aBoss = ContextUtils.getAuthzBoss(ctx);
        HttpSession session = request.getSession();
        WebUser user = (WebUser) request.getSession().getAttribute(
                Constants.WEBUSER_SES_ATTR);
        DashboardConfig dashConfig = DashboardUtils.findDashboard(
        		(Integer)session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
        		user, aBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        int sessionId = user.getSessionId().intValue();
        long ts = System.currentTimeMillis();
        String token;
        try {
            token = RequestUtils.getStringParameter(request, "token");
        } catch (ParameterNotFoundException e) {
            token = null;
        }

        // For multi-portlet configuration
        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String resTypeKey = PropertiesForm.RES_TYPE;
        String metricKey = PropertiesForm.METRIC;
        String descendingKey = PropertiesForm.DECSENDING;
        String titleKey = PropertiesForm.TITLE;

        if (token != null) {
            numKey += token;
            resKey += token;
            resTypeKey += token;
            metricKey += token;
            descendingKey += token;
            titleKey += token;
        }

        JSONObject res = new JSONObject();
        if (token != null) {
            res.put("token", token);
        } else {
            res.put("token", JSONObject.NULL);
        }

        res.put("title", dashPrefs.getValue(titleKey, ""));
        
        // Load resources
        List entityIds = DashboardUtils.preferencesAsEntityIds(resKey, dashPrefs);
        AppdefEntityID[] arrayIds =
            (AppdefEntityID[])entityIds.toArray(new AppdefEntityID[0]);
        int count = Integer.parseInt(dashPrefs.getValue(numKey, "10"));
        String metric = dashPrefs.getValue(metricKey, "");
        boolean isDescending =
            Boolean.valueOf(dashPrefs.getValue(descendingKey, "true")).booleanValue();

        // Validate
        if (arrayIds.length == 0 || count == 0 || metric.length() == 0) {
            res.put("metricValues", new JSONObject());
            response.getWriter().write(res.toString());
            return null;
        }

        Integer[] tids = new Integer[] { new Integer(metric) };
        PageList metricTemplates =
            mBoss.findMeasurementTemplates(sessionId, tids,
                                           PageControl.PAGE_ALL);
        MeasurementTemplateValue template =
            (MeasurementTemplateValue)metricTemplates.get(0);

        String resource = dashPrefs.getValue(resTypeKey);
        AppdefEntityTypeID typeId = new AppdefEntityTypeID(resource);
        AppdefResourceTypeValue typeVal =
            appdefBoss.findResourceTypeById(sessionId, typeId);
        CacheData[] data = new CacheData[arrayIds.length];
        Integer[] mids = new Integer[arrayIds.length];
        long interval = 0;
        ArrayList toRemove = new ArrayList();
        for (int i = 0; i < arrayIds.length; i++) {
            AppdefEntityID id = arrayIds[i];
            try {
                data[i] = loadData(sessionId, id, template);
            } catch (AppdefEntityNotFoundException e) {
                toRemove.add(id.getAppdefKey());
            }
            if (data[i] != null) {
                mids[i] = data[i].getMetricId();

                if (data[i].getInterval() > interval) {
                    interval = data[i].getInterval();
                }
            } else {
                mids[i] = null;
            }
        }

        MetricValue[] vals = mBoss.getLastMetricValue(sessionId, mids,
                                                      interval);
        TreeSet sortedSet =
            new TreeSet(new MetricSummaryComparator(isDescending));
        for (int i = 0; i < data.length; i++) {
            // Only show resources with data
            if (vals[i] != null) {
                MetricSummary summary = new MetricSummary(data[i].getResource(),
                                                          template, vals[i]);
                sortedSet.add(summary);
            }
        }

        JSONObject metricValues = new JSONObject();
        metricValues.put("resourceTypeName", typeVal.getName());
        metricValues.put("metricName", template.getName());
        ArrayList values = new ArrayList();
        for (Iterator i = sortedSet.iterator(); i.hasNext() && count-- > 0; ) {
            MetricSummary s = (MetricSummary)i.next();
            JSONObject val = new JSONObject();
            val.put("value", s.getFormattedValue());
            val.put("resourceId", s.getAppdefResourceValue().getId());
            val.put("resourceTypeId",
                    s.getAppdefResourceValue().getEntityId().getType());
            val.put("resourceName", s.getAppdefResourceValue().getName());
            values.add(val);
        }
        metricValues.put("values", values);
        res.put("metricValues", metricValues);

        response.getWriter().write(res.toString());

        _log.debug("Metric viewer loaded in " +
                   (System.currentTimeMillis() - ts) + " ms.");

        if (toRemove.size() > 0) {
            _log.debug("Removing " + toRemove.size() + " missing resources.");
            DashboardUtils.removeResources((String[])toRemove.toArray(new String[0]),
                                           resKey, dashPrefs);
        }

        return null;
    }

    private class MetricSummary {
        private AppdefResourceValue _resource;
        private MeasurementTemplateValue _template;
        private MetricValue _val;

        public MetricSummary(AppdefResourceValue resource,
                             MeasurementTemplateValue template,
                             MetricValue val) {
            _resource = resource;
            _template = template;
            _val = val;
        }

        public AppdefResourceValue getAppdefResourceValue() {
            return _resource;
        }

        public MetricValue getMetricValue() {
            return _val;
        }

        public String getFormattedValue() {
            FormattedNumber fn = UnitsConvert.convert(_val.getValue(),
                                                      _template.getUnits());
            return fn.toString();
        }

        public String toString() {
            return "[" + _resource.getEntityId() + "]=" + _val.getValue();
        }
    }

    private class MetricSummaryComparator implements Comparator {

        private boolean _decending;

        public MetricSummaryComparator(boolean decending) {
            _decending = decending;
        }

        public int compare(Object o1, Object o2) {
            MetricSummary s1 = (MetricSummary)o1;
            MetricSummary s2 = (MetricSummary)o2;

            MetricValue m1 = s1.getMetricValue();
            MetricValue m2 = s2.getMetricValue();

            if (m1.getValue() == m2.getValue()) {
                String n1 = s1.getAppdefResourceValue().getName();
                String n2 = s2.getAppdefResourceValue().getName();
                return n1.compareTo(n2);
            } else if (m1.getValue() < m2.getValue()) {
                if (_decending) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if (_decending) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }

    private CacheData loadData(int sessionId, AppdefEntityID id,
                               MeasurementTemplateValue template)
        throws AppdefEntityNotFoundException
    {
        Cache cache = CacheManager.getInstance().getCache("MetricViewer");
        CacheKey key = new CacheKey(sessionId, id, template);
        Element e = cache.get(key);

        if (e != null) {
            return (CacheData)e.getObjectValue();
        }

        // Otherwise, load from the backend
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss      aBoss = ContextUtils.getAppdefBoss(ctx);
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);

        try {
            AppdefResourceValue val = aBoss.findById(sessionId, id);
            DerivedMeasurementValue dm =
                mBoss.getMeasurement(sessionId, id, template.getAlias());
            if (dm == null) {
                return null; // No metric scheduled.
            }

            CacheData data = new CacheData(val, dm.getId(), dm.getInterval());
            cache.put(new Element(key, data));
            return data;
        } catch (AppdefEntityNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            _log.debug("Caught exception loading data: " + ex, ex);
            return null;
        }
    }
    
    // Classes for caching dashboard data

    public class CacheKey {
        private int _sessionId;
        private AppdefEntityID _id;
        private MeasurementTemplateValue _template;

        public CacheKey(int sessionId, AppdefEntityID id,
                        MeasurementTemplateValue template) {
            _sessionId = sessionId;
            _id = id;
            _template = template;
        }

        public boolean equals(Object o) {
            return (o instanceof CacheKey) &&
                ((CacheKey)o)._id.equals(_id) &&
                ((CacheKey)o)._template.equals(_template) &&
                ((CacheKey)o)._sessionId == _sessionId;
        }

        public int hashCode() {
            return _id.hashCode() + _template.hashCode() + _sessionId;
        }
    }

    public class CacheData {
        private AppdefResourceValue _resource;
        private Integer _metricId;
        private long _interval;

        public CacheData(AppdefResourceValue resource,
                         Integer metricId, long interval) {
            _resource = resource;
            _metricId = metricId;
            _interval = interval;
        }

        public AppdefResourceValue getResource() {
            return _resource;
        }

        public Integer getMetricId() {
            return _metricId;
        }

        public long getInterval() {
            return _interval;
        }
    }
}
