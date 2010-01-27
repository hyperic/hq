/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.availsummary;

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

        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        Integer sessionId = user.getSessionId();
        PropertiesForm pForm = (PropertiesForm) form;
        PageList<AppdefResourceValue> resources = new PageList<AppdefResourceValue>();

        String token = pForm.getToken();

        String resKey = PropertiesForm.RESOURCES;
        String numKey = PropertiesForm.NUM_TO_SHOW;
        String titleKey = PropertiesForm.TITLE;

        if (token != null) {
            resKey += token;
            numKey += token;
            titleKey += token;
        }

        // Clean up session attributes
        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        // We set defaults here rather than in DefaultUserPreferences.properites

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        Integer numberToShow = new Integer(dashPrefs.getValue(numKey, "10"));
        pForm.setNumberToShow(numberToShow);

        pForm.setTitle(dashPrefs.getValue(titleKey, ""));

        List<AppdefEntityID> resourceList = DashboardUtils.preferencesAsEntityIds(resKey, dashPrefs);
        AppdefEntityID[] aeids = resourceList.toArray(new AppdefEntityID[resourceList.size()]);

        PageControl pc = RequestUtils.getPageControl(request);
        resources = appdefBoss.findByIds(sessionId.intValue(), aeids, pc);
        request.setAttribute("availSummaryList", resources);

        return null;
    }
}
