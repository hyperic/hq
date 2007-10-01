package org.hyperic.hq.ui.util;

import java.rmi.RemoteException;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.session.DashboardConfig;
import org.hyperic.hq.bizapp.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.bizapp.server.session.RoleDashboardConfig;
import org.hyperic.hq.bizapp.server.session.UserDashboardConfig;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardManagerLocal;
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
			DashboardConfig dashboardConfig = (DashboardConfig) session
					.getAttribute(Constants.SELECTED_DASHBOARD);
			ConfigResponse dashboardConfigResp = dashboardConfig.getConfig();
			dashboardConfigResp.setValue(key, value);
			if (dashboardConfig.isUserConfig()) {
				dashManager.configureDashboard(me,
						(UserDashboardConfig) dashboardConfig,
						dashboardConfigResp);
			} else {
				dashManager.configureDashboard(me,
						(RoleDashboardConfig) dashboardConfig,
						dashboardConfigResp);
			}
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
		DashboardConfig dashConfig = (DashboardConfig) session
				.getAttribute(Constants.SELECTED_DASHBOARD);
		dashManager.configureDashboard(me, dashConfig,
				dashConfigResp);
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
