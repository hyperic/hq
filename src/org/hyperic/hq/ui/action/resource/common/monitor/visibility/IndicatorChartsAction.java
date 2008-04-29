/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.InvalidOptionException;

/**
 *
 * Generate the metric info for the indicator charts to be displayed
 */
public class IndicatorChartsAction extends DispatchAction 
    implements Serializable 
{
    private static Log log =
        LogFactory.getLog(IndicatorChartsAction.class.getName());
    
    private static String PREF_DELIMITER = Constants.DASHBOARD_DELIMITER;
    
    private static String DEFAULT_VIEW = 
        "resource.common.monitor.visibility.defaultview";
    
    protected ActionForward dispatchMethod(ActionMapping mapping,
                                           ActionForm form,
                                           HttpServletRequest request,
                                           HttpServletResponse response,
                                           String name)
        throws Exception {
        WebUser user = RequestUtils.getWebUser(request);
        Map pref = user.getMetricRangePreference(true);
        request.setAttribute(MonitorUtils.BEGIN, pref.get(MonitorUtils.BEGIN));
        request.setAttribute(MonitorUtils.END, pref.get(MonitorUtils.END));

        return super.dispatchMethod(mapping, form, request, response, name);
    }

    private List getMetrics(HttpServletRequest request, MeasurementBoss boss,
                            AppdefEntityID aeid, AppdefEntityTypeID ctype,
                            List tids)
        throws ServletException, SessionTimeoutException,
               SessionNotFoundException, AppdefEntityNotFoundException,
               PermissionException, AppdefCompatException, RemoteException {
        ArrayList metrics = new ArrayList();
        int sessionId = RequestUtils.getSessionId(request).intValue();

        // Get metric range defaults
        WebUser user = RequestUtils.getWebUser(request);
        Map pref = user.getMetricRangePreference(true);
        long begin = ((Long) pref.get(MonitorUtils.BEGIN)).longValue();
        long end = ((Long) pref.get(MonitorUtils.END)).longValue();
        
        // See if there are entities passed in
        AppdefEntityID[] eids = (AppdefEntityID[]) request.getSession()
            .getAttribute(aeid.getAppdefKey() + ".entities");
        
        ArrayList entList = null;
        if (eids != null) {
            entList = new ArrayList(Arrays.asList(eids));
        }
        
        for (Iterator it = tids.iterator(); it.hasNext(); ) {
            Integer tid = (Integer) it.next();
            
            try {
                MetricDisplaySummary mds;
                
                if (entList != null) {
                    if (ctype == null && aeid.getType() !=
                        AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                        // Not group or autogroup
                        entList.add(aeid);
                    }
                    
                    mds = boss.findMetric(sessionId, entList, tid, begin, end);
                }
                else
                    mds = boss.findMetric(sessionId, aeid, ctype, tid,
                        begin, end);

                if (mds != null) {
                    IndicatorDisplaySummary ids =
                        new IndicatorDisplaySummary(mds);
                    ids.setEntityId(aeid);
                    ids.setChildType(ctype);
                    metrics.add(ids);
                }
            } catch (MeasurementNotFoundException e) {
                // Probably deleted, just log it
                log.debug("Metric (" + tid + ") for " + aeid + " ctype " +
                          ctype + " not found");
            } catch (Exception e) {
                // No matter what happens, continue
                log.error("Unknown exception", e);
            }
        }

        return metrics;
    }
    
    private List getViewMetrics(HttpServletRequest request, AppdefEntityID aeid,
                                AppdefEntityTypeID ctype, String viewName)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, RemoteException, ServletException {
        MessageResources res = getResources(request);
        int sessionId = RequestUtils.getSessionId(request).intValue();
        MeasurementBoss boss =
            ContextUtils.getMeasurementBoss(getServlet().getServletContext());

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);
        WebUser user = RequestUtils.getWebUser(request);

        try {
            // First we try to find the metrics
            String metricsStr =
                user.getPreference(generatePrefsMetricsKey(key, viewName));

            // The metrics have to come from the preferences
            List metrics = StringUtil.explode(metricsStr, PREF_DELIMITER);
            
            ArrayList summaries = new ArrayList();
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                IndicatorDisplaySummary ids =
                    new IndicatorDisplaySummary((String) it.next());
                
                ArrayList tids = new ArrayList();
                tids.add(ids.getTemplateId());
                
                summaries.addAll(
                    this.getMetrics(request, boss, ids.getEntityId(),
                                    ids.getChildType(), tids));
            }
            
            return summaries;
        } catch (InvalidOptionException e) {
            // Maybe we have a "default" view
            if (viewName.equals(res.getMessage(DEFAULT_VIEW))) {
                ArrayList tids = new ArrayList();
                HashSet cats = new HashSet(4);
                cats.add(MeasurementConstants.CAT_AVAILABILITY);
                cats.add(MeasurementConstants.CAT_UTILIZATION);
                cats.add(MeasurementConstants.CAT_THROUGHPUT);
                cats.add(MeasurementConstants.CAT_PERFORMANCE);
        
                List tmpls;
                try {
                    if (ctype == null) {
                        tmpls = boss.getDesignatedTemplates(sessionId, aeid,
                                                            cats);
                    }
                    else {
                        tmpls = boss.getAGDesignatedTemplates(
                            sessionId, new AppdefEntityID[] { aeid }, ctype,
                            cats);
                    }
                    
                    for (Iterator it = tmpls.iterator(); it.hasNext(); ) {
                        MeasurementTemplate mtv =
                            (MeasurementTemplate) it.next();
                        
                        if (!mtv.getAlias().equalsIgnoreCase(
                                MeasurementConstants.CAT_AVAILABILITY))
                            tids.add(mtv.getId());
                    }
                } catch (MeasurementNotFoundException me) {
                    // No utilization metric
                    log.debug("Designated metrics not found for autogroup " +
                              ctype);
                }
                
                return this.getMetrics(request, boss, aeid, ctype, tids);
            }
        }
        
        // No metrics
        return new ArrayList();
    }

    private String generateSessionKey(HttpServletRequest request) {
        return generateUniqueKey(request) + ".view";
    }
    
    private String generateUniqueKey(HttpServletRequest request) {
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        try {
            // See if there's a ctype
            AppdefEntityTypeID ctype =
                RequestUtils.getChildResourceTypeId(request);
            return aeid.getAppdefKey() + "." + ctype.getAppdefKey();
        } catch (ParameterNotFoundException e) {
            // No problem, this is not an autogroup
            return aeid.getAppdefKey();
        }
    }
    
    private String generatePrefsMetricsKey(String key, String view) {
        return key + "." + view;
    }

    private void storeMetrics(HttpServletRequest request, List metrics,
                              IndicatorViewsForm form) {
        request.setAttribute(Constants.CHART_DATA_KEYS, metrics);
        
        String[] tmplIds = new String[metrics.size()];
        int i = 0;
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            tmplIds[i++] = summary.getTemplateId().toString();
        }
        form.setMetric(tmplIds);

        // Set the metrics in the session
        String key = this.generateSessionKey(request);
        request.getSession().setAttribute(key, metrics);
    }
    
    private List retrieveMetrics(HttpServletRequest request,
                                 IndicatorViewsForm form)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException,
               RemoteException, ServletException {
        String key = this.generateSessionKey(request);
        return (List) request.getSession().getAttribute(key);
    }

    private ActionForward error(ActionMapping mapping,
                                HttpServletRequest request, String key) {
        RequestUtils.setError(request, key);
        return mapping.findForward(Constants.MODE_MON_CUR);
    }
    
    public ActionForward fresh(ActionMapping mapping, ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        String viewName = ivf.getView();
        
        // Look up the metrics based on view name
        List metrics;
        try {
            // See if there's a ctype
            AppdefEntityTypeID childTypeId =
                RequestUtils.getChildResourceTypeId(request);
            metrics = this.getViewMetrics(request, aeid, childTypeId, viewName);
        } catch (ParameterNotFoundException e) {
            // No problem, this is not an autogroup
            metrics = this.getViewMetrics(request, aeid, null, viewName);
        }
        
        this.storeMetrics(request, metrics, ivf);
        
        return mapping.findForward(Constants.SUCCESS_URL);
    }

    public ActionForward refresh(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {        
        return fresh(mapping, form, request, response);
    }

    public ActionForward add(ActionMapping mapping, ActionForm form,
                             HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {        
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        
        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        if (metrics == null) {
            return mapping.findForward(Constants.FAILURE_URL);
        }
        
        MeasurementBoss boss =
            ContextUtils.getMeasurementBoss(getServlet().getServletContext());
        
        String newMetric = ivf.getAddMetric();
        
        // Parse the metric
        IndicatorDisplaySummary ids = new IndicatorDisplaySummary(newMetric);
        
        // Get the Metric Display summary (clearly, not taking into account the
        // appdefKey for now
        boolean found = false;
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (found = summary.getTemplateId().equals(ids.getTemplateId()))
                break;
        }

        // Add the new metrics
        if (!found) {
            ArrayList tids = new ArrayList();
            tids.add(ids.getTemplateId());
            metrics.addAll(getMetrics(request, boss, ids.getEntityId(),
                                      ids.getChildType(), tids));
        }

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.SUCCESS_URL);
    }

    public ActionForward remove(ActionMapping mapping, ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);
        
        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);
        
        // Go through and remove the metric
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
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

    public ActionForward moveUp(ActionMapping mapping, ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics =
            new MetricDisplaySummary[metrics.size()];
        
        Iterator it = metrics.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MetricDisplaySummary summary = (MetricDisplaySummary) it.next();
            if (summary.getTemplateId().equals(mid)) {
                orderedMetrics[i] = orderedMetrics[i - 1];
                orderedMetrics[i - 1] = summary;
            }
            else {
                orderedMetrics[i] = summary;
            }
        }

        metrics = new ArrayList(Arrays.asList(orderedMetrics));

        // Now store the metrics back
        this.storeMetrics(request, metrics, ivf);

        return mapping.findForward(Constants.AJAX_URL);
    }

    public ActionForward moveDown(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List metrics = this.retrieveMetrics(request, ivf);

        String oldMetric = ivf.getMetric()[0];
        Integer mid = new Integer(oldMetric);

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics =
            new MetricDisplaySummary[metrics.size()];

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

    public ActionForward go(ActionMapping mapping, ActionForm form,
                            HttpServletRequest request,
                            HttpServletResponse response)
        throws Exception {
        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    //this used to be in StringUtil but was only used here.
    //we should probably handle all user input cases the same,
    //escapeHTML if needed before save, unescapeHTML after retrieving.
    /**
     * Find characters having special meaning <em>inside</em> HTML tags
     * and URLs.
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
    private static int indexOfSpecialChars(String aTagFragment) {
        final StringCharacterIterator iterator =
            new StringCharacterIterator(aTagFragment);
        
        int i = 0;
        for (char character = iterator.current();
             character != StringCharacterIterator.DONE;
             character = iterator.next(), i++) {
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
     
    public ActionForward create(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);
        
        // A couple of checks
        if (ivf.getView().length() == 0) {
            return error(mapping, request,
                    "resource.common.monitor.visibility.view.error.empty");
        }
        
        if (indexOfSpecialChars(ivf.getView()) > -1) {
            return error(mapping, request, "error.input.badchars");            
        }
        
        String views = "";
        try {
            views = user.getPreference(key);
            
            if (views.length() > 0) {
                // Make sure that we're not duplicating names
                List viewNames = StringUtil.explode(views, PREF_DELIMITER);
                for (Iterator it = viewNames.iterator(); it.hasNext(); ) {
                    if (ivf.getView().equals(it.next())) {
                        return error (mapping, request,
                        "resource.common.monitor.visibility.view.error.exists");
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
    
    public ActionForward update(ActionMapping mapping, ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);
        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);

        // Now fetch the charts from the session
        List metrics = retrieveMetrics(request, ivf);
        
        StringBuffer viewMetrics = new StringBuffer();
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            viewMetrics.append(it.next()).append(PREF_DELIMITER);
        }

        // Set the user preferences now
        user.setPreference(generatePrefsMetricsKey(key, ivf.getView()),
                           viewMetrics.toString());
        
        AuthzBoss boss =
            ContextUtils.getAuthzBoss(getServlet().getServletContext());
        boss.setUserPrefs(user.getSessionId(), user.getId(),
                          user.getPreferences());            

        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    public ActionForward delete(ActionMapping mapping, ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = RequestUtils.getWebUser(request);

        String key = Constants.INDICATOR_VIEWS + generateUniqueKey(request);
        
        String views;
        try {
            views = user.getPreference(key);
        }
        catch (InvalidOptionException e) {
            // See, this is the "default"
            return mapping.findForward(Constants.MODE_MON_CUR);
        }
        
        // Parse the views
        List viewNames = StringUtil.explode(views, PREF_DELIMITER);

        for (Iterator it = viewNames.iterator(); it.hasNext(); ) {
            String view = (String) it.next();
            
            if (view.equals(ivf.getUpdate()))
                it.remove();
        }
        
        if (viewNames.size() > 0) {
            views = StringUtil.listToString(viewNames, PREF_DELIMITER); 
            user.setPreference(key, views);
        }
        else {
            user.unsetPreference(key);
        }

        // Now unset the metrics
        user.unsetPreference(
                key + generatePrefsMetricsKey(key, ivf.getUpdate()));

        AuthzBoss boss =
            ContextUtils.getAuthzBoss(getServlet().getServletContext());
        boss.setUserPrefs(user.getSessionId(), user.getId(),
                          user.getPreferences());            

        return mapping.findForward(Constants.MODE_MON_CUR);
    }

    public class IndicatorDisplaySummary extends MetricDisplaySummary
        implements Serializable {
        public static final String DELIMITER = ",";
        
        private AppdefEntityID entityId = null;
        private AppdefEntityTypeID childType = null;
        
        /** Constructor using a summary with data
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

            if (autogroup)
                this.childType = new AppdefEntityTypeID(st.nextToken());
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
        
        public String toString() {
            StringBuffer strBuf =
                new StringBuffer(getEntityId().getAppdefKey())
                .append(DELIMITER)
                .append(getTemplateId());
            
            if (getChildType() != null) {
                strBuf.append(DELIMITER).append(getChildType().getAppdefKey());                
            }
            
            return strBuf.toString();
        }
    }
}
