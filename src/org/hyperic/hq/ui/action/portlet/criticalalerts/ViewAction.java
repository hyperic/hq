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

package org.hyperic.hq.ui.action.portlet.criticalalerts;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.uibeans.DashboardAlertBean;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.json.JSONObject;

/**
 * This action class is used by the Critical Alerts portlet.  It's main
 * use is to generate the JSON objects required for display into the UI.
 */
public class ViewAction extends TilesAction {

    private Log _log = LogFactory.getLog("CRITICAL ALERTS");

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        EventsBoss eventBoss = ContextUtils.getEventsBoss(ctx);
        WebUser user = (WebUser) request.getSession().getAttribute(
            Constants.WEBUSER_SES_ATTR);
        String key = ".dashContent.criticalalerts.resources";

        List entityIds = DashboardUtils.preferencesAsEntityIds(key, user);

        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];

        int h = 0;
        for (Iterator i = entityIds.iterator(); i.hasNext(); h++) {
            arrayIds[h] = (AppdefEntityID) i.next();
        }

        int count = Integer.parseInt(user.getPreference(PropertiesForm.
            ALERT_NUMBER));
        int priority = Integer.parseInt(user.getPreference(PropertiesForm.
            PRIORITY));
        long timeRange = Long.parseLong(user.getPreference(PropertiesForm.PAST));
        boolean all =
            "all".equals(user.getPreference(PropertiesForm.SELECTED_OR_ALL));

        int sessionID = user.getSessionId().intValue();
        PageControl pc = new PageControl();
        pc.setPagesize(10);

        if (all) {
            arrayIds = null;
        }

        PageList criticalAlerts =
            eventBoss.findAlerts(sessionID, count, priority, timeRange, arrayIds,
                                 pc);

        JSONObject alerts = new JSONObject();
        List a = new ArrayList();
        for (Iterator i = criticalAlerts.iterator(); i.hasNext(); ) {
            DashboardAlertBean bean = (DashboardAlertBean)i.next();
            JSONObject alert = new JSONObject();
            alert.put("alertId", bean.getAlertId());
            alert.put("appdefKey",
                      bean.getResource().getEntityId().getAppdefKey());
            alert.put("resourceName", bean.getResource().getName());
            alert.put("alertDefName", bean.getAlertDefName());
            alert.put("cTime", bean.getCtime());

            a.add(alert);
        }

        alerts.put("criticalAlerts", a);

        _log.info(alerts.toString());

        //response.getWriter().write(alerts.toString());
        
        return null;
    }
}
