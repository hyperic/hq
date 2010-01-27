/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

public class PrepareAction
    extends TilesAction {

    private AppdefBoss appdefBoss;
    private AuthzBoss authzBoss;
    private DashboardManager dashboardManager;

    @Autowired
    public PrepareAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, DashboardManager dashboardManager) {
        super();
        this.appdefBoss = appdefBoss;
        this.authzBoss = authzBoss;
        this.dashboardManager = dashboardManager;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        PropertiesForm pForm = (PropertiesForm) form;

        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        Integer sessionId = user.getSessionId();

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        String token = pForm.getToken();

        // For multi-portlet configurations
        String resKey = ViewAction.RESOURCES_KEY;
        String countKey = PropertiesForm.ALERT_NUMBER;
        String priorityKey = PropertiesForm.PRIORITY;
        String timeKey = PropertiesForm.PAST;
        String selOrAllKey = PropertiesForm.SELECTED_OR_ALL;
        String titleKey = PropertiesForm.TITLE;

        if (token != null) {
            resKey += token;
            countKey += token;
            priorityKey += token;
            timeKey += token;
            selOrAllKey += token;
            titleKey += token;
        }

        // This quarantees that the session dosen't contain any resources it
        // shouldn't
        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        // Set all the form properties, falling back to the default user
        // preferences if the key is not set. (In the case of multi-portlet)
        Integer numberOfAlerts;
        long past;
        String priority;
        String selectedOrAll;

        pForm.setTitle(dashPrefs.getValue(titleKey, ""));

        numberOfAlerts = new Integer(dashPrefs.getValue(countKey, dashPrefs.getValue(PropertiesForm.ALERT_NUMBER)));

        past = Long.parseLong(dashPrefs.getValue(timeKey, dashPrefs.getValue(PropertiesForm.PAST)));

        priority = dashPrefs.getValue(priorityKey, dashPrefs.getValue(PropertiesForm.PRIORITY));

        selectedOrAll = dashPrefs.getValue(selOrAllKey, dashPrefs.getValue(PropertiesForm.SELECTED_OR_ALL));

        DashboardUtils.verifyResources(resKey, getServlet().getServletContext(), dashPrefs, user, appdefBoss, authzBoss);

        pForm.setNumberOfAlerts(numberOfAlerts);
        pForm.setPast(past);
        pForm.setPriority(priority);
        pForm.setSelectedOrAll(selectedOrAll);

        List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(resKey, dashPrefs);
        AppdefEntityID[] aeids = entityIds.toArray(new AppdefEntityID[entityIds.size()]);

        PageControl pc = RequestUtils.getPageControl(request);
        PageList<AppdefResourceValue> resources = appdefBoss.findByIds(sessionId.intValue(), aeids, pc);
        request.setAttribute("criticalAlertsList", resources);
        return null;
    }
}
