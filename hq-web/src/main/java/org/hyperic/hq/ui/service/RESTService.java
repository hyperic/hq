package org.hyperic.hq.ui.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.CloningBoss;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.CheckPermissionsUtil;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil;
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

    private static final Pattern MTID_PATTERN =
        Pattern.compile(".*&m=(\\d+).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern CTYPE_PATTERN =
        Pattern.compile(".*ctype=(\\d+)%3A(\\d+).*", Pattern.CASE_INSENSITIVE);
    
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
        Double serviceVersion =
            Double.parseDouble(cycle.getParameter(PARAM_SERVICE_VERSION));
        String servicePointId = cycle.getParameter(PARAM_SERVICE_ID);

        if (SERVICE_VERSION_1_0 == serviceVersion) {
            if (SERVICE_ID_CHART_WIDGET.equalsIgnoreCase(servicePointId)) {
                _response.getWriter().write(serviceChartWidget(cycle));
            } else if (SERVICE_ID_ALERT_SUM_WIDGET.equalsIgnoreCase(servicePointId)) {
                _response.getWriter().write(serviceAlertSummaryWidget(cycle));
            } else if (SERVICE_ID_MAINTENANCE_WINDOW_WIDGET.equalsIgnoreCase(servicePointId)) {
            	_response.getWriter().write(serviceMaintenanceWindowWidget(cycle));
            } else if (SERVICE_ID_CLONE_PLATFORM_WIDGET.equalsIgnoreCase(servicePointId)) {
            	_response.getWriter().write(serviceClonePlatformWidget(cycle));
            } else if (SERVICE_ID_GROUP_MANAGER_WIDGET.equalsIgnoreCase(servicePointId)) {
                _response.getWriter().write(serviceGroupManagerWidget(cycle));                
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
        AuthzBoss boss  = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        ConfigResponse config = loadDashboardConfig(me);
        if (config == null)
            return ERROR_GENERIC;

        // Get the list of groups
        String groups =
            config.getValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_GROUPS);

        List<String> groupsList =
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
                    if (groupsList.isEmpty()) {
                        config.unsetValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_GROUPS);
                    } else {
                        config.setValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_GROUPS,
                                        ids);
                    }
                    update = true;
                }

                if (update) {
                    //update the crispo
                    Bootstrap.getBean(ConfigurationProxy.class)
                        .setDashboardPreferences(_request.getSession(), user,
                                                 config);
                }
                
                int sessionId = RequestUtils.getSessionId(_request).intValue();
                
                PageList resources = Bootstrap.getBean(AppdefBoss.class)
                    .search(sessionId, AppdefEntityConstants.APPDEF_TYPE_GROUP,
                            regexFilterParam, null, null,
                            new int[] {
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS,
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC},
                            false, false, false, PageControl.PAGE_ALL);
                
                JSONArray arr = new JSONArray();
                JSONObject avails = new JSONObject();
                for (Iterator<AppdefResourceValue> it = resources.iterator();
                     it.hasNext(); ) {
                    AppdefResourceValue resource = it.next();
                    final String resourceId = resource.getId().toString();
                    avails.put(resourceId, resource.getName());
                    if (groupsList.contains(resourceId))
                        arr.put(resourceId);
                }
                
                res = new JSONObject()
                    .put(PARAM_RESOURCE_ID, arr)
                    .put(PARAM_TIME_RANGE,
                         config.getValue(Constants.USER_DASHBOARD_ALERT_SUMMARY_RANGE))
                    .put("data", avails)
                    .toString();
            } else if (resourceIdParam != null) {
                ResourceGroupManager rgman = 
                    Bootstrap.getBean(ResourceGroupManager.class);
                JSONArray arr = new JSONArray();
                for (String group : groupsList)
                {
                    Integer gid = Integer.valueOf(group); 
                    ResourceGroup rg = rgman.findResourceGroupById(gid);
                    arr.put(new JSONObject().put("id", gid)
                            .put("name", rg.getName()));
                }
                res = arr.toString();
            } else {
                // get alert data
                List<Integer> gids = new ArrayList<Integer>(groupsList.size());
                for (String group : groupsList)
                {
                    gids.add(Integer.valueOf(group)); 
                }
                
                PageInfo pi = PageInfo.create(PageControl.PAGE_ALL, null);
                
                DashboardPortletBoss dashBoss =
                    Bootstrap.getBean(DashboardPortletBoss.class);

                res = dashBoss._getAlertCounts(me, gids, pi).toString();
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
        String deleteParam        = cycle.getParameter(PARAM_DELETE);
        String rpTemp             = cycle.getParameter(PARAM_RESOURCE_ID);
        String ctypeParam         = cycle.getParameter(PARAM_CTYPE);
        
        Integer resourceIdParam = null;
        if (rpTemp != null) {
            resourceIdParam = Integer.valueOf(rpTemp);
        }

        // Get the AuthzSubject
        WebUser user = (WebUser) _request.getSession()
                .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        ConfigResponse config = loadDashboardConfig(me);
        if (config == null)
            return ERROR_GENERIC;

        String res;

        if (resourceIdParam != null && metricTemplIdParam != null && deleteParam == null) {
            // Get the timerange for the chart
            String timeRange = config.getValue(Constants.USER_DASHBOARD_CHART_RANGE);
            long end = System.currentTimeMillis();
            long start;
            if (timeRange.equalsIgnoreCase("1h")) {
                start = end - 3600000l; //1h
            } else if (timeRange.equalsIgnoreCase("6h")) {
                start = end - 21600000l; //6h
            } else if (timeRange.equalsIgnoreCase("1d")) {
                start = end - 86400000l; //1d
            } else if (timeRange.equalsIgnoreCase("1w")) {
                start = end - 604800000l; //1w
            } else {
                start = end - 86400000l; //default to 1d
            }

            // Get chart metric data, given the RID and MTIDs
            try {
                JSONArray mtidArray = new JSONArray(metricTemplIdParam);
                
                AppdefEntityTypeID ctype = null;
                if ((ctypeParam != null) && (ctypeParam.trim().length()>0)) {
                    try {
                        ctype = new AppdefEntityTypeID(ctypeParam);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                // Only do one metric
                res = Bootstrap.getBean(DashboardPortletBoss.class)
                            ._getMeasurementData(
                                    me, 
                                    new Integer(resourceIdParam),
                                    (Integer) mtidArray.get(0),
                                    ctype,
                                    start, 
                                    end).toString();
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
                    Bootstrap.getBean(ConfigurationProxy.class)
                        .setDashboardPreferences(_request.getSession(), user,
                                                  config);
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
        } else if (deleteParam != null && metricTemplIdParam != null && resourceIdParam != null) {
            res = config.getValue(Constants.USER_DASHBOARD_CHARTS);
            if (res == null)
                res = ERROR_GENERIC; // chart not found
            else {
                List<String> chartList = null;
                try {
                    chartList = StringUtil.explode(res, Constants.DASHBOARD_DELIMITER);
                    if (chartList != null) {
                        ResourceManager resMan = Bootstrap.getBean(ResourceManager.class);
                        for (String chartCfg : chartList) {
                            List<String> chart =
                                StringUtil.explode(chartCfg, ",");
                            //Lookup the rid for the resource in each chart
                            Integer resId = 0;
                            try {
                                AppdefEntityID aeid = SaveChartToDashboardUtil
                                                        .getAppdefEntityIDFromChartUrl(chart.get(1));
                                if (aeid != null) {
                                    Resource resource = resMan.findResource(aeid);
                                    resId = resource.getId();
                                }
                            } catch (Exception e) {
                                // Resource removed
                                log.debug(e.getLocalizedMessage(), e);
                                continue;
                            }
                            if (resId.intValue() == resourceIdParam) {
                                //If the resource exists in the chart then check all the mtids
                                JSONArray mtidArray = new JSONArray(metricTemplIdParam);
                                int mtidCount = 0;
                                for (int i = 0; i < mtidArray.length(); i++) {
                                    if (chartCfg.indexOf(mtidArray.getString(i)) != -1) {
                                        mtidCount++;
                                    }
                                }
                                if (mtidCount == mtidArray.length()) {
                                    //If all the mtids are in the chart then remove it
                                    chartList.remove(chartCfg);
                                    // put the rest of the charts back in the config and persist it
                                    String list = StringUtil.implode(chartList, 
                                            Constants.DASHBOARD_DELIMITER);
                                    list = Constants.DASHBOARD_DELIMITER + list;
                                    config.setValue(Constants.USER_DASHBOARD_CHARTS, list);
                                    Bootstrap.getBean(ConfigurationProxy.class).setDashboardPreferences(
                                            _request.getSession(), user, config);
                                    break;
                                }
                            }
                        }
                    }
                    res = EMPTY_RESPONSE;
                } catch (Exception e) {
                    res = ERROR_GENERIC;
                }
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
    
                        ResourceManager resMan =
                            Bootstrap.getBean(ResourceManager.class);
                        

                        Matcher matcher;
                        for (String chartCfg : chartList)
                        {
                            List<String> chart =
                                StringUtil.explode(chartCfg, ",");
                         

            		        // Determine what entityIds can be viewed by this user
            		        // This code probably should be in the boss somewhere but
            		        // for now doing it here...
            				if (CheckPermissionsUtil.canUserViewChart(RequestUtils.getSessionId(_request).intValue(),
            						                                  (String) chart.get(1),
            						                                  Bootstrap.getBean(AppdefBoss.class))) 
            				{
	
	                            
	                            matcher = MTID_PATTERN.matcher(chart.get(1));
	                            JSONArray mtid = new JSONArray();
	                            if (matcher.matches()) {
	                                mtid.put(Integer.valueOf(matcher.group(1)));
	                            }                            
	                            
	                            // Extract the resource ID & name
	                            Integer resId = 0;
	                            String resName = chart.get(0);
	                            	                            
	                            try {
	                                AppdefEntityID aeid = SaveChartToDashboardUtil
	                                                        .getAppdefEntityIDFromChartUrl(chart.get(1));	                            
	                                if (aeid != null) {
	                                    Resource resource =	resMan.findResource(aeid);
	                                    
	                                    resId = resource.getId();
	                                    resName = resource.getName();
	                                }
	                            } catch (Exception e) {
	                                // Resource removed
	                                log.debug(e.getLocalizedMessage(), e);
	                                continue;
	                            }
	                            
	                            // Extract the ctype if exists
	                            String ctype = "";
	                            matcher = CTYPE_PATTERN.matcher(chart.get(1));
	                            if (matcher.matches()) {
	                                ctype = matcher.group(1) + ":" + matcher.group(2);
	                            }
	                            
	                            arr.put(new JSONObject().put("name", resName)
	                                                    .put("rid", resId)
	                                                    .put("mtid", mtid)
	                                                    .put("ctype", ctype)
	                                                    .put("url", chart.get(1)));
            				}
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

    /**
     * Service for the Group Manager Widget 
     * 
     * @param cycle the service parameters
     * @return the service JSON response
     */
    private String serviceGroupManagerWidget(IRequestCycle cycle) {
        // Get the AuthzSubject
        WebUser user = (WebUser) _request.getSession()
                .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;
        
        JSONObject jRes = new JSONObject();

        try {
            String mode = cycle.getParameter(Constants.MODE_PARAM);
            JSONArray aeidJArray = new JSONArray(cycle.getParameter(Constants.ENTITY_ID_PARAM));
            
            if (Constants.MODE_ADD_GROUPS.equals(mode)) {
                JSONArray groupJArray = new JSONArray(cycle.getParameter("groupId"));
                Integer[] groupIds = new Integer[groupJArray.length()];
                
                for (int j=0; j<groupJArray.length(); j++) {
                    groupIds[j] = new Integer(groupJArray.getString(j));
                }
                
                for (int i=0; i<aeidJArray.length(); i++) {
                    AppdefEntityID aeid = new AppdefEntityID(aeidJArray.getString(i));
                    if (!aeid.isGroup()) {
                                        
                    
                        Bootstrap.getBean(AppdefBoss.class)
                                 .batchGroupAdd(
                                         user.getSessionId(),
                                         aeid,
                                         groupIds);
                    }
                }
            } else {
                AppdefEntityID[] aeids = new AppdefEntityID[aeidJArray.length()];
                
                for (int k=0; k<aeidJArray.length(); k++) {
                    aeids[k] = new AppdefEntityID(aeidJArray.getString(k));
                }

                PageList availableGroups = 
                    Bootstrap.getBean(AppdefBoss.class).findAllGroupsMemberExclusive(
                                                    user.getSessionId(),
                                                    PageControl.PAGE_ALL,
                                                    aeids);
                
                JSONArray jarr = new JSONArray();
                AppdefGroupValue group = null;
                for (Iterator<AppdefGroupValue> it = availableGroups.iterator(); it.hasNext();) {
                    group = it.next();
                    jarr.put(new JSONObject()
                                .put("id", group.getId())
                                .put("name", group.getName())
                                .put("description", group.getDescription()));
                }
                jRes.put("groups", jarr);
            }
        } catch (Throwable e) {
            log.debug(e.getLocalizedMessage());
            
            try {
                jRes.put("error", true)
                    .put("error_message", e.getLocalizedMessage());
            } catch (Exception e2) {}
        }
        
        return jRes.toString();      
    }
    
    /**
     * Service for the Maintenance Window Widget
     * 
     * @param cycle the service parameters
     * @return the service JSON response
     */
    private String serviceMaintenanceWindowWidget(IRequestCycle cycle) {
    	String groupIdParam  = cycle.getParameter(MaintenanceEvent.GROUP_ID);
        String startTimeParam = cycle.getParameter(MaintenanceEvent.START_TIME);
        String endTimeParam = cycle.getParameter(MaintenanceEvent.END_TIME);
        String scheduleParam = cycle.getParameter(PARAM_SCHEDULE);

        // Get the AuthzSubject
        WebUser user = (WebUser) _request.getSession()
                .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        JSONObject jRes = new JSONObject();
        
        try {
            EventsBoss eb = Bootstrap.getBean(EventsBoss.class);
            MaintenanceEvent event = null;
            Integer groupId = Integer.valueOf(groupIdParam);

            if ((scheduleParam == null) || (scheduleParam.trim().length() == 0))
            {
            	// Get Scheduled Maintenance Event              
                event = eb.getMaintenanceEvent(user.getSessionId(), groupId);
            	if (event == null) {
            	    event = new MaintenanceEvent(groupId);
            	}
            } else {
            	event = new MaintenanceEvent(groupId);
            	
            	if (Boolean.valueOf(scheduleParam).booleanValue()) {
            		// Reschedule Maintenance Event
                	event.setStartTime(Long.parseLong(startTimeParam));
                	event.setEndTime(Long.parseLong(endTimeParam));
            		event = eb.scheduleMaintenanceEvent(user.getSessionId(),
            		                                    event);
            	} else {
            		// Unschedule Maintenance Event
            		eb.unscheduleMaintenanceEvent(user.getSessionId(), event);
            	}		
            }
            
            if (event != null) {
            	jRes = event.toJSON();
            	
            	boolean canSchedule = PermissionManagerFactory.getInstance()
            									.getMaintenanceEventManager()
            									.canSchedule(me, event);
            	jRes.put("permission", canSchedule);
            }
        	jRes.put("serverTime", new Date().getTime());
     
        } catch (Exception e) {
            log.debug(e.getLocalizedMessage());
            
            try {
            	jRes.put("error", true)
            		.put("error_message", e.getLocalizedMessage());
            } catch (Exception e2) {}
        }
        
        return (jRes.length() > 0) ? jRes.toString() : ERROR_GENERIC;
    }

    /**
     * Service for the Clone Platform Widget
     * 
     * @param cycle the service parameters
     * @return the service JSON response
     */
    private String serviceClonePlatformWidget(IRequestCycle cycle) {
        String query = cycle.getParameter(PARAM_SEARCH_QUERY);
        String platformId = cycle.getParameter(PARAM_PLATFORM_ID);
        String cloneTargetId = cycle.getParameter(PARAM_CLONE_TARGET_ID);
        String performClone = cycle.getParameter(PARAM_CLONE);
       
    	String res = EMPTY_RESPONSE; // default to an empty response

        // Get the AuthzSubject
        WebUser user = (WebUser) _request.getSession()
                .getAttribute(Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = getAuthzSubject(user, boss);
        if (me == null)
            return ERROR_GENERIC;

        try {
        	CloningBoss cloningBoss = Bootstrap.getBean(CloningBoss.class);

        	if ((performClone != null) 
        			&& (Boolean.valueOf(performClone).booleanValue()))
        	{
        		// Clone platform
        		List cloneTargetIdList = new ArrayList();
                JSONArray arr = new JSONArray(cloneTargetId);
                
                for (int i = 0; i < arr.length(); i++) {
        			cloneTargetIdList.add(Integer.valueOf(arr.getString(i)));
                }
        		
        		cloningBoss.clonePlatform(
        							me,
        							Integer.valueOf(platformId),
        							cloneTargetIdList);
        		
        	} else {
        		PlatformManager platformMgr = 
        		    Bootstrap.getBean(PlatformManager.class);		
        		Platform platform = 
        				platformMgr.findPlatformById(Integer.valueOf(platformId));
        		PlatformType platformType = 
        				platform.getPlatformType();
        		List platforms;
        		
        		if ((query == null) || (query.trim().length() == 0)) {
            		// Get all platforms
                    platforms = platformMgr.getPlatformsByType(
												me, 
												platformType.getName());
        		} else {
        			// Search for platforms
        			platforms = cloningBoss.findPlatformsByTypeAndName(
	        									me, 
	        									platformType.getId(), 
	        									StringUtil.escapeForRegex(query.trim(), true));
        		}
	        	
	        	JSONObject jPlatform = new JSONObject();
	
	            for (Iterator<Platform> it = platforms.iterator(); it.hasNext();) {
	                platform = it.next();
	                jPlatform.put(
	                		platform.getId().toString(),
	                		platform.getName());
	            }
	            
	        	res = jPlatform.toString();
        	}
                    	
        } catch (Exception e) {
            log.debug(e.getLocalizedMessage());
            
            try {
            	res = new JSONObject()
            				.put("error", true)
            				.put("error_message", e.getLocalizedMessage())
            				.toString();
            } catch (Exception e2) {
            	res = ERROR_GENERIC;
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
        DashboardManager dashManager = Bootstrap.getBean(DashboardManager.class);
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
