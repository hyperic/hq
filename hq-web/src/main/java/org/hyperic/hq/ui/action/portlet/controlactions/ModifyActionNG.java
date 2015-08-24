package org.hyperic.hq.ui.action.portlet.controlactions;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
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

@Component("controlActionsModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements
		ModelDriven<PropertiesFormNG> {

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

		String forward = checkSubmit(pForm);

		if (forward != null) {
			return forward;
		}

		String lastCompleted = pForm.getLastCompleted().toString();
		String mostFrequent = pForm.getMostFrequent().toString();
		String nextScheduled = pForm.getNextScheduled() == null ? "1" : pForm
				.getNextScheduled().toString();

		String useLastCompleted = String.valueOf(pForm.isUseLastCompleted());
		String useMostFrequent = String.valueOf(pForm.isUseMostFrequent());
		String useNextScheduled = String.valueOf(pForm.isUseNextScheduled());
		String past = String.valueOf(pForm.getPast());

		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);

		ConfigResponse dashPrefs = dashConfig.getConfig();

		dashPrefs.setValue(".ng.dashContent.controlActions.lastCompleted",
				lastCompleted);
		dashPrefs.setValue(".ng.dashContent.controlActions.mostFrequent",
				mostFrequent);
		dashPrefs.setValue(".ng.dashContent.controlActions.nextScheduled",
				nextScheduled);

		dashPrefs.setValue(".ng.dashContent.controlActions.useLastCompleted",
				useLastCompleted);
		dashPrefs.setValue(".ng.dashContent.controlActions.useMostFrequent",
				useMostFrequent);
		dashPrefs.setValue(".ng.dashContent.controlActions.useNextScheduled",
				useNextScheduled);
		dashPrefs.setValue(".ng.dashContent.controlActions.past", past);

		configurationProxy.setDashboardPreferences(session, user, dashPrefs);

		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in controlactions/ModifyAction "
						+ " for " + user.getId() + " at "
						+ System.currentTimeMillis() + " user.prefs = "
						+ dashPrefs.getKeys().toString());
		session.removeAttribute(Constants.USERS_SES_PORTAL);

		return SUCCESS;
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
