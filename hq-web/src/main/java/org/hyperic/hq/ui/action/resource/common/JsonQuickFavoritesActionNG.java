package org.hyperic.hq.ui.action.resource.common;

import java.io.InputStream;

import javax.annotation.Resource;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("jsonQuickFavoritesActionNG")
@Scope("prototype")
public class JsonQuickFavoritesActionNG extends BaseActionNG {

	@Resource
	private ConfigurationProxy configurationProxy;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;
	@Resource
	private SessionManager sessionManager;
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}
	
	
	
	public String execute() throws Exception {

		request = getServletRequest();
		super.execute();
		JsonActionContextNG ctx = this.setJSONContext();

		JSONObject ajaxJson = new JSONObject();

		String result = this.flipFavorite();
		ajaxJson.put("result", result);
		JSONResult res = new JSONResult(ajaxJson);
		ctx.setJSONResult(res);

		inputStream = this.streamJSONResult(ctx);

		return null;
	}
	
	private String flipFavorite() throws Exception {
		WebUser user = RequestUtils.getWebUser(request);
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
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

}
