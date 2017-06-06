package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


@Component("resourceHealthModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements ModelDriven<PropertiesFormNG> {

    @Resource
	private ConfigurationProxy configurationProxy;
    @Resource    
    private AuthzBoss authzBoss;
    @Resource
    private DashboardManager dashboardManager;
	
    
	PropertiesFormNG pForm=new PropertiesFormNG();

	
	
	public String update() throws Exception {

		this.request = getServletRequest();
		HttpSession session = request.getSession();
		WebUser user = RequestUtils.getWebUser(request);

		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);
		ConfigResponse dashPrefs = dashConfig.getConfig();
		String forwardStr = SUCCESS;

		if (pForm.isRemoveClicked()) {
			DashboardUtils.removeResources(pForm.getIds(),
					Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG, dashPrefs);
			forwardStr = "review";
			configurationProxy
					.setDashboardPreferences(session, user, dashPrefs);
		}

		String forward = checkSubmit(pForm);
		
        if (forward != null) {
            return forward;
        }

        // Set the order of resources
		String order = StringUtil.replace(pForm.getOrder(), "%3A", ":");
		StringTokenizer orderTK = new StringTokenizer(order, "=&");
		ArrayList<String> resources = new ArrayList<String>();
		while (orderTK.hasMoreTokens()) {
			orderTK.nextToken();
			resources.add(orderTK.nextToken());
		}
		configurationProxy.setPreference(session, user,
				Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG, StringUtil
						.listToString(resources,
								StringConstants.DASHBOARD_DELIMITER));

		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in resourcehealth/ModifyAction "
						+ " for " + user.getId() + " at "
						+ System.currentTimeMillis() + " user.prefs = "
						+ dashPrefs.getKeys().toString());

		session.removeAttribute(Constants.USERS_SES_PORTAL);
		return forwardStr;
	}
    
    @SkipValidation
    public String cancel() throws Exception {
        clearErrorsAndMessages();
        clearCustomErrorMessages();
        return "cancel";
    }

    @SkipValidation
    public String reset() throws Exception {
    	pForm.reset();
        clearErrorsAndMessages();
        clearCustomErrorMessages();
        return "reset";
    }
   
	public PropertiesFormNG getModel() {	
		return pForm;
	}
	
	public PropertiesFormNG getPForm() {
		return pForm;
	}

	public void setPForm(PropertiesFormNG pForm) {
		this.pForm = pForm;
	}
}
