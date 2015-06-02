package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import java.util.ArrayList;
import java.util.List;

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
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;


@Component("recentlyApprovedViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewActionNG.class);
	
	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private DashboardManager dashboardManager;

	public void execute(TilesRequestContext reqContext, AttributeContext attrContext) {
		// TODO Auto-generated method stub
		try {
			HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        int sessionId = user.getSessionId().intValue();

        Integer range = new Integer(dashPrefs.getValue(PropertiesForm.RANGE));

        try {
            // Hard code to look for platforms created in the last two days
            List<PlatformValue> platforms = appdefBoss.findRecentPlatforms(sessionId, 2 * MeasurementConstants.DAY,
                range.intValue());
            attrContext.putAttribute("recentlyAdded", new Attribute( platforms ));
        } catch (Exception e) {
            List<PlatformValue> emptyList = new ArrayList<PlatformValue>();
            attrContext.putAttribute("recentlyApproved", new Attribute( emptyList ) );
            log.debug("Error getting recent platforms: " + e.getMessage(), e);
        }

        // Store the current time in request
        request.setAttribute("current", new Long(System.currentTimeMillis()));
		} catch (Exception ex) {
			log.error(ex);
		}
	}
}
