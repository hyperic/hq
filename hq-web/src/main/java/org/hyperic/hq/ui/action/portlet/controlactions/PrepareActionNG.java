package org.hyperic.hq.ui.action.portlet.controlactions;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;


@Component("controlActionsPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {

	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private DashboardManager dashboardManager;
    
    
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		// TODO Auto-generated method stub
		this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        Integer lastCompleted = new Integer(dashPrefs.getValue(".dashContent.controlActions.lastCompleted"));
        Integer mostFrequent = new Integer(dashPrefs.getValue(".dashContent.controlActions.mostFrequent"));
        Integer nextScheduled = new Integer(dashPrefs.getValue(".dashContent.controlActions.nextScheduled"));
        boolean useLastCompleted = Boolean.valueOf(dashPrefs.getValue(".dashContent.controlActions.useLastCompleted"))
            .booleanValue();
        boolean useMostFrequent = Boolean.valueOf(dashPrefs.getValue(".dashContent.controlActions.useMostFrequent"))
            .booleanValue();
        boolean useNextScheduled = Boolean.valueOf(dashPrefs.getValue(".dashContent.controlActions.useNextScheduled"))
            .booleanValue();
        long past = Long.parseLong(dashPrefs.getValue(".dashContent.controlActions.past"));

        request.setAttribute("past", past);
        request.setAttribute("useNextScheduled", useNextScheduled);
        request.setAttribute("useMostFrequent", useMostFrequent);
        request.setAttribute("useLastCompleted", useLastCompleted);
        request.setAttribute("nextScheduled", nextScheduled);
        request.setAttribute("mostFrequent", mostFrequent);
        request.setAttribute("lastCompleted", lastCompleted);
	}

}
