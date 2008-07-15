package org.hyperic.hq.ui.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.ServiceConstants;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.session.DashboardPortletBossEJBImpl;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossLocal;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The UI Dashboard Widgets Service
 * 
 */
public class RESTService extends BaseService {

    private static Log log = LogFactory.getLog(RESTService.class);

    public static final String SERVICE_NAME = "api";

    public ILink getLink(boolean post, Object parameter) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(ServiceConstants.SERVICE, getName());

        if (parameter != null)
            parameters.putAll((Map) parameter);
        return _linkFactory.constructLink(this, post, parameters, true);
    }

    /**
     * Supports service version 1.0+
     */
    public void service(IRequestCycle cycle) throws IOException {
        Double serviceVersion = Double.parseDouble(cycle.getParameter(SERVICE_VERSION_PARAM));
        String widgetID = cycle.getParameter(PARAM_SERVICE_ID);

        if (SERVICE_VERSION_1_0 == serviceVersion) {
            if (widgetID.equalsIgnoreCase(SERVICE_ID_CHART_WIDGET)) {
                _response.getWriter().write(serviceChartWidget(cycle));
            } else if (widgetID.equalsIgnoreCase(SERVICE_ID_ALERT_SUM_WIDGET)) {
                _response.getWriter().write(serviceAlertSummaryWidget(cycle));
            }
        }
    }

    /**
     * Service for the AlertSummary Widget
     * 
     * @param cycle
     * @return
     */
    private String serviceAlertSummaryWidget(IRequestCycle cycle) {
        return "";

    }

    /**
     * Service method for Chart Widget
     * 
     * @param cycle
     * @return the JSON response
     * @throws IOException
     */
    private String serviceChartWidget(IRequestCycle cycle) throws IOException {
        //Get the service parameters
        Integer resourceIdParam = null;
        if (cycle.getParameter(PARAM_RESOURCE_ID) != null) {
            resourceIdParam = Integer.valueOf((String) (cycle.getParameter(PARAM_RESOURCE_ID)));
        }
        String metricTemplIdParam = cycle.getParameter(PARAM_METRIC_TEMPLATE_ID);
        String timeRangeParam = cycle.getParameter(PARAM_TIME_RANGE);
        String configParam = cycle.getParameter(PARAM_CONFIG);
        
        //Get the AuthzSubject
        WebUser user = (WebUser) _request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = ContextUtils.getAuthzBoss(_servletContext);
        AuthzSubject me = null;
        try {
            me = boss.findSubjectById(user.getSessionId(), user.getSubject().getId());
        } catch (Exception e) {
            log.debug(e.getLocalizedMessage());
            return ERROR_GENERIC;
        }

        // Load the current dashboard
        DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
        Integer selectedDashboard = SessionUtils.getIntegerAttribute(_request.getSession(), Constants.SELECTED_DASHBOARD_ID, null);
        ArrayList<DashboardConfig> dashboardList = null;
        try {
            dashboardList = (ArrayList<DashboardConfig>) dashManager.getDashboards(me);
        } catch (PermissionException e) {
            log.debug(e.getLocalizedMessage());
            return ERROR_GENERIC;
        }
        // Get the configuration for the current dashboard
        DashboardConfig dashConfig = DashboardUtils.findDashboard(dashboardList, selectedDashboard);
        ConfigResponse config = dashConfig.getConfig();

        String res;
        
        if (resourceIdParam != null && metricTemplIdParam != null) {
            // Get the timerange for the chart
            String timeRange = config.getValue(".dashContent.charts.range");
            long end = System.currentTimeMillis();
            long start;
            if (timeRange.equalsIgnoreCase("1h")) {
                start = 3600000l; //1h
            } else if (timeRange.equalsIgnoreCase("6h")) {
                start = 21600000l; //6h
            } else if (timeRange.equalsIgnoreCase("1d")) {
                start = 86400000l; //1d
            } else if (timeRange.equalsIgnoreCase("1w")) {
                start = 604800000l; //1w
            } else {
                start = 3600000l; //default to 1h
            }
        
            // Get chart metric data, given the RID and MTIDs
            try {
                ArrayList<Integer> mtids = new ArrayList<Integer>();
                JSONArray mtidArray = new JSONArray(metricTemplIdParam);
                for (int i = 0; i < mtidArray.length(); i++) {
                    mtids.add(Integer.valueOf((String) mtidArray.get(i)));
                }
                Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
                map.put(resourceIdParam, mtids);
                DashboardPortletBossLocal dashBoss = DashboardPortletBossEJBImpl.getOne();
                res = dashBoss.getMeasurementData(me, map, start, end).toString();
            } catch (Exception e) {
                log.debug(e.getLocalizedMessage());
                return ERROR_GENERIC;
            }
        } else if (configParam != null) {
            if (timeRangeParam != null) {
                config.setValue(".dashContent.charts.tr", timeRangeParam);
                //update the crispo
                try {
                    ConfigurationProxy.getInstance().setDashboardPreferences(_request.getSession(), user, boss, config);
                } catch (Exception e) {
                    log.debug(e.getLocalizedMessage());
                    res = ERROR_GENERIC;
                }
                res = EMPTY_RESPONSE;
            } else {
                try {
                    res = new JSONObject().put("tr", config.getValue(".dashContent.charts.tr")).toString();
                } catch (JSONException e) {
                    log.debug(e.getLocalizedMessage());
                    res = ERROR_GENERIC;
                }
            }
        } else {
            // Get the list of saved charts for this user
            res = config.getValue(".dashContent.charts");
            if (res == null)
                res = EMPTY_RESPONSE; // no saved charts
        }
        return res;
    }

    public String getName() {
        return SERVICE_NAME;
    }
}
