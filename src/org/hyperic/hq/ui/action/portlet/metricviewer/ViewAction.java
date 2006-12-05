package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.units.FormattedNumber;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * This action class is used by the Metric Viewer portlet.  It's main
 * use is to generate the JSON objects required for display into the UI.
 */
public class ViewAction extends BaseAction {

    private Log _log = LogFactory.getLog("METRIC VIEWER");

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        WebUser user = (WebUser) request.getSession().getAttribute(
            Constants.WEBUSER_SES_ATTR);
        int sessionId = user.getSessionId().intValue();

        List entityIds =
            DashboardUtils.preferencesAsEntityIds(PropertiesForm.RESOURCES,
                                                  user);
        AppdefEntityID[] arrayIds =
            (AppdefEntityID[])entityIds.toArray(new AppdefEntityID[0]);

        int count = Integer.parseInt(user.
            getPreference(PropertiesForm.NUM_TO_SHOW));

        String metric = user.getPreference(PropertiesForm.METRIC);
        if (metric == null || metric.length() == 0) {
            return null;
        }
        Integer[] tids = new Integer[] { new Integer(metric) };
        PageList metricTemplates =
            mBoss.findMeasurementTemplates(sessionId, tids,
                                           PageControl.PAGE_ALL);
        MeasurementTemplateValue template =
            (MeasurementTemplateValue)metricTemplates.get(0);

        String resource = user.getPreference(PropertiesForm.RES_TYPE);
        AppdefEntityTypeID typeId = new AppdefEntityTypeID(resource);
        AppdefResourceTypeValue typeVal =
            appdefBoss.findResourceTypeById(sessionId, typeId);

        PageList resources = appdefBoss.findByIds(sessionId, arrayIds);
        TreeSet sortedSet = new TreeSet(new MetricSummaryComparator());
        for (Iterator i = resources.iterator(); i.hasNext(); ) {
            AppdefResourceValue rValue = (AppdefResourceValue)i.next();
            MetricValue[] val = mBoss.getLastMetricValue(sessionId,
                                                         rValue.getEntityId(),
                                                         tids);
            // handle DataNotAvailable
            if (val[0] != null) {
                MetricSummary summary = new MetricSummary(rValue, template,
                                                          val[0]);
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
        JSONObject res = new JSONObject();
        res.put("metricValues", metricValues);

        _log.info(res.toString(2));

        response.getWriter().write(res.toString());

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
                return 1;
            } else {
                return -1;
            }
        }
    }
}
