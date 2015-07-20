package org.hyperic.hq.ui.action.portlet.savedqueries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("savedQueriesPrepareActionNG")
public class PrepareActionNG extends BaseActionNG  implements ViewPreparer
{
	 private final Log log = LogFactory.getLog(PrepareAction.class.getName());
	 @Resource
	    private AuthzBoss authzBoss;
	 @Resource
	    private DashboardManager dashboardManager;
	/* @Autowired
	    public PrepareAction(AuthzBoss authzBoss, DashboardManager dashboardManager) {
	        super();
	        this.authzBoss = authzBoss;
	        this.dashboardManager = dashboardManager;
	  }
	 */
	    
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		
		// TODO Auto-generated method stub
		log.trace("getting saved charts associated with user ");
		
        WebUser user = null;  
		try {
			request = ServletActionContext.getRequest();
			user = RequestUtils.getWebUser(request);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) request.getSession().getAttribute(
            Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        List<String> chartList = StringUtil.explode(dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS),
            StringConstants.DASHBOARD_DELIMITER);

        ArrayList<KeyValuePair> charts = new ArrayList<KeyValuePair>();

        for (Iterator<String> i = chartList.iterator(); i.hasNext();) {
            StringTokenizer st = new StringTokenizer(i.next(), ",");
            if (st.countTokens() >= 2)
                charts.add(new KeyValuePair(st.nextToken(), st.nextToken()));
        }
        request.setAttribute("charts", charts);
        request.setAttribute("chartsize", String.valueOf(charts.size()));
        request.setAttribute("paggingList", getPaggingList(charts.size()));	
	}

}
