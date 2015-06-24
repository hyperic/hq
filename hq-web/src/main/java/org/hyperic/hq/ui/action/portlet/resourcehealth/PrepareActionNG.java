package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
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
import org.hyperic.util.pager.Pager;
import org.springframework.stereotype.Component;

@Component("resourcehealthPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(PrepareActionNG.class);
	
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;
	@Resource
	private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		
		try {
			this.request = getServletRequest();
			HttpSession session = request.getSession();
			WebUser user;
			
				user = RequestUtils.getWebUser(session);
			
			DashboardConfig dashConfig = dashboardManager
					.findDashboard((Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
							authzBoss);
			ConfigResponse dashPrefs = dashConfig.getConfig();
	
			DashboardUtils.verifyResources(	Constants.USERPREF_KEY_FAVORITE_RESOURCES, dashPrefs, user, appdefBoss, authzBoss);
			// this quarantees that the session dosen't contain any resources it
			// shouldnt
			SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
			List<AppdefResourceValue> resources = preferencesAsResources( Constants.USERPREF_KEY_FAVORITE_RESOURCES, user, dashPrefs);
	
			Pager pendingPager = Pager.getDefaultPager();
			PageList viewableResourses = pendingPager.seek(resources, PageControl.PAGE_ALL);
	
			viewableResourses.setTotalSize(resources.size());
	
			request.setAttribute(Constants.RESOURCE_HEALTH_LIST, viewableResourses);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			log.error(ex.getMessage());
		}

	}

	private List<AppdefResourceValue> preferencesAsResources(String key,
			WebUser user, ConfigResponse config)
			throws Exception {
		List resourceList = config.getPreferenceAsList(key, Constants.DASHBOARD_DELIMITER);
		return DashboardUtils.listAsResources(resourceList, user,
				appdefBoss);
	}
}
