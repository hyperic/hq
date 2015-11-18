package org.hyperic.hq.ui.action.portlet.savedqueries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.ServerValue;
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
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.stereotype.Component;

@Component("savedQueriesPrepareActionNG")
public class PrepareActionNG extends BaseActionNG  implements ViewPreparer
{
	 private final Log log = LogFactory.getLog(PrepareActionNG.class.getName());
	 @Resource
	    private AuthzBoss authzBoss;
	 @Resource
	    private DashboardManager dashboardManager;
	 
	 private Pager valuePager;
	 
	 private static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_server";
	    
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
			log.error(e,e);
		}
		
		if (valuePager == null) {
			try {
				valuePager = Pager.getPager(VALUE_PROCESSOR);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e,e);
			}
		}
		
		PageControl pc = RequestUtils.getPageControl(getServletRequest(),
				"ps", "pn", "so", "sc");

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) request.getSession().getAttribute(
            Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        List<String> chartList = StringUtil.explode(dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS),
            StringConstants.DASHBOARD_DELIMITER);

        ArrayList<KeyValuePair> charts = new ArrayList<KeyValuePair>();
        PageList<KeyValuePair> newCharts = new PageList<KeyValuePair>();

        for (Iterator<String> i = chartList.iterator(); i.hasNext();) {
            StringTokenizer st = new StringTokenizer(i.next(), ",");
            if (st.countTokens() >= 2) {
            	String temp1 = st.nextToken();
            	String temp2 = st.nextToken();
                charts.add(new KeyValuePair(temp1, temp1));
            	// newCharts.add(new KeyValuePair(temp1, temp1));
            }
        }
        newCharts = valuePager.seek(charts, pc);
        // If sort descending, then reverse the list
        if(pc != null && pc.isDescending()) {
            Collections.reverse(newCharts);
        }
        
        request.setAttribute("charts", newCharts);
        request.setAttribute("chartsize", String.valueOf(charts.size()));
        request.setAttribute("paggingList", getPaggingList(charts.size()));	
	}

}
