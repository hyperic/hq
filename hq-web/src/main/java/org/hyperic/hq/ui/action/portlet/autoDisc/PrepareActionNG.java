package org.hyperic.hq.ui.action.portlet.autoDisc;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

@Component("autoDiscPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(PrepareActionNG.class);
	
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private DashboardManager dashboardManager;

	public void execute(TilesRequestContext requestContext, AttributeContext attrContext) {
		try {
			request = ServletActionContext.getRequest();
	
	        HttpSession session = request.getSession();
	
	        WebUser user = RequestUtils.getWebUser(session);
	
	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        ConfigResponse dashPrefs = dashConfig.getConfig();
	        Integer range = new Integer(dashPrefs.getValue(".ng.dashContent.autoDiscovery.range"));
	
	        requestContext.getRequestScope().put("range", range);
		} catch (Exception ex) {
			log.error(ex);
		}
	}

}
