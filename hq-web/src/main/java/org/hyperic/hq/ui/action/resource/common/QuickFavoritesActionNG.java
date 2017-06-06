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

import javax.annotation.Resource;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("quickFavoritesActionNG")
@Scope("prototype")
public class QuickFavoritesActionNG extends BaseActionNG {
	
	@Resource
	private ConfigurationProxy configurationProxy;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;
	@Resource
	private SessionManager sessionManager;
	
	private String internalEid;
	
	public String execute() throws Exception {
		WebUser user = RequestUtils.getWebUser(request);
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		internalEid = aeid.toString();
		String mode = request.getParameter(Constants.MODE_PARAM);
		String[] dashboardIds = request.getParameterValues(Constants.DASHBOARD_ID_PARAM);

		request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

		if (mode == null) {
			return INPUT;
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
							Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG,
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
						return INPUT;
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
					return SUCCESS;
				}

				// Add to favorites and save
				DashboardUtils.addEntityToPreferences(
						Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG,
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
				return SUCCESS;
			}

			// Remove from favorites and save
			DashboardUtils.removeResources(
					new String[] { aeid.getAppdefKey() },
					Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG, configResponse);
			configurationProxy.setUserDashboardPreferences(configResponse, user);
		} else {
			// Not an add or remove, what the heck is it? It's an error.
			return INPUT;
		}
		
		return SUCCESS;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

}
