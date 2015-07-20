package org.hyperic.hq.ui.action.portlet.criticalalerts;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForward;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("criticalAlertsModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements ModelDriven<PropertiesFormNG> {

	@Resource
    private ConfigurationProxy configurationProxy;
	@Resource
    private AuthzBoss authzBoss;
    @Resource
    private DashboardManager dashboardManager;
    
    PropertiesFormNG pForm = new PropertiesFormNG();
    
    
	public String update() throws Exception {
		
	       HttpSession session = request.getSession();
	        WebUser user = SessionUtils.getWebUser(session);

	        String forwardStr = SUCCESS;

	        String token = pForm.getToken();

	        // For multi-portlet configurations
	        String resKey = JsonLoadCriticalAlertsNG.RESOURCES_KEY;
	        String countKey = PropertiesFormNG.ALERT_NUMBER;
	        String priorityKey = PropertiesFormNG.PRIORITY;
	        String timeKey = PropertiesFormNG.PAST;
	        String selOrAllKey = PropertiesFormNG.SELECTED_OR_ALL;
	        String titleKey = PropertiesFormNG.TITLE;

	        if (token != null) {
	            resKey += token;
	            countKey += token;
	            priorityKey += token;
	            timeKey += token;
	            selOrAllKey += token;
	            titleKey += token;
	        }
	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        ConfigResponse dashPrefs = dashConfig.getConfig();

	        if (pForm.isRemoveClicked()) {
	            DashboardUtils.removeResources(pForm.getIds(), resKey, dashPrefs);
	            configurationProxy.setDashboardPreferences(session, user, dashPrefs);
	            forwardStr = "review";
	        }

	        String forward = checkSubmit(pForm);
			
	        if (forward != null) {
	            return forward;
	        }

	        Integer numberOfAlerts = pForm.getNumberOfAlerts();
	        String past = String.valueOf(pForm.getPast());
	        String prioritity = pForm.getPriority();
	        String selectedOrAll = pForm.getSelectedOrAll();

	        dashPrefs.setValue(countKey, numberOfAlerts.toString());
	        dashPrefs.setValue(timeKey, past);
	        dashPrefs.setValue(priorityKey, prioritity);
	        dashPrefs.setValue(selOrAllKey, selectedOrAll);
	        dashPrefs.setValue(titleKey, pForm.getTitle());

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
