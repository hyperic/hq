package org.hyperic.hq.ui.action.portlet.criticalalerts;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("criticAlalertsPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(PrepareActionNG.class);
	
	@Resource
	private AppdefBoss appdefBoss;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
		this.request = getServletRequest();
        HttpSession session = request.getSession();
		WebUser user;
		
		user = RequestUtils.getWebUser(session);

		Integer sessionId = user.getSessionId();

		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);
		ConfigResponse dashPrefs = dashConfig.getConfig();

		//String token = pForm.getToken();
		String token="";

		// For multi-portlet configurations
		String resKey = JsonLoadCriticalAlertsNG.RESOURCES_KEY;
		String countKey = PropertiesFormNG.ALERT_NUMBER;
		String priorityKey = PropertiesFormNG.PRIORITY;
		String timeKey = PropertiesFormNG.PAST;
		String selOrAllKey = PropertiesFormNG.SELECTED_OR_ALL;
		String titleKey = PropertiesFormNG.TITLE;

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

		request.setAttribute("title", dashPrefs.getValue(titleKey, ""));
		
		numberOfAlerts = new Integer(dashPrefs.getValue(countKey,
				dashPrefs.getValue(PropertiesFormNG.ALERT_NUMBER)));

		past = Long.parseLong(dashPrefs.getValue(timeKey,
				dashPrefs.getValue(PropertiesFormNG.PAST)));

		priority = dashPrefs.getValue(priorityKey,
				dashPrefs.getValue(PropertiesFormNG.PRIORITY));

		selectedOrAll = dashPrefs.getValue(selOrAllKey,
				dashPrefs.getValue(PropertiesFormNG.SELECTED_OR_ALL));

		DashboardUtils.verifyResources(resKey, dashPrefs, user, appdefBoss, authzBoss);

		request.setAttribute("numberOfAlerts", numberOfAlerts);
		request.setAttribute("past", past);
		request.setAttribute("priority", priority);
		request.setAttribute("selectedOrAll", selectedOrAll);

		List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(
				resKey, dashPrefs);
		AppdefEntityID[] aeids = entityIds.toArray(new AppdefEntityID[entityIds
				.size()]);

		PageControl pc = RequestUtils.getPageControl(request);
		PageList<AppdefResourceValue> resources = appdefBoss.findByIds(
				sessionId.intValue(), aeids, pc);
		request.setAttribute("criticalAlertsList", resources);
		request.setAttribute("titleDescription", dashPrefs.getValue(titleKey, ""));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}

}
