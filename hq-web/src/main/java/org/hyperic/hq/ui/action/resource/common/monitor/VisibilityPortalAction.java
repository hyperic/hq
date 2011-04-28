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

package org.hyperic.hq.ui.action.resource.common.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>BaseDispatchAction</code> that sets up common monitor portals.
 */
public class VisibilityPortalAction
    extends ResourceController {

    private static final String TITLE_EDIT_RANGE = "resource.common.monitor.visibility.MetricDisplayRangeTitle";

    private static final String PORTLET_EDIT_RANGE = ".resource.common.monitor.visibility.MetricDisplayRange";

    private static final String TITLE_CONFIGURE_VISIBILITY = "resource.common.monitor.visibility.MetricDisplayRangeTitle";

    private static final String PORTLET_CONFIGURE_VISIBILITY = ".resource.common.monitor.visibility.MetricDisplayRange";

    private static final String TITLE_CHART = "resource.common.monitor.visibility.ChartTitle";
    private static final String PORTLET_CHART_SMSR = ".resource.common.monitor.visibility.charts.metric.smsr";
    private static final String PORTLET_CHART_SMMR = ".resource.common.monitor.visibility.charts.metric.smmr";
    private static final String PORTLET_CHART_MMSR = ".resource.common.monitor.visibility.charts.metric.mmsr";

    private static final String TITLE_COMPARE_METRICS = "resource.common.monitor.visibility.CompareMetricsTitle";
    private static final String PORTLET_COMPARE_METRICS = ".resource.common.monitor.visibility.CompareMetrics";
    private static final String PORTLET_METRIC_METADATA = ".resource.common.monitor.visibility.MetricMetadata";
    private static final String TITLE_METRIC_METADATA = "resource.common.monitor.visibility.MetricMetadata";

    private final Log log = LogFactory.getLog(VisibilityPortalAction.class.getName());

    @Autowired
    public VisibilityPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
    }

    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_MON_EDIT_RANGE, "editRange");
        map.setProperty(Constants.MODE_CONFIGURE, "configureVisibility");
        map.setProperty(Constants.MODE_MON_CHART_SMSR, "chartSingleMetricSingleResource");
        map.setProperty(Constants.MODE_MON_CHART_SMMR, "chartSingleMetricMultiResource");
        map.setProperty(Constants.MODE_MON_CHART_MMSR, "chartMultiMetricSingleResource");
        map.setProperty(Constants.MODE_MON_COMPARE_METRICS, "compareMetrics");
        map.setProperty(Constants.MODE_MON_METRIC_METADATA, "metricMetadata");
        return map;
    }

    public ActionForward editRange(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal(TITLE_EDIT_RANGE, PORTLET_EDIT_RANGE);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward configureVisibility(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal(TITLE_CONFIGURE_VISIBILITY, PORTLET_CONFIGURE_VISIBILITY);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Chart a single metric for a single resource.
     */
    public ActionForward chartSingleMetricSingleResource(ActionMapping mapping, ActionForm form,
                                                         HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        try {
            setResource(request, response);
        } catch (AppdefEntityNotFoundException e) {
            // It's ok, we'll offer to delete the link
        }
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_SMSR);
        portal.setDialog(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String returnURL = SessionUtils.getReturnPath(request.getSession());
        if (returnURL != null)
            request.setAttribute(Constants.BACK_URL, returnURL);

        return null;
    }

    /**
     * Chart a single metric for a multiple resources.
     */
    public ActionForward chartSingleMetricMultiResource(ActionMapping mapping, ActionForm form,
                                                        HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        try {
            setResource(request, response);
        } catch (AppdefEntityNotFoundException e) {
            // It's ok, we'll offer to delete the link
        }
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_SMMR);
        portal.setDialog(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Chart multiple metrics for a single resource.
     */
    public ActionForward chartMultiMetricSingleResource(ActionMapping mapping, ActionForm form,
                                                        HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        try {
            setResource(request, response);
        } catch (AppdefEntityNotFoundException e) {
            // It's ok, we'll offer to delete the link
        }
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_MMSR);
        portal.setDialog(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String returnURL = SessionUtils.getReturnPath(request.getSession());
        if (returnURL != null)
            request.setAttribute(Constants.BACK_URL, returnURL);
        return null;
    }

    public ActionForward metricMetadata(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal(TITLE_METRIC_METADATA, PORTLET_METRIC_METADATA);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward compareMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal(TITLE_COMPARE_METRICS, PORTLET_COMPARE_METRICS);

        portal.setDialog(true);
        portal.setWorkflowPortal(true);

        // we potentially have a chained workflow happening here:
        // wherever we came from =>
        // compare metrics workflow =>
        // edit metric display range workflow
        //
        // first we set up a bunch of workflow params so that the
        // return path that goes to the compare metrics page has
        // all the state it needs
        //
        // then we push the return path that goes to wherever we came
        // from on to the compare metrics workflow
        //
        // next, ResourceController uses our workflow params to set up
        // the return path that goes to the compare metrics page. this
        // is used if we go to the edit display range page.
        //
        // finally, when the user clicks the "back to wherever we came
        // from" link on the compare metrics page, we have to pop the
        // return path for the compare metrics page off the compare
        // metrics workflow. this leaves only one return path: the one
        // that goes back to wherever we came from. this happens in
        // CompareMetricsAction.
        //
        // the last thing to remember is that the compare metrics page
        // has to use the same workflow property in struts-config.xml
        // as the edit metric display range page. this allows them to
        // utilize the same workflow stack.

        // set up workflow params that preserve state for compare
        // metrics page

        portal.setWorkflowParams(makeCompareWorkflowParams(request));

        // push old return path onto compare metrics workflow
        SessionUtils.pushWorkflow(request.getSession(false), mapping, Constants.WORKFLOW_COMPARE_METRICS_NAME);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    private Map<String, Object> makeCompareWorkflowParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        params.put(Constants.MODE_PARAM, RequestUtils.getMode(request));
        params.put(Constants.RESOURCE_PARAM, aeid.getId());
        params.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(aeid.getType()));
        params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, RequestUtils.getChildResourceTypeId(request));
        params.put("appdefTypeId", request.getParameter("appdefTypeId"));
        params.put("name", request.getParameter("name"));

        // make sure none of these values are duplicated
        String[] raw = request.getParameterValues("r");
        ArrayList<String> cooked = new ArrayList<String>();
        HashMap<String, String> idx = new HashMap<String, String>();
        for (int i = 0; i < raw.length; i++) {
            String val = raw[i];
            if (idx.get(val) == null) {
                cooked.add(val);
                idx.put(val, val);
            }
        }
        params.put("r", (String[]) cooked.toArray(new String[0]));

        return params;
    }

    /**
     * This sets the return path for a ResourceAction by appending the type and
     * resource id to the forward url.
     * 
     * @param request The current controller's request.
     * @param mapping The current controller's mapping that contains the input.
     * 
     * @exception ParameterNotFoundException if the type or id are not found
     * @exception ServletException If there is not input defined for this form
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, Map<String, Object> params)
        throws Exception {
        this.fetchReturnPathParams(request, params);
        String mode = (String) params.get(Constants.MODE_PARAM);
        if (mode != null && mode.startsWith("chart")) {
            // Don't save any path back to charts
            return;
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }
        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}
