package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;

public class SetDashboardAction extends org.hyperic.hq.ui.action.BaseAction {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		DashboardForm dForm = (DashboardForm) form;
		WebUser user = (WebUser) request.getSession().getAttribute(
				Constants.WEBUSER_SES_ATTR);
		AuthzBoss authzBoss = ContextUtils.getAuthzBoss(request.getSession()
				.getServletContext());
		boolean persistPrefs = false;
		if (dForm.getSelectedDashboardId() != null) {
			//assign a selected dashboard
			user.setPreference(Constants.SELECTED_DASHBOARD_ID, dForm
					.getSelectedDashboardId());
			persistPrefs = true;
		}
		if (dForm.getDefaultDashboard() != null
			//assign a new default dashboard
				&& user.getPreference(Constants.DEFAULT_DASHBOARD_ID, null) == null) {
			user.setPreference(Constants.DEFAULT_DASHBOARD_ID, dForm
					.getDefaultDashboard());
			persistPrefs = true;
		}
		if (persistPrefs) {
			authzBoss.setUserPrefs(user.getSessionId(), user.getSubject()
					.getId(), user.getPreferences());
		}
		return mapping.findForward(Constants.SUCCESS_URL);
	}

}
