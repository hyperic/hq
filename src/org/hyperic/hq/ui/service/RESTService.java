package org.hyperic.hq.ui.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.ServiceConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl;
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
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
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

    public String getName() {
        return SERVICE_NAME;
    }
    
    /**
     * Generates the service urls
     */
    public ILink getLink(boolean post, Object parameter) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(ServiceConstants.SERVICE, getName());

        if (parameter != null)
            parameters.putAll((Map) parameter);
        return _linkFactory.constructLink(this, post, parameters, true);
    }

    /**
     * The Service method. Supports services of version 1.0+
     */
    public void service(IRequestCycle cycle) throws IOException {
        Double serviceVersion = Double.parseDouble(cycle.getParameter(PARAM_SERVICE_VERSION));
        String servicePointId = cycle.getParameter(PARAM_SERVICE_ID);

        if (SERVICE_VERSION_1_0 == serviceVersion) {
            if (servicePointId.equalsIgnoreCase(SERVICE_ID_CHART_WIDGET)) {
                _response.getWriter().write(serviceChartWidget(cycle));
            } else if (servicePointId.equalsIgnoreCase(SERVICE_ID_ALERT_SUM_WIDGET)) {
                _response.getWriter().write(serviceAlertSummaryWidget(cycle));
            }
        }
    }

    /**
     * Service for the AlertSummary Widget
     * 
     * @param cycle the service parameters
     * @return the service JSON response
     */
    private String serviceAlertSummaryWidget(IRequestCycle cycle) {
        String configParam      = cycle.getParameter(PARAM_CONFIG);
        String pageNumberParam  = cycle.getParameter(PARAM_PAGE_NUM);
        String regexFilterParam = cycle.getParameter(PARAM_REGEX_FILTER);
        String resourceIdParam  = cycle.getParameter(PARAM_RESOURCE_ID);
        String timeRangeParam   = cycle.getParameter(PARAM_TIME_RANGE);

        String res = EMPTY_RESPONSE; // default to an empty response

        //Get the AuthzSubject
        WebUser user    = (WebUser) _request.getSession()
            .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss  = ContextUtils.getAuthzBoss(_servletContext);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        ConfigResponse config = loadDashboardConfig(me);
        if (config == null)
            return ERROR_GENERIC;

        // Get the list of groups
        String groups =
            config.getValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_GROUPS);

        List groupsList =
            StringUtil.explode(groups, Constants.DASHBOARD_DELIMITER);

        try {
            if (configParam != null) {
                // config
                boolean update = false;
                if (timeRangeParam != null) {
                    config.setValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_RANGE,
                                    timeRangeParam);
                    update = true;
                }

                if (resourceIdParam != null) {
                    // set the resource configuration property
                    String ids = "";
                    try {
                        JSONArray arr = new JSONArray(resourceIdParam);
                        groupsList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            ids += Constants.DASHBOARD_DELIMITER +
                                   arr.getString(i);
                            groupsList.add(arr.getString(i));
                        }
                    } catch (JSONException e) {
                        log.debug(e.getLocalizedMessage());
                    }
                    config.setValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_GROUPS,
                                    ids);
                    update = true;
                }

                if (update) {
                    //update the crispo
                    ConfigurationProxy.getInstance()
                        .setDashboardPreferences(_request.getSession(), user,
                                                 boss, config);
                }

                JSONArray arr = new JSONArray();
                for (Iterator<String> it = groupsList.iterator(); it.hasNext();)
                {
                    arr.put(Integer.valueOf(it.next()));
                }
                
                int sessionId = RequestUtils.getSessionId(_request).intValue();
                
                PageList resources = AppdefBossEJBImpl.getOne()
                    .search(sessionId, AppdefEntityConstants.APPDEF_TYPE_GROUP,
                            regexFilterParam, null, null,
                            new int[] {
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS,
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC},
                            false, false, false, PageControl.PAGE_ALL);
                
                JSONArray avails = new JSONArray();
                for (Iterator<AppdefResourceValue> it = resources.iterator();
                     it.hasNext(); ) {
                    AppdefResourceValue resource = it.next();
                    avails.put(new JSONObject().put("id", resource.getId())
                                               .put("name",resource.getName()));
                }
                
                res = new JSONObject()
                    .put(PARAM_RESOURCE_ID, arr)
                    .put(PARAM_TIME_RANGE,
                         config.getValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_RANGE))
                    .put("avail", avails)
                    .toString();
            } else if (resourceIdParam != null) {
                ResourceGroupManagerLocal rgman = 
                    ResourceGroupManagerEJBImpl.getOne();
                JSONArray arr = new JSONArray();
                for (Iterator<String> it = groupsList.iterator(); it.hasNext();)
                {
                    Integer gid = Integer.valueOf(it.next()); 
                    ResourceGroup rg = rgman.findResourceGroupById(gid);
                    arr.put(new JSONObject().put("id", gid)
                            .put("name", rg.getName()));
                }
                res = arr.toString();
            } else {
                // get alert data
            }
        } catch (Exception e) {
            log.debug(e.getLocalizedMessage());
            res = ERROR_GENERIC;
        }

        return res;
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
        String metricTemplIdParam = cycle.getParameter(PARAM_METRIC_TEMPLATE_ID);
        String timeRangeParam     = cycle.getParameter(PARAM_TIME_RANGE);
        String rotationParam      = cycle.getParameter(PARAM_ROTATION);
        String intervalParam      = cycle.getParameter(PARAM_INTERVAL);
        String configParam        = cycle.getParameter(PARAM_CONFIG);
        String rpTemp             = cycle.getParameter(PARAM_RESOURCE_ID);
        
        Integer resourceIdParam = null;
        if (rpTemp != null) {
            resourceIdParam = Integer.valueOf(rpTemp);
        }

        //Get the AuthzSubject
        WebUser user    = (WebUser) _request.getSession()
            .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss  = ContextUtils.getAuthzBoss(_servletContext);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        ConfigResponse config = loadDashboardConfig(me);
        if (config == null)
            return ERROR_GENERIC;

        String res;

        if (resourceIdParam != null && metricTemplIdParam != null) {
            // Get the timerange for the chart
            String timeRange = config.getValue(Constants.USER_DASHBOARD_CHART_RANGE);
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
                start = 86400000l; //default to 1d
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
            boolean update = false;
            if (timeRangeParam != null) {
                config.setValue(Constants.USER_DASHBOARD_CHART_RANGE,
                                timeRangeParam);
                update = true;
            }
            
            if (rotationParam != null) {
                config.setValue(Constants.USER_DASHBOARD_CHART_ROTATION,
                                rotationParam);
                update = true;
            }
            
            if (intervalParam != null) {
                config.setValue(Constants.USER_DASHBOARD_CHART_INTERVAL,
                                intervalParam);
                update = true;
            }
            
            if (update) {
                //update the crispo
                try {
                    ConfigurationProxy.getInstance()
                        .setDashboardPreferences(_request.getSession(), user,
                                                 boss, config);
                } catch (Exception e) {
                    log.debug(e.getLocalizedMessage());
                    res = ERROR_GENERIC;
                }
            }

            try {
                res = new JSONObject()
                    .put(PARAM_TIME_RANGE,
                         config.getValue(Constants.USER_DASHBOARD_CHART_RANGE))
                    .put(PARAM_ROTATION,
                         config.getValue(Constants.USER_DASHBOARD_CHART_ROTATION))
                    .put(PARAM_INTERVAL,
                         config.getValue(Constants.USER_DASHBOARD_CHART_INTERVAL))
                    .toString();
            } catch (JSONException e) {
                log.debug(e.getLocalizedMessage());
                res = ERROR_GENERIC;
            }
        } else {
            // Get the list of saved charts for this dashboard
            res = config.getValue(Constants.USER_DASHBOARD_CHARTS);
            if (res == null)
                res = EMPTY_RESPONSE; // no saved charts
            else {
                List<String> chartList = null;
                try {
                    chartList =
                        StringUtil.explode(res, Constants.DASHBOARD_DELIMITER);
                    if (chartList != null) {
                        JSONArray arr = new JSONArray();
    
                        ResourceManagerLocal resMan =
                            ResourceManagerEJBImpl.getOne();
                        
                        for (Iterator<String> i = chartList.iterator();
                             i.hasNext();)
                        {
                            List<String> chart =
                                StringUtil.explode(i.next(), ",");
                            
                            // Extract the mtid
                            String mtidRegex = ".*&m=(\\d+).*";
                            Pattern pattern =
                                Pattern.compile(mtidRegex,
                                                Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(chart.get(1));

                            JSONArray mtid = new JSONArray();
                            if (matcher.matches()) {
                                mtid.put(Integer.valueOf(matcher.group(1)));
                            }                            
                            
                            // Extract the resource ID
                            Integer resId = 0;
                            String aeidRegex = ".*&eid=(\\d+:\\d+).*";
                            pattern = Pattern.compile(mtidRegex,
                                                      Pattern.CASE_INSENSITIVE);
                            matcher = pattern.matcher(chart.get(1));
                            if (matcher.matches()) {
                                AppdefEntityID aeid =
                                    new AppdefEntityID(matcher.group(1));
                                Resource resource = resMan.findResource(aeid);
                                resId = resource.getId();
                            }
                                
                            arr.put(new JSONObject().put("name", chart.get(0))
                                                    .put("rid", resId)
                                                    .put("mtid", mtid));
                        }
                        
                        res = arr.toString();
                    }
                } catch (Exception e) {
                    res = EMPTY_RESPONSE;
                }
            }
        }
        return res;
    }

    private AuthzSubject getAuthzSubject(WebUser user, AuthzBoss boss) {
        try {
            return boss.findSubjectById(user.getSessionId(),
                                        user.getSubject().getId());
        } catch (Exception e) {
            log.debug(e.getLocalizedMessage());
            return null;
        }
    }

    private ConfigResponse loadDashboardConfig(AuthzSubject me) {
        // Load the current dashboard
        DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
        Integer selectedDashboard =
            SessionUtils.getIntegerAttribute(_request.getSession(),
                                             Constants.SELECTED_DASHBOARD_ID,
                                             null);
        ArrayList<DashboardConfig> dashboardList = null;
        try {
            dashboardList =
                (ArrayList<DashboardConfig>) dashManager.getDashboards(me);
        } catch (PermissionException e) {
            log.debug(e.getLocalizedMessage());
            return null;
        }
        
        // Get the configuration for the current dashboard
        DashboardConfig dashConfig =
            DashboardUtils.findDashboard(dashboardList, selectedDashboard);
        ConfigResponse config = dashConfig.getConfig();
        return config;
    }
}
