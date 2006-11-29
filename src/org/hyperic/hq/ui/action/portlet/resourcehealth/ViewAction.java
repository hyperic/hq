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

package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.measurement.MeasurementConstants;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.json.JSONObject;

/**
 * This action class is used by the Favorite Resources portlet.  It's main
 * use is to generate the JSON objects required for display into the UI.
 */
public class ViewAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
       throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);
        WebUser user = (WebUser)
            request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        String key = Constants.USERPREF_KEY_FAVORITE_RESOURCES;

        List list;
        try{
            list = getResources(key, boss, user);
        } catch(Exception e) {
            DashboardUtils.verifyResources(key, ctx, user);
            list = getResources(key, boss, user);
        }

        // XXX: We should get rid of this configuration, who uses it, and why?
        Boolean availability =
            Boolean.valueOf(user.
                getPreference(".dashContent.resourcehealth.availability"));
        Boolean throughput =
            Boolean.valueOf(user.
                getPreference(".dashContent.resourcehealth.throughput"));
        Boolean performance =
            Boolean.valueOf(user.
                getPreference(".dashContent.resourcehealth.performance"));
        Boolean utilization =
            Boolean.valueOf(user.
                getPreference(".dashContent.resourcehealth.utilization"));
        //context.putAttribute("availability", availability);
        //context.putAttribute("throughput", throughput);
        //context.putAttribute("performance", performance);
        //context.putAttribute("utilization", utilization);

        // Due to the complexity of the UIBeans, we need to construct the
        // JSON objects by hand.
        JSONObject favorites = new JSONObject();

        List resources = new ArrayList();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            JSONObject res = new JSONObject();
            ResourceDisplaySummary bean = (ResourceDisplaySummary)i.next();
            res.put("resourceName", bean.getResourceName());
            res.put("resourceTypeName", bean.getResourceTypeName());
            res.put("resourceTypeId", bean.getResourceTypeId());
            res.put("resourceId", bean.getResourceId());
            res.put("performance", bean.getPerformance());
            res.put("throughput", bean.getThroughput());
            res.put("throughputUnits", bean.getThroughputUnits());
            res.put("availability", getAvailString(bean.getAvailability()));
            res.put("monitorable", bean.getMonitorable());
            res.put("alerts", bean.getAlerts());

            resources.add(res);
        }
        
        favorites.put("favorites", resources);

        response.getWriter().write(favorites.toString());

        return null;
    }

    /**
     * Get the availability string for the given metric value.  The returned
     * string should match the availabilty icon filenames.
     * @param availability The availability metric value.
     * @return The mapped string for the given availablity metric.  If the
     * given metric is not valid, unknown is returned.
     */
    private String getAvailString(Double availability) {
        double avail = availability.doubleValue();

        if (avail == MeasurementConstants.AVAIL_UP) {
                return "green";
        } else if (avail == MeasurementConstants.AVAIL_DOWN) {
                return "red";
        } else if (avail == MeasurementConstants.AVAIL_PAUSED) {
                return "orange";
        } else if (avail == MeasurementConstants.AVAIL_WARN) {
                return "yellow";
        } else {
                return "unknown";
        }
    }

    private List getResources(String key, MeasurementBoss boss, WebUser user)
        throws Exception
    {
        List entityIds =  DashboardUtils.preferencesAsEntityIds(key, user);                                    

        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = (AppdefEntityID[]) entityIds.toArray(arrayIds);
        
        return boss.findResourcesCurrentHealth(user.getSessionId().intValue(),
                                               arrayIds);
    }
}
