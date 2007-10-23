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

import java.rmi.RemoteException;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.util.config.ConfigResponse;

public class ConfigurationProxy {

	private static ConfigurationProxy _configProxy = new ConfigurationProxy();

	public static ConfigurationProxy getInstance() {
		return _configProxy;
	}

	public void setPreference(HttpSession session, WebUser user,
			AuthzBoss boss, String key, String value)
			throws ApplicationException, RemoteException {
		if (key.substring(0, 5).equalsIgnoreCase(".dash")) {
			// Dashboard preference
			DashboardManagerLocal dashManager = DashboardManagerEJBImpl
					.getOne();
			AuthzSubject me = boss.findSubjectById(user.getSessionId(), user
					.getSubject().getId());
			DashboardConfig dashConfig = DashboardUtils.findDashboard(
					(Integer) session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, boss);
			ConfigResponse dashboardConfigResp = dashConfig.getConfig();
			dashboardConfigResp.setValue(key, value);
            dashManager.configureDashboard(me, dashConfig,
                                           dashboardConfigResp);
		} else {
			// User preference
			user.setPreference(key, value);
			boss.setUserPrefs(user.getSessionId(), user.getId(), user
					.getPreferences());
		}
	}

	public void setDashboardPreferences(HttpSession session, WebUser user,
			AuthzBoss boss, ConfigResponse dashConfigResp)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException, RemoteException {
		DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
		AuthzSubject me = boss.findSubjectById(user.getSessionId(), user
				.getSubject().getId());
		DashboardConfig dashConfig = DashboardUtils.findDashboard(
				(Integer) session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, boss);
		dashManager.configureDashboard(me, dashConfig, dashConfigResp);
	}

	public void setUserPreferences(HttpSession session, WebUser user,
			AuthzBoss boss, ConfigResponse userPrefs)
			throws ApplicationException, RemoteException {
		user.getPreferences().merge(userPrefs, false);
		boss.setUserPrefs(user.getSessionId(), user.getId(), user
				.getPreferences());
	}

	public void setUserDashboardPreferences(ConfigResponse userPrefs,
			AuthzBoss boss, WebUser user) throws ApplicationException,
			RemoteException {

		DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
		AuthzSubject me = boss.findSubjectById(user.getSessionId(), user
				.getSubject().getId());
		dashManager.configureDashboard(me,
				dashManager.getUserDashboard(me, me), userPrefs);
	}

	public void setRoleDashboardPreferences(ConfigResponse preferences,
			AuthzBoss boss, WebUser user, Role role)
			throws ApplicationException, RemoteException {

		DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
		AuthzSubject me = boss.findSubjectById(user.getSessionId(), user
				.getSubject().getId());
		dashManager.configureDashboard(me, dashManager.getRoleDashboard(me,
				role), preferences);
	}

}
