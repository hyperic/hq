/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class DisplayDashboardAction
    extends TilesAction {

    private ConfigurationProxy configurationProxy;

    private AuthzBoss authzBoss;

    private DashboardManager dashboardManager;

    private UpdateBoss updateBoss;

    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public DisplayDashboardAction(ConfigurationProxy configurationProxy, AuthzBoss authzBoss,
                                  DashboardManager dashboardManager, UpdateBoss updateBoss,
                                  AuthzSubjectManager authzSubjectManager) {
        super();
        this.configurationProxy = configurationProxy;
        this.authzBoss = authzBoss;
        this.dashboardManager = dashboardManager;
        this.updateBoss = updateBoss;
        this.authzSubjectManager = authzSubjectManager;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        DashboardForm dForm = (DashboardForm) form;
        WebUser user = RequestUtils.getWebUser(request);

        AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
        Portal portal = (Portal) session.getAttribute(Constants.USERS_SES_PORTAL);
        portal = new Portal();

        portal.setName("dashboard.template.title");
        portal.setColumns(2);

        // Set dashboard string list for the dashboard select list
        List<DashboardConfig> dashboardCollection = (ArrayList<DashboardConfig>) dashboardManager.getDashboards(me);
        List<Dashboard> dashboards = new ArrayList<Dashboard>();

        for (Iterator<DashboardConfig> i = dashboardCollection.iterator(); i.hasNext();) {
            DashboardConfig config = i.next();
            Dashboard dashboard = new Dashboard();

            dashboard.setId(config.getId());
            dashboard.set_name(config.getName());
            dashboards.add(dashboard);
        }

        if (dashboards.size() > 0) {
            dForm.setDashboards(dashboards);
        } else {
            dashboards.add(new Dashboard(""));
            dForm.setDashboards(dashboards);
        }

        // Check if there is a default dashboard, selected dashboard or none of
        // the above
        Integer selectedDashboard = SessionUtils.getIntegerAttribute(session, Constants.SELECTED_DASHBOARD_ID, null);
        String defaultDashboard = user.getPreference(Constants.DEFAULT_DASHBOARD_ID, null);
        DashboardConfig dashboardConfig = null;

        if (defaultDashboard != null && selectedDashboard == null) {
            // If this is a fresh session, selected dashboard id won't be set so
            // we'll need to
            // initially set it to the default dashboard id.
            dashboardConfig = DashboardUtils.findDashboard((ArrayList<DashboardConfig>) dashboardCollection, Integer
                .valueOf(defaultDashboard));
        } else if (dashboardCollection.size() == 1) {
            // No need to select a default - only one available
            dashboardConfig = (DashboardConfig) dashboardCollection.get(0);
            defaultDashboard = dashboardConfig.getId().toString();
        } else if (selectedDashboard != null) {
            // If we have a selected dashboard id, find it in the list of
            // dashboards
            // if it has been removed, inform the user
            dashboardConfig = DashboardUtils.findDashboard((ArrayList<DashboardConfig>) dashboardCollection,
                selectedDashboard);
        }

        if (dashboardConfig == null) {
            // Either no default/selected dashboard or default dashboard no
            // longer exists
            // in both cases, we'll set default dashboard to the user dashboard
            dashboardConfig = dashboardManager.getUserDashboard(me, me);
            defaultDashboard = dashboardConfig.getId().toString();

            // update preferences
            user.setPreference(Constants.DEFAULT_DASHBOARD_ID, defaultDashboard);
            authzBoss.setUserPrefs(user.getSessionId(), user.getSubject().getId(), user.getPreferences());
        }

        // Update the sessions with the selected dashboard
        session.setAttribute(Constants.SELECTED_DASHBOARD_ID, dashboardConfig.getId());

        // Update the form with whatever values we figure out above
        dForm.setSelectedDashboardId(dashboardConfig.getId().toString());
        dForm.setDefaultDashboard(defaultDashboard);

        if (dashboardManager.isEditable(me, dashboardConfig)) {
            session.setAttribute(Constants.IS_DASH_EDITABLE, "true");
        } else {
            session.removeAttribute(Constants.IS_DASH_EDITABLE);
        }

        // Dashboard exists, display it
        ConfigResponse dashPrefs = dashboardConfig.getConfig();

        // See if we need to initialize the dashboard (for Roles)
        // we now check both columns for null-ness, instead of only the first
        // TODO This typically will only occur if the role was created via API
        // We'll need to add some logic to role creation in hqapi so that
        // dashboard creation happens, however for 4.2 this is not something
        // we can squeeze in, this will suffice for the time being
        if (dashPrefs.getValue(Constants.USER_PORTLETS_FIRST) == null &&
            dashPrefs.getValue(Constants.USER_PORTLETS_SECOND) == null) {
            ConfigResponse defaultRoleDashPrefs = (ConfigResponse) getServlet().getServletContext().getAttribute(
                Constants.DEF_ROLE_DASH_PREFS);
            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

            dashboardManager.configureDashboard(overlord, dashboardConfig, defaultRoleDashPrefs);
            dashPrefs.merge(defaultRoleDashPrefs, true);
        }

        portal.addPortletsFromString(dashPrefs.getValue(Constants.USER_PORTLETS_FIRST), 1);
        portal.addPortletsFromString(dashPrefs.getValue(Constants.USER_PORTLETS_SECOND), 2);

        // Go through the portlets and see if they have descriptions
        for (Iterator<Collection<Portlet>> pit = portal.getPortlets().iterator(); pit.hasNext();) {
            Collection<Portlet> portlets = pit.next();

            for (Iterator<Portlet> it = portlets.iterator(); it.hasNext();) {
                Portlet portlet = it.next();
                String titleKey = portlet.getUrl() + ".title" + (portlet.getToken() != null ? portlet.getToken() : "");

                portlet.setDescription(dashPrefs.getValue(titleKey, ""));
            }
        }

        session.setAttribute(Constants.USERS_SES_PORTAL, portal);

        // Make sure there's a valid RSS auth token
        ConfigResponse dashCfg = dashboardManager.getUserDashboard(me, me).getConfig();
        String rssToken = dashCfg.getValue(Constants.RSS_TOKEN);

        if (rssToken == null) {
            rssToken = String.valueOf(session.hashCode());

            dashCfg.setValue(Constants.RSS_TOKEN, rssToken);
            // Now store the RSS auth token
            configurationProxy.setUserDashboardPreferences(dashCfg, user);
        }

        session.setAttribute("rssToken", rssToken);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        Map<String, Object> userOpsMap = (Map<String, Object>) session.getAttribute(Constants.USER_OPERATIONS_ATTR);

        if (userOpsMap.containsKey(AuthzConstants.rootOpCAMAdmin)) {
            // Now check for updates

            try {
                RequestUtils.getStringParameter(request, "update");
                updateBoss.ignoreUpdate();
            } catch (ParameterNotFoundException e) {
                String update = updateBoss.getUpdateReport();

                if (update != null) {
                    request.setAttribute("HQUpdateReport", update);
                }
            }
        }

        return null;
    }
}