package org.hyperic.hq.ui.action.portlet.summaryCounts;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;
import org.springframework.stereotype.Component;


@Component("summaryCountsViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewActionNG.class);
	
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private DashboardManager dashboardManager;
    private final Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");
	
	
	public void execute(TilesRequestContext requestContext, AttributeContext attrContext) {
        StopWatch timer = new StopWatch();
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        try {
        	AppdefInventorySummary summary = appdefBoss.getInventorySummary(user.getSessionId().intValue(), true);
        	attrContext.putAttribute("summary", new Attribute( summary ));
        } catch (Exception ex) {
        	log.error(ex);
        	return;
        }

        

        // get all the displayed subtypes
        attrContext.putAttribute("application", new Attribute( new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.application")) ) );
        attrContext.putAttribute("applicationTypes", new Attribute(  StringUtil.explode(dashPrefs
            .getValue(".dashContent.summaryCounts.applicationTypes"), ",") ) );

        attrContext.putAttribute("platform", new Attribute(  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.platform")) ) );
        attrContext.putAttribute("platformTypes", new Attribute(  StringUtil.explode(dashPrefs
            .getValue(".dashContent.summaryCounts.platformTypes"), ",") ) );

        attrContext.putAttribute("server", new Attribute(  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.server")) ) );
        attrContext.putAttribute("serverTypes", new Attribute( StringUtil.explode(dashPrefs
            .getValue(".dashContent.summaryCounts.serverTypes"), ",") ) );

        attrContext.putAttribute("service", new Attribute(  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.service")) ) );
        attrContext.putAttribute("serviceTypes", new Attribute(  StringUtil.explode(dashPrefs
            .getValue(".dashContent.summaryCounts.serviceTypes"), ",") ) );

        attrContext.putAttribute("cluster", new Attribute(  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.cluster")) ) );
        attrContext.putAttribute("clusterTypes", new Attribute(  StringUtil.explode(dashPrefs
            .getValue(".dashContent.summaryCounts.group.clusterTypes"), ",") ) );

        attrContext.putAttribute("groupMixed", new Attribute(  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.mixed")) ) );
        attrContext.putAttribute("groupGroups", new Attribute( new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.groups")) ) );
        attrContext.putAttribute("groupPlatServerService", new Attribute(  new Boolean(dashPrefs
            .getValue(".dashContent.summaryCounts.group.plat.server.service")) ) );
        attrContext.putAttribute("groupApplication", new Attribute(  new Boolean(dashPrefs
            .getValue(".dashContent.summaryCounts.group.application")) ) );

        timingLog.trace("SummaryCounts - timing [" + timer.toString() + "]");

	}

}
