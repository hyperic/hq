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
        this.request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        try {
        	AppdefInventorySummary summary = appdefBoss.getInventorySummary(user.getSessionId().intValue(), true);
        	requestContext.getRequestScope().put("summary",  summary );
        	// attrContext.putAttribute("summary", new Attribute( summary ));
        } catch (Exception ex) {
        	log.error(ex);
        	return;
        }

        

        // get all the displayed subtypes
        // attrContext.putAttribute("application", new Attribute( new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.application")) ) );
        requestContext.getRequestScope().put("scApplication", new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.application")) );
        requestContext.getRequestScope().put("scApplicationTypes", (  StringUtil.explode(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.applicationTypes"), ",") ) );

        requestContext.getRequestScope().put("scPlatform",  new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.platform")) ) ;
        requestContext.getRequestScope().put("scPlatformTypes",   StringUtil.explode(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.platformTypes"), ",") ) ;

        requestContext.getRequestScope().put("scServer", new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.server"))  );
        requestContext.getRequestScope().put("scServerTypes", StringUtil.explode(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.serverTypes"), ",")  );

        requestContext.getRequestScope().put("scService",   new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.service"))  );
        requestContext.getRequestScope().put("scServiceTypes",  StringUtil.explode(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.serviceTypes"), ",")  );

        requestContext.getRequestScope().put("scCluster",   new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.group.cluster"))  );
        requestContext.getRequestScope().put("scClusterTypes",   StringUtil.explode(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.group.clusterTypes"), ",")  );

        requestContext.getRequestScope().put("scGroupMixed",   new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.group.mixed"))  );
        requestContext.getRequestScope().put("scGroupGroups",new Boolean(dashPrefs.getValue(".ng.dashContent.summaryCounts.group.groups"))  );
        requestContext.getRequestScope().put("scGroupPlatServerService",   new Boolean(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.group.plat.server.service")) );
        requestContext.getRequestScope().put("scGroupApplication",  new Boolean(dashPrefs
            .getValue(".ng.dashContent.summaryCounts.group.application")) );

        timingLog.trace("SummaryCounts - timing [" + timer.toString() + "]");

	}

}
