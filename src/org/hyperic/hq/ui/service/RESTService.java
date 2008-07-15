package org.hyperic.hq.ui.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.ServiceConstants;
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
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * The UI Dashboard Widgets Service
 * 
 */
public class RESTService extends BaseService {

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
            }else if(widgetID.equalsIgnoreCase(SERVICE_ID_ALERT_SUM_WIDGET)){
                _response.getWriter().write(serviceAlertSummaryWidget(cycle));
            }
        }
    }

    /**
     * Service for the AlertSummary Widget
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
        Integer rid = Integer.valueOf((String)(cycle.getParameter(PARAM_RESOURCE_ID)));
        String mtid = cycle.getParameter(PARAM_METRIC_TEMPLATE_ID);
        
        WebUser user = (WebUser) _request.getSession().getAttribute(
                Constants.WEBUSER_SES_ATTR);
        AuthzBoss boss = ContextUtils.getAuthzBoss(_servletContext);
        AuthzSubject me = null;
        
        try {
            me = boss.findSubjectById(user.getSessionId(), user
                    .getSubject().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String res = "";

        if (rid != null && mtid != null) {
            // Get chart metric data for a resource
            try {
                ArrayList mtids = new ArrayList();
                JSONArray mtidArray = new JSONArray(mtid);
                for(int i =0; i< mtidArray.length();i++){
                    mtids.add(Integer.valueOf((String)mtidArray.get(i)));
                }
                Map map = new HashMap();
                map.put(rid, mtids);
                DashboardPortletBossLocal dashBoss = DashboardPortletBossEJBImpl
                        .getOne();
                long start = new Date().getTime();
                long end = new Date().getTime() -1000;
                    res = dashBoss.getMeasurementData(me, map, start, end)
                            .toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Get the list of saved charts for this user
            DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
            HttpSession session = _request.getSession();
            Integer selectedDashboard = SessionUtils.getIntegerAttribute(
                    session, Constants.SELECTED_DASHBOARD_ID, null);
            ArrayList dashboardList = null;
            try {
                dashboardList = (ArrayList) dashManager.getDashboards(me);
            } catch (PermissionException e) {
                e.printStackTrace();
            }
            // get the configuration for the current dashboard
            DashboardConfig dashConfig = DashboardUtils.findDashboard(dashboardList, selectedDashboard);
            ConfigResponse c = dashConfig.getConfig();
            // get the configuration for the current dashboard and lookup the widget props
            res = c.getValue(".dashContent.charts");
            if(res == null)
                res = "";
        }
        return res;
    }

    public String getName() {
        return SERVICE_NAME;
    }
}
