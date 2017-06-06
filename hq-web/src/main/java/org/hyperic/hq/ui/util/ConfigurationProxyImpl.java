/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.util;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationProxyImpl implements ConfigurationProxy {

    private DashboardManager dashboardManager;
    private AuthzBoss authzBoss;

    @Autowired
    public ConfigurationProxyImpl(DashboardManager dashboardManager, AuthzBoss boss) {
        super();
        this.dashboardManager = dashboardManager;
        this.authzBoss = boss;
    }

    public void setPreference(HttpSession session, WebUser user, String key, String value) throws ApplicationException {
        if (key.toLowerCase().contains(".ng.dash".toLowerCase())) {
            // Dashboard preference

            AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
            DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
                .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
            ConfigResponse dashboardConfigResp = dashConfig.getConfig();
            dashboardConfigResp.setValue(key, value);
            dashboardManager.configureDashboard(me, dashConfig, dashboardConfigResp);
        } else {
        	
        	 if (key.toLowerCase().contains(".dash".toLowerCase())) {
                 // Dashboard preference

                 AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
                 DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
                     .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
                 ConfigResponse dashboardConfigResp = dashConfig.getConfig();
                 dashboardConfigResp.setValue(key, value);
                 dashboardManager.configureDashboard(me, dashConfig, dashboardConfigResp);
        	 } else {
	            // User preference
	            user.setPreference(key, value);
	            authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user.getPreferences());
        	 }
        }
    }

    public void setDashboardPreferences(HttpSession session, WebUser user, ConfigResponse dashConfigResp)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException {

        AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        dashboardManager.configureDashboard(me, dashConfig, dashConfigResp);
    }

    public void setUserPreferences(HttpSession session, WebUser user, ConfigResponse userPrefs)
        throws ApplicationException {
        user.getPreferences().merge(userPrefs, false);
        authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user.getPreferences());
    }

    public void setUserDashboardPreferences(ConfigResponse userPrefs, WebUser user) throws ApplicationException {

        AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
        dashboardManager.configureDashboard(me, dashboardManager.getUserDashboard(me, me), userPrefs);
    }

    public void setRoleDashboardPreferences(ConfigResponse preferences, WebUser user, Role role)
        throws ApplicationException {

        AuthzSubject me = authzBoss.findSubjectById(user.getSessionId(), user.getSubject().getId());
        dashboardManager.configureDashboard(me, dashboardManager.getRoleDashboard(me, role), preferences);
    }

}
