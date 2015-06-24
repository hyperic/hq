package org.hyperic.hq.ui.action.portlet.savedqueries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.portlet.recentlyApproved.PropertiesFormNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("savedQueriesModifyActionNG")
public class ModifyActionNG extends BaseActionNG implements
		ModelDriven<PropertiesFormNG> {
	@Resource
	private ConfigurationProxy configurationProxy;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;
	
	PropertiesFormNG pform=new PropertiesFormNG();


	public String update()
			throws Exception {
		HttpSession session = request.getSession();
		WebUser user = RequestUtils.getWebUser(request);

		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);
		ConfigResponse dashPrefs = dashConfig.getConfig();

		PropertiesForm form = null;
		PropertiesForm pForm = (PropertiesForm) form;
		String forward = checkSubmit(pForm);
		String returnString = "success";
		if (forward != null) {
			return forward;
		}

		String[] charts = pForm.getCharts();
		if (charts != null && pForm.isDeleteClicked()) {
			String userCharts = dashPrefs
					.getValue(Constants.USER_DASHBOARD_CHARTS);

			for (int i = 0; i < charts.length; i++) {
				userCharts = StringUtil.remove(userCharts, charts[i]);
			}
			dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, userCharts);
			returnString = "remove";
		} else {
			// Sort by order
			List<String> chartList = new ArrayList<String>();
			chartList.add(dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS));
			chartList.add(dashPrefs
					.getValue(StringConstants.DASHBOARD_DELIMITER));

			for (Iterator<String> it = chartList.iterator(); it.hasNext();) {
				if ("null".equals(it.next()))
					it.remove();
			}

			String[] orderedCharts = new String[chartList.size()];

			StringTokenizer orderTK = new StringTokenizer(pForm.getOrder(),
					"=&");
			for (int i = 0; orderTK.hasMoreTokens(); i++) {
				orderTK.nextToken(); // left-hand
				int index = Integer.parseInt(orderTK.nextToken()); // index
				orderedCharts[i] = (String) chartList.get(index - 1);
			}

			dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, StringUtil
					.arrayToString(orderedCharts,
							StringConstants.DASHBOARD_DELIMITER.charAt(0)));
		}
 
		configurationProxy.setDashboardPreferences(session, user, dashPrefs);

		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in savedqueries/ModifyAction "
						+ " for " + user.getId() + " at "
						+ System.currentTimeMillis() + " user.prefs = "
						+ dashPrefs.getKeys().toString());
		//return mapping.findForward(returnString);
                     
					return SUCCESS;	
	}
	
    @SkipValidation
    public String cancel() throws Exception {
        clearErrorsAndMessages();
        return "cancel";
    }

    @SkipValidation
    public String reset() throws Exception {
    	pform.reset();
        clearErrorsAndMessages();
        return "reset";
    }

	public PropertiesFormNG getModel() {
		// TODO Auto-generated method stub
		return pform;
	}

}
