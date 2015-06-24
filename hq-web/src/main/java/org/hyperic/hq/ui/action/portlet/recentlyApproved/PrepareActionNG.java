package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component("recentlyApprovedPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {
    @Resource
	private AuthzBoss authzBoss;
    @Resource
    private DashboardManager dashboardManager;

    private PropertiesForm pForm;


  /*  @Autowired
    public PrepareAction(AuthzBoss authzBoss, DashboardManager dashboardManager) {
        super();
        this.authzBoss = authzBoss;
        this.dashboardManager = dashboardManager;
    }
*/
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		 
			request = ServletActionContext.getRequest();
	        HttpSession session = request.getSession();
	        WebUser user = null;
			try {
				user = RequestUtils.getWebUser(session);
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        ConfigResponse dashPrefs = dashConfig.getConfig();

	        Integer range = new Integer(dashPrefs.getValue(PropertiesForm.RANGE));

	        
	        tilesContext.getRequestScope().put("range", range);

		
	}

}
