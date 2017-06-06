package org.hyperic.hq.ui.action.portlet.controlactions;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.beans.DashboardControlBean;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.springframework.stereotype.Component;


@Component("controlActionsViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {
	
	private final Log log = LogFactory.getLog(ViewActionNG.class);

	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private ControlBoss controlBoss;
	@Resource
    private AppdefBoss appdefBoss;
	@Resource
    private DashboardManager dashboardManager;
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
        StopWatch timer = new StopWatch();
        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");

        this.request = getServletRequest();
        Map<String,Object>  context = tilesContext.getRequestScope();
        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        int sessionId = user.getSessionId().intValue();

        Boolean lastCompleted = Boolean.valueOf(dashPrefs.getValue(".ng.dashContent.controlActions.useLastCompleted",
            Boolean.TRUE.toString()));
        context.put("displayLastCompleted", lastCompleted);

        Boolean mostFrequent = new Boolean(dashPrefs.getValue(".ng.dashContent.controlActions.useMostFrequent",
            Boolean.FALSE.toString()));
        context.put("displayMostFrequent", mostFrequent);

        Boolean nextScheduled = new Boolean(dashPrefs.getValue(".ng.dashContent.controlActions.useNextScheduled",
            Boolean.TRUE.toString()));
        context.put("displayNextScheduled", nextScheduled);

        if (lastCompleted.booleanValue()) {
            int rows = Integer.parseInt(dashPrefs.getValue(".ng.dashContent.controlActions.lastCompleted", "5"));
            long past = Long.parseLong(dashPrefs.getValue(".ng.dashContent.controlActions.past", "604800000"));
            PageList<ControlHistory> pageList = controlBoss.getRecentControlActions(sessionId, rows, past);
            context.put("lastCompleted", pageList);
        }

        if (nextScheduled.booleanValue()) {
            int rows = Integer.parseInt(dashPrefs.getValue(".ng.dashContent.controlActions.nextScheduled", "5"));
            PageList<ControlSchedule> pageList = controlBoss.getPendingControlActions(sessionId, rows);

            PageList<DashboardControlBean> pendingList = new PageList<DashboardControlBean>();
            pendingList.setTotalSize(pageList.getTotalSize());

            for (ControlSchedule control : pageList) {

                DashboardControlBean bean = new DashboardControlBean();
                try {
                    AppdefEntityID entity = new AppdefEntityID(control.getEntityType().intValue(), control
                        .getEntityId());
                    bean.setResource(appdefBoss.findById(sessionId, entity));
                    bean.setControl(control);
                    pendingList.add(bean);
                } catch (NullPointerException e) {
                    // ignore the error don't add it to the page this is
                    // added as a result of bug #7596
                }
            }

            context.put("nextScheduled", pendingList);
        }

        if (mostFrequent.booleanValue()) {

            int size = Integer.parseInt(dashPrefs.getValue(".ng.dashContent.controlActions.mostFrequent"));
            PageList<ControlFrequencyValue> pageList;
			
			pageList = controlBoss.getOnDemandControlFrequency(sessionId, size);

            context.put("mostFrequent", pageList);
        }
        timingLog.trace("ViewControl- timing [" + timer.toString() + "]");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
	}
	
}
