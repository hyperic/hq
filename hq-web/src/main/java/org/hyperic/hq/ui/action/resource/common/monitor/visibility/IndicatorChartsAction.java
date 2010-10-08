/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Generate the metric info for the indicator charts to be displayed
 */
public class IndicatorChartsAction
    extends DispatchAction implements Serializable {
    private final Log log = LogFactory.getLog(IndicatorChartsAction.class.getName());

    private MeasurementBoss measurementBoss;
    private AuthzBoss authzBoss;
    private TemplateManager templateManager;

    private static final String PREF_DELIMITER = Constants.DASHBOARD_DELIMITER;

    private static final String DEFAULT_VIEW = "resource.common.monitor.visibility.defaultview";

    @Autowired
    public IndicatorChartsAction(MeasurementBoss measurementBoss, AuthzBoss authzBoss, TemplateManager templateManager) {
        super();
        this.measurementBoss = measurementBoss;
        this.authzBoss = authzBoss;
        this.templateManager = templateManager;
    }

    protected ActionForward dispatchMethod(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                           HttpServletResponse response, String name) throws Exception {
        WebUser user = RequestUtils.getWebUser(request);
        Map<String, Object> pref = user.getMetricRangePreference(true);
        request.setAttribute(MonitorUtils.BEGIN, pref.get(MonitorUtils.BEGIN));
        request.setAttribute(MonitorUtils.END, pref.get(MonitorUtils.END));

        return super.dispatchMethod(mapping, form, request, response, name);
    }

    private List<IndicatorDisplaySummary> getMetrics(HttpServletRequest request, AppdefEntityID aeid, AppdefEntityTypeID ctype, List<Integer> tids)
        throws ServletException, SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException, AppdefCompatException, RemoteException {
        final boolean debug = log.isDebugEnabled();
    
        List<IndicatorDisplaySummary> result = new ArrayList<IndicatorDisplaySummary>();
        int sessionId = RequestUtils.getSessionId(request).intValue();


        // See if there are entities passed in
        AppdefEntityID[] eids = (AppdefEntityID[]) request.getSession().getAttribute(aeid.getAppdefKey() + ".entities");
        List<AppdefEntityID> entList = null;
        WebUser user = RequestUtils.getWebUser(request);
        Map<String, Object> pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);
        long interval = TimeUtil.getInterval(begin, end, Constants.DEFAULT_CHART_POINTS);
        PageControl pc = new PageControl(0, Constants.DEFAULT_CHART_POINTS);

        
        if (eids != null) {
            entList = new ArrayList<AppdefEntityID>(Arrays.asList(eids));
        }

        for (Integer tid : tids) {
            MeasurementTemplate template = templateManager.getTemplate(tid);
            List<HighLowMetricValue> data = new ArrayList<HighLowMetricValue>();
            try {
               

                if (entList != null) {
                    if (ctype == null && !aeid.isGroup()) {
                        // Not group or autogroup
                        entList.add(aeid);
                    }

                    data = measurementBoss.findMeasurementData(sessionId, tid, entList, begin, end, interval, true, pc);
                } else {
                    if (ctype != null) {
                       data = measurementBoss.findMeasurementData(sessionId, tid, aeid, ctype, begin, end, interval, true, pc);
                     } else {
                       data = measurementBoss.findMeasurementData(sessionId, tid, aeid, begin, end, interval, true, pc);
                     }
                }
                MetricDisplaySummary mds = getSummarizedMetricData(template, getAggregateData(template, data), begin, end, (entList == null) ? 0 : entList.size());
                if (mds != null) {
                    IndicatorDisplaySummary ids = new IndicatorDisplaySummary(mds, data);
                    ids.setEntityId(aeid);
                    ids.setChildType(ctype);
                    result.add(ids);
                }
            } catch (MeasurementNotFoundException e) {
                // Probably deleted, just log it
                if (debug) log.debug("Metric (" + tid + ") for " + aeid + " ctype " + ctype + " not found");
            } catch (Exception e) {
                // No matter what happens, continue
                log.error("Unknown exception", e);
            }
        }

        return result;
    }
    
    /**
     * TODO: The logic here is similar to DataManagerEJBImpl.getAggData().
     * Need to consolidate the code.
     * 
     */
    private double[] getAggregateData(MeasurementTemplate template, List<HighLowMetricValue> metricData) {
        final boolean debug = log.isDebugEnabled();
        
        if (metricData.size() == 0) {
            return null;
        }
        
        double high  = Double.MIN_VALUE;
        double low   = Double.MAX_VALUE;
        double total = 0;
        Double lastVal = null;
        int count = 0;
        long last = Long.MIN_VALUE;
        
        for (Iterator<HighLowMetricValue> it = metricData.iterator(); it.hasNext();) {
            final HighLowMetricValue mv = it.next();
            final double currentLowValue = mv.getLowValue();
            final double currentHighValue = mv.getHighValue();
            final double currentValue = mv.getValue();
            final int currentCount = mv.getCount();
            final long currentTimestamp = mv.getTimestamp();
            
            if (!Double.isNaN(currentLowValue) && !Double.isInfinite(currentLowValue)) {
                low = Math.min(mv.getLowValue(), low);
            }
            
            if (!Double.isNaN(currentHighValue) && !Double.isInfinite(currentHighValue)) {
                high = Math.max(mv.getHighValue(), high);
            }
            
            if (mv.getTimestamp() > last && !Double.isNaN(currentValue) && !Double.isInfinite(currentValue)) {
                lastVal = currentValue;
            }
                
            count += currentCount;
            
            if (!Double.isNaN(currentValue) && !Double.isInfinite(currentValue)) {
                total += currentValue * currentCount;
            }
            
            if (debug) {
                log.debug("Measurement=" + template.getName() 
                            + ", Current {Low=" + currentLowValue 
                            + ", High=" + currentHighValue 
                            + ", Value=" + currentValue 
                            + ", Count=" + currentCount 
                            + ", Timestamp=" + currentTimestamp
                            + "}, Summary {Low=" + low
                            + ", High=" + high
                            + ", Total=" + total
                            + ", Count=" + count
                            + "}");
            }
        }
        
        // This should only happen if every value in the array is NaN/Infinity...
        // ...if so, set low to zero
        if (low == Double.MAX_VALUE) {
            low = 0;
        }

        if (high == Double.MIN_VALUE) {
            high = 0;
        }

        // ...low should never be greater than high...
        assert(low <= high);
        
        double avg = 0.0d;
        
        // calculate average if count is more than 0...
        if (count > 0 && !(low == 0 && high == 0)) {
            avg = (double)total/count;
        }

        /* TODO Ensure this is true, have seen some odd cases where the data makes these assertion fail
        // ...low should never be greater than avg...
        assert(low <= avg);
        
        // ...avg should never be greater than high...
        assert(avg <= high);
        */
        
        final double[] data = new double[MeasurementConstants.IND_LAST_TIME + 1];
        
        data[MeasurementConstants.IND_MIN] = low;
        data[MeasurementConstants.IND_AVG] = avg;
        data[MeasurementConstants.IND_MAX] = high;
        data[MeasurementConstants.IND_CFG_COUNT] = count;
        
        if (lastVal != null) {
            data[MeasurementConstants.IND_LAST_TIME] = lastVal;
        }
        
        if (debug) {
            log.debug("Measurement=" + template.getName() 
                          + ", Last {Value=" + lastVal
                          + "}, Summary {Avg=" + avg
                          + ", Low=" + low
                          + ", High=" + high
                          + ", Total=" + total
                          + ", Count=" + count
                          + "}");
        }
        
        return data;    
    }
    
    /**
    * TODO: The logic here is similar to MeasurementProcessor.getMetricDisplaySummary().
    * Need to consolidate the code.
    */
    private MetricDisplaySummary getSummarizedMetricData(MeasurementTemplate template, double[] data, long begin, long end, int totalConfigured) {
        MetricDisplaySummary summary = new MetricDisplaySummary();
        
        // Set the time range
        summary.setBeginTimeFrame(begin);
        summary.setEndTimeFrame(end);
            
        // Set the template info
        summary.setLabel(template.getName());
        summary.setTemplateId(template.getId());
        summary.setTemplateCat(template.getCategory().getId());
        summary.setCategory(template.getCategory().getName());
        summary.setUnits(template.getUnits());
        summary.setCollectionType(new Integer(template.getCollectionType()));
        summary.setDesignated(Boolean.valueOf(template.isDesignate()));
        summary.setMetricSource(template.getMonitorableType().getName());
        
        // Not collecting, no interval
        summary.setCollecting(false);
        
        if (data != null) {
            // Set the data values
            summary.setMetric(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_MIN]));
            summary.setMetric(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_AVG]));
            summary.setMetric(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_MAX]));
            
            // Groups get sums, not last value
            if (totalConfigured == 1 || template.getCollectionType() == MeasurementConstants.COLL_TYPE_STATIC) {
                summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_LAST_TIME]));
            } else {
                // Percentage metrics (including Availability) do not need to be summed
                if (MeasurementConstants.UNITS_PERCENTAGE.equals(template.getUnits())) {
                    summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_AVG]));
                } else {
                    summary.setMetric(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(data[MeasurementConstants.IND_AVG] * data[MeasurementConstants.IND_CFG_COUNT]));
                }
            }

            // Number configured
            summary.setAvailUp((int) Math.round(data[MeasurementConstants.IND_CFG_COUNT]));
            summary.setAvailUnknown(new Integer(totalConfigured));
        }
        
        return summary;
    }


    private List<IndicatorDisplaySummary> getViewMetrics(HttpServletRequest request,
                                                         AppdefEntityID aeid,
                                                         AppdefEntityTypeID ctype,
                                                         IndicatorViewsForm indicatorViews)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException,
        AppdefCompatException, RemoteException, ServletException {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        MessageResources res = getResources(request);
        int sessionId = RequestUtils.getSessionId(request).intValue();

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);
        WebUser user = RequestUtils.getWebUser(request);
        List<IndicatorDisplaySummary> summaries = new ArrayList<IndicatorDisplaySummary>();
        
        // for groups, we get the chart data one at a time to improve UI performance
        // when rendering the charts
        if (aeid.isGroup() || ctype != null) {
            if (indicatorViews.getDisplaySize() > 0) {
                summaries = this.retrieveMetrics(request, indicatorViews);
            } else {
                // first view should return one so that the charts will
                // progressively load one by one using ajax
                indicatorViews.setDisplaySize(1);
            }
        }

        try {
            // First we try to find the metrics
            String metricsStr = user.getPreference(generatePrefsMetricsKey(key, indicatorViews.getView()));

            // The metrics have to come from the preferences
            List<String> metrics = StringUtil.explode(metricsStr, PREF_DELIMITER);
            
            for (String m : metrics) {
                IndicatorDisplaySummary ids = new IndicatorDisplaySummary(m);

                if (indicatorViews.getDisplaySize() > 0 &&
                    indicatorDisplaySummaryExists(ids.getTemplateId(), summaries)) {
                    continue;
                }
                
                List<Integer> tids = new ArrayList<Integer>();
                tids.add(ids.getTemplateId());
                
                String label = "getMetrics[" + m + "]";
                watch.markTimeBegin(label);
                summaries.addAll(this.getMetrics(request, ids.getEntityId(), ids.getChildType(), tids));
                watch.markTimeEnd(label);
                                
                if (indicatorViews.getDisplaySize() > 0) {
                    if (summaries.size() >= metrics.size()) {
                        // all summaries fetched
                        indicatorViews.setDisplaySize(-1);
                    } else if (summaries.size() == indicatorViews.getDisplaySize()) {
                        break;
                    }
                }
            }
        } catch (InvalidOptionException e) {
            // Maybe we have a "default" view
            if (indicatorViews.getView().equals(res.getMessage(DEFAULT_VIEW))) {
                ArrayList<Integer> tids = new ArrayList<Integer>();
                HashSet<String> cats = new HashSet<String>(4);
                cats.add(MeasurementConstants.CAT_AVAILABILITY);
                cats.add(MeasurementConstants.CAT_UTILIZATION);
                cats.add(MeasurementConstants.CAT_THROUGHPUT);
                cats.add(MeasurementConstants.CAT_PERFORMANCE);

                List<MeasurementTemplate> tmpls = new ArrayList<MeasurementTemplate>();
                try {
                    if (ctype == null) {
                        tmpls = measurementBoss.getDesignatedTemplates(sessionId, aeid, cats);
                    } else {
                        tmpls = measurementBoss.getAGDesignatedTemplates(sessionId, new AppdefEntityID[] { aeid },
                            ctype, cats);
                    }
                    
                    for (MeasurementTemplate mtv : tmpls) {
                        if (indicatorViews.getDisplaySize() > 0 &&
                            indicatorDisplaySummaryExists(mtv.getId(), summaries)) {
                            continue;
                        }
                        
                        if (!mtv.getAlias().equalsIgnoreCase(MeasurementConstants.CAT_AVAILABILITY)) {
                            tids.add(mtv.getId());
                            if (indicatorViews.getDisplaySize() > 0) {
                                break;
                            }
                        }
                    }
                } catch (MeasurementNotFoundException me) {
                    // No utilization metric
                    if (debug) log.debug("Designated metrics not found for autogroup " + ctype);
                }

                watch.markTimeBegin("getMetrics");
                summaries.addAll(this.getMetrics(request, aeid, ctype, tids)); 
                watch.markTimeEnd("getMetrics");
                
                if ((indicatorViews.getDisplaySize() > 0) && (summaries.size() >= tmpls.size() - 1)) {
                    // all summaries fetched
                    indicatorViews.setDisplaySize(-1);
                }
            }
        } finally {
            if (debug) {
                log.debug("IndicatorChartsAction.getViewMetrics: " + watch);
            }
        }

        return summaries;
    }

    private boolean indicatorDisplaySummaryExists(Integer templateId,
                                                  List<IndicatorDisplaySummary> summaries) {
        boolean found = false;
        for (IndicatorDisplaySummary summary : summaries) {
            if (summary.getTemplateId().equals(templateId)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private String generateUniqueKey(HttpServletRequest request) {
        String sessionKey = RequestUtils.generateSessionKey(request);
        // -5 is the ".view" bit
        return sessionKey.substring(0, sessionKey.length() - 5);
    }

    private String generatePrefsMetricsKey(String key, String view) {
        return key + "." + view;
    }

    private void storeMetrics(HttpServletRequest request, List<IndicatorDisplaySummary> metrics, IndicatorViewsForm form) {
        request.setAttribute(Constants.CHART_DATA_KEYS, metrics);

        String[] tmplIds = new String[metrics.size()];
        int i = 0;
        for (MetricDisplaySummary summary : metrics) {

            tmplIds[i++] = summary.getTemplateId().toString();
        }
        form.setMetric(tmplIds);

        // Set the metrics in the session
        String key = RequestUtils.generateSessionKey(request);
        request.getSession().setAttribute(key, metrics);
    }

    private List<IndicatorDisplaySummary> retrieveMetrics(HttpServletRequest request, IndicatorViewsForm form) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, PermissionException, RemoteException, ServletException {
        String key = RequestUtils.generateSessionKey(request);
        return (List<IndicatorDisplaySummary>) request.getSession().getAttribute(key);
    }

    private ActionForward error(ActionMapping mapping, HttpServletRequest request, String key) {
        RequestUtils.setError(request, key);
        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    public ActionForward fresh(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics based on view name
        List<IndicatorDisplaySummary> metrics;
        try {
            // See if there's a ctype
            AppdefEntityTypeID childTypeId = RequestUtils.getChildResourceTypeId(request);
            metrics = this.getViewMetrics(request, aeid, childTypeId, ivf);
        } catch (ParameterNotFoundException e) {
            // No problem, this is not an autogroup
            metrics = this.getViewMetrics(request, aeid, null, ivf);
        }
        
        this.storeMetrics(request, metrics, ivf);
        if (debug) log.debug("IndicatorChartsAction.fresh: " + watch);
        
        if ("json".equals(ivf.getOutput())) {
            IndicatorDisplaySummary metric = metrics.get(metrics.size()-1);
            JSONObject json = metric.toJSON();            
            json.put("index", metrics.size()-1);
            json.put("displaySize", ivf.getDisplaySize());
            json.put("timeToken", ivf.getTimeToken());
            
            request.setAttribute(Constants.AJAX_JSON, json);
            return mapping.findForward("json");
        } else {
            return mapping.findForward(Constants.SUCCESS_URL);
        }
    }

    public ActionForward add(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        if (metrics == null) {
            return mapping.findForward(Constants.FAILURE_URL);
        }

        String newMetric = ivf.getAddMetric();

        // Parse the metric
        IndicatorDisplaySummary ids = new IndicatorDisplaySummary(newMetric);

        // Get the Metric Display summary (clearly, not taking into account the
        // appdefKey for now
        boolean found = false;
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (found = summary.getTemplateId().equals(ids.getTemplateId()))
                break;
        }

        // Add the new metrics
        if (!found) {
            ArrayList<Integer> tids = new ArrayList<Integer>();
            tids.add(ids.getTemplateId());
            metrics.addAll(getMetrics(request, ids.getEntityId(), ids.getChildType(), tids));
        }

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.SUCCESS_URL);
    }

    public ActionForward remove(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);

        // Go through and remove the metric
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (summary.getTemplateId().equals(mid)) {
                it.remove();
                break;
            }
        }

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.AJAX_URL);
    }

    public ActionForward moveUp(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics = new MetricDisplaySummary[metrics.size()];

        Iterator it = metrics.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (summary.getTemplateId().equals(mid)) {
                orderedMetrics[i] = orderedMetrics[i - 1];
                orderedMetrics[i - 1] = summary;
            } else {
                orderedMetrics[i] = summary;
            }
        }

        metrics = new ArrayList(Arrays.asList(orderedMetrics));

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.AJAX_URL);
    }

    public ActionForward moveDown(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics = new MetricDisplaySummary[metrics.size()];

        Iterator it = metrics.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (summary.getTemplateId().equals(mid) && it.hasNext())
                orderedMetrics[i++] = (MetricDisplaySummary) it.next();

            orderedMetrics[i] = summary;
        }

        metrics = new ArrayList(Arrays.asList(orderedMetrics));

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.AJAX_URL);
    }

    public ActionForward go(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    // this used to be in StringUtil but was only used here.
    // we should probably handle all user input cases the same,
    // escapeHTML if needed before save, unescapeHTML after retrieving.
    /**
     * Find characters having special meaning <em>inside</em> HTML tags and
     * URLs.
     * 
     * <P>
     * The special characters are :
     * <ul>
     * <li><
     * <li>>
     * <li>"
     * <li>'
     * <li>\
     * <li>&
     * <li>|
     * <li>?
     * </ul>
     * 
     * <P>
     */
    private int indexOfSpecialChars(String aTagFragment) {
        final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);

        int i = 0;
        for (char character = iterator.current(); character != StringCharacterIterator.DONE; character = iterator
            .next(), i++) {
            switch (character) {
                case '<':
                case '>':
                case '\"':
                case '\'':
                case '\\':
                case '&':
                case '|':
                case '?':
                    return i;
                default:
                    break;
            }
        }
        return -1;
    }

    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);

        // A couple of checks
        if (ivf.getView().length() == 0) {
            return error(mapping, request, "resource.common.monitor.visibility.view.error.empty");
        }

        if (indexOfSpecialChars(ivf.getView()) > -1) {
            return error(mapping, request, "error.input.badchars");
        }

        String views = "";
        try {
            views = user.getPreference(key);

            if (views.length() > 0) {
                // Make sure that we're not duplicating names
                List<String> viewNames = StringUtil.explode(views, PREF_DELIMITER);
                for (Iterator<String> it = viewNames.iterator(); it.hasNext();) {
                    if (ivf.getView().equals(it.next())) {
                        return error(mapping, request, "resource.common.monitor.visibility.view.error.exists");
                    }
                }

                views += PREF_DELIMITER;
            }
        } catch (InvalidOptionException e) {
            // If this is the first new one, then let's create a default one,
            // too
            MessageResources res = getResources(request);
            String defName = res.getMessage(Constants.DEFAULT_INDICATOR_VIEW);

            if (!defName.equals(ivf.getView()))
                views = defName + PREF_DELIMITER;
        }

        views += ivf.getView();
        user.setPreference(key, views);

        // Call update to save the metrics to be viewed
        return update(mapping, ivf, request, response);
    }

    public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);
        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);

        // Now fetch the charts from the session
        List metrics = retrieveMetrics(request, ivf);

        StringBuffer viewMetrics = new StringBuffer();
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            viewMetrics.append(it.next()).append(PREF_DELIMITER);
        }

        // Set the user preferences now
        user.setPreference(generatePrefsMetricsKey(key, ivf.getView()), viewMetrics.toString());

        authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user.getPreferences());

        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    public ActionForward delete(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);

        String views;
        try {
            views = user.getPreference(key);
        } catch (InvalidOptionException e) {
            // See, this is the "default"
            return mapping.findForward(Constants.MODE_MON_CUR);
        }

        // Parse the views
        List<String> viewNames = StringUtil.explode(views, PREF_DELIMITER);

        for (Iterator<String> it = viewNames.iterator(); it.hasNext();) {
            String view = (String) it.next();

            if (view.equals(ivf.getUpdate()))
                it.remove();
        }

        if (viewNames.size() > 0) {
            views = StringUtil.listToString(viewNames, PREF_DELIMITER);
            user.setPreference(key, views);
        } else {
            user.unsetPreference(key);
        }

        // Now unset the metrics
        user.unsetPreference(key + generatePrefsMetricsKey(key, ivf.getUpdate()));

        authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user.getPreferences());

        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    public class IndicatorDisplaySummary
        extends MetricDisplaySummary implements Serializable {
        public static final String DELIMITER = ",";

        private AppdefEntityID entityId = null;
        private AppdefEntityTypeID childType = null;
        private List<HighLowMetricValue> highLowMetrics = new ArrayList<HighLowMetricValue>();
        
        /** Constructor using a summary with data
         * @param summary the actual data
         */
        public IndicatorDisplaySummary(MetricDisplaySummary summary, List<HighLowMetricValue> data) {
           this(summary);
           this.highLowMetrics = data;
        }

        /**
         * Constructor using a summary with data
         * @param summary the actual data
         */
        public IndicatorDisplaySummary(MetricDisplaySummary summary) {
            super();
            super.setAlertCount(summary.getAlertCount());
            super.setOobCount(summary.getOobCount());
            super.setBeginTimeFrame(summary.getBeginTimeFrame());
            super.setCollectionType(summary.getCollectionType());
            super.setDesignated(summary.getDesignated());
            super.setDisplayUnits(summary.getDisplayUnits());
            super.setEndTimeFrame(summary.getEndTimeFrame());
            super.setLabel(summary.getLabel());
            super.setMetrics(summary.getMetrics());
            super.setMetricSource(summary.getMetricSource());
            super.setShowNumberCollecting(summary.getShowNumberCollecting());
            super.setTemplateCat(summary.getTemplateCat());
            super.setTemplateId(summary.getTemplateId());
            super.setUnits(summary.getUnits());
            super.setAvailDown(summary.getAvailDown());
            super.setAvailUnknown(summary.getAvailUnknown());
            super.setAvailUp(summary.getAvailUp());
        }

        protected IndicatorDisplaySummary(String token) {
            StringTokenizer st = new StringTokenizer(token, DELIMITER);
            boolean autogroup = st.countTokens() > 2;

            this.entityId = new AppdefEntityID(st.nextToken());
            this.setTemplateId(new Integer(st.nextToken()));

            if (autogroup) {
                this.childType = new AppdefEntityTypeID(st.nextToken());
            }
        }

        public AppdefEntityTypeID getChildType() {
            return childType;
        }

        public void setChildType(AppdefEntityTypeID childType) {
            this.childType = childType;
        }

        public AppdefEntityID getEntityId() {
            return entityId;
        }

        public void setEntityId(AppdefEntityID entityId) {
            this.entityId = entityId;
        }

        public int getUnitUnits() {
            return UnitsConvert.getUnitForUnit(getUnits());
        }

        public int getUnitScale() {
            return UnitsConvert.getScaleForUnit(getUnits());
        }
        
        public List<HighLowMetricValue> getHighLowMetrics() {
            return highLowMetrics;
        }

        public String toString() {
            StringBuffer strBuf = new StringBuffer(getEntityId().getAppdefKey()).append(DELIMITER).append(
                getTemplateId());

            if (getChildType() != null) {
                strBuf.append(DELIMITER).append(getChildType().getAppdefKey());
            }

            return strBuf.toString();
        }
        
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {                
                json.put("entityId", getEntityId().toString());
                json.put("entityType", getEntityId().getType());
                
                if (getChildType() != null) {
                    json.put("ctype", getChildType().toString());
                }
                
                json.put("metricId", getTemplateId());
                json.put("metricLabel", getLabel());
                json.put("metricSource", getMetricSource());
                json.put("minMetric", getMinMetric().getValueFmt().toString());
                json.put("avgMetric", getAvgMetric().getValueFmt().toString());
                json.put("maxMetric", getMaxMetric().getValueFmt().toString());
                json.put("unitUnits", getUnitUnits());
                json.put("unitScale", getUnitScale());
           } catch (JSONException e) {
                throw new SystemException(e);
            }
            return json;
        }
    }
}
