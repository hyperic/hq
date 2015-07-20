package org.hyperic.hq.ui.action.portlet.availsummary;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
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
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ibm.icu.impl.StringUCharacterIterator;
import com.opensymphony.xwork2.ModelDriven;

@Component("availSummaryModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements ModelDriven<PropertiesFormNG> {

	@Resource
    private ConfigurationProxy configurationProxy;
	@Resource
    private AuthzBoss authzBoss;
	@Resource
    private DashboardManager dashboardManager;
	
	private PropertiesFormNG pForm = new PropertiesFormNG();
    
	public String update() throws Exception {
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        String forwardStr = SUCCESS;

        String token = pForm.getToken();

        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String titleKey = PropertiesForm.TITLE;

        if (token != null) {
            resKey += token;
            numKey += token;
            titleKey += token;
        }

        if (pForm.isRemoveClicked()) {
            DashboardUtils.removeResources(pForm.getIds(), resKey, dashPrefs);
            forwardStr = "review";
            configurationProxy.setDashboardPreferences(session, user, dashPrefs);
        }

        String forward = checkSubmit(pForm);
		
        if (forward != null) {
            return forward;
        }

        Integer numberToShow = pForm.getNumberToShow();

        dashPrefs.setValue(numKey, numberToShow.toString());
        dashPrefs.setValue(titleKey, pForm.getTitle());
        
        ArrayList<String> resources = new ArrayList<String>();
		configurationProxy.setPreference(session, user,
				Constants.USERPREF_KEY_AVAILABITY_RESOURCES_NG, StringUtil
						.listToString(resources,
								StringConstants.DASHBOARD_DELIMITER));

		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in AvailabilitySummary/ModifyAction "
						+ " for " + user.getId() + " at "
						+ System.currentTimeMillis() + " user.prefs = "
						+ dashPrefs.getKeys().toString());

        configurationProxy.setDashboardPreferences(session, user, dashPrefs);

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
