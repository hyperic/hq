package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class SetDefaultDashboardAction
    extends BaseAction {
    private AuthzBoss authzBoss;

    @Autowired
    public SetDefaultDashboardAction(AuthzBoss authzBoss) {
        super();
        this.authzBoss = authzBoss;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        DashboardForm dForm = (DashboardForm) form;
        WebUser user = RequestUtils.getWebUser(session);
        String currentDefaultDashboardId = user.getPreference(Constants.DEFAULT_DASHBOARD_ID, null);
        String submittedDefaultDashboardId = dForm.getDefaultDashboard();

        // Compare the incoming default dashboard id with the one we had in our
        // user preferences
        // If they aren't equal it means the user is changing it, so update
        if (!submittedDefaultDashboardId.equals(currentDefaultDashboardId)) {
            user.setPreference(Constants.DEFAULT_DASHBOARD_ID, dForm.getDefaultDashboard());
            session.setAttribute(Constants.SELECTED_DASHBOARD_ID, new Integer(dForm.getDefaultDashboard()));
            authzBoss.setUserPrefs(user.getSessionId(), user.getSubject().getId(), user.getPreferences());
        }

        return mapping.findForward(Constants.AJAX_URL);
    }
}
