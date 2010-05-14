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

package org.hyperic.hq.ui.action.resource.common;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class QuickFavoritesAction extends BaseAction {

	private ConfigurationProxy configurationProxy;
	private AuthzBoss authzBoss;
	private DashboardManager dashboardManager;
	private SessionManager sessionManager;

	@Autowired
	public QuickFavoritesAction(ConfigurationProxy configurationProxy,
			AuthzBoss authzBoss, DashboardManager dashboardManager,
			SessionManager sessionManager) {
		super();
		this.configurationProxy = configurationProxy;
		this.authzBoss = authzBoss;
		this.dashboardManager = dashboardManager;
		this.sessionManager = sessionManager;
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		WebUser user = RequestUtils.getWebUser(request);

		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		String mode = request.getParameter(Constants.MODE_PARAM);
		String[] dashboardIds = request
				.getParameterValues(Constants.DASHBOARD_ID_PARAM);
		HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);

		forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

		if (mode == null) {
			return returnFailure(request, mapping, forwardParams);
		}

		if (mode.equals(Constants.MODE_ADD)) {
			if (dashboardIds != null) {
				for (int x = 0; x < dashboardIds.length; x++) {
					Integer dashId = Integer.valueOf(dashboardIds[x]);
					DashboardConfig dashboardConfig = dashboardManager
							.findDashboard(dashId, user, authzBoss);
					ConfigResponse configResponse = dashboardConfig.getConfig();
					Boolean isFavorite = QuickFavoritesUtil.isFavorite(
							configResponse, aeid);

					if (isFavorite.booleanValue())
						continue;

					DashboardUtils.addEntityToPreferences(
							Constants.USERPREF_KEY_FAVORITE_RESOURCES,
							configResponse, aeid, Integer.MAX_VALUE);

					if (dashboardConfig instanceof RoleDashboardConfig) {
						RoleDashboardConfig roleDashboardConfig = (RoleDashboardConfig) dashboardConfig;

						configurationProxy.setRoleDashboardPreferences(
								configResponse, user, roleDashboardConfig
										.getRole());
					} else if (dashboardConfig instanceof UserDashboardConfig) {
						configurationProxy.setUserDashboardPreferences(
								configResponse, user);
					} else {
						// Neither role or user dashboard. This shouldn't
						// happen, but if it somehow does, treat it as an error.
						return returnFailure(request, mapping, forwardParams);
					}
				}
			} else {
				ConfigResponse configResponse = DashboardUtils
						.findUserDashboardConfig(user, dashboardManager,
								sessionManager);
				Boolean isFavorite = QuickFavoritesUtil.isFavorite(
						configResponse, aeid);

				// Is this already in the favorites list? Should not happen
				if (isFavorite.booleanValue()) {
					// Just return, it's already there
					return returnSuccess(request, mapping, forwardParams,
							BaseAction.YES_RETURN_PATH);
				}

				// Add to favorites and save
				DashboardUtils.addEntityToPreferences(
						Constants.USERPREF_KEY_FAVORITE_RESOURCES,
						configResponse, aeid, Integer.MAX_VALUE);
				configurationProxy.setUserDashboardPreferences(configResponse,
						user);
			}
		} else if (mode.equals(Constants.MODE_REMOVE)) {
			ConfigResponse configResponse = DashboardUtils
					.findUserDashboardConfig(user, dashboardManager,
							sessionManager);
			Boolean isFavorite = QuickFavoritesUtil.isFavorite(configResponse,
					aeid);

			// Is this not in the favorites list? Should not happen
			if (!isFavorite.booleanValue()) {
				// Already removed, just return
				return returnSuccess(request, mapping, forwardParams,
						BaseAction.YES_RETURN_PATH);
			}

			// Remove from favorites and save
			DashboardUtils.removeResources(
					new String[] { aeid.getAppdefKey() },
					Constants.USERPREF_KEY_FAVORITE_RESOURCES, configResponse);
			configurationProxy
					.setUserDashboardPreferences(configResponse, user);
		} else {
			// Not an add or remove, what the heck is it? It's an error.
			return returnFailure(request, mapping, forwardParams);
		}

		return returnSuccess(request, mapping, forwardParams,
				BaseAction.YES_RETURN_PATH);
	}
}