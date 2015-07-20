package org.hyperic.hq.ui.action.portlet.criticalalerts;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

@Component("criticAlalertsViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {
	private final Log log = LogFactory.getLog(JsonLoadCriticalAlertsNG.class
			.getName());

	static final String RESOURCES_KEY = Constants.USERPREF_KEY_CRITICAL_ALERTS_RESOURCES_NG;

	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		// Most load logic was moved to the Json loader instead
		// (JsonLoadCriticalAlertsNG)
		try {
			this.request = getServletRequest();
			HttpSession session = request.getSession();
			WebUser user = RequestUtils.getWebUser(session);
			DashboardConfig dashConfig = dashboardManager.findDashboard(
					(Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, authzBoss);

			ConfigResponse dashPrefs = dashConfig.getConfig();
			String titleKey = PropertiesFormNG.TITLE;
			request.setAttribute("titleDescription",
					dashPrefs.getValue(titleKey, ""));

		} catch (Exception ex) {
			log.error("missing dashConfig for key "
					+ Constants.SELECTED_DASHBOARD_ID);
		}
	}

}
