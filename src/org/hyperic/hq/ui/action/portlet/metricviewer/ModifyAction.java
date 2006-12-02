package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

public class ModifyAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        PropertiesForm pForm = (PropertiesForm) form;
        HttpSession session = request.getSession();
        WebUser user = (WebUser)
            session.getAttribute(Constants.WEBUSER_SES_ATTR);

        String forwardStr = Constants.SUCCESS_URL;

        if(pForm.isRemoveClicked()){
            DashboardUtils
                .removeResources(pForm.getIds(),
                                 ".dashContent.metricviewer.resources",
                                 user);
            forwardStr = "review";
        }

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        Integer numberToShow = pForm.getNumberToShow();

        user.setPreference(PropertiesForm.NUM_TO_SHOW,
                           numberToShow.toString());

        String resourceType = pForm.getResourceType();
        user.setPreference(PropertiesForm.RES_TYPE, resourceType);

        String metric = pForm.getMetric();
        user.setPreference(PropertiesForm.METRIC, metric);

        boss.setUserPrefs(user.getSessionId(), user.getId(),
                          user.getPreferences());

        session.removeAttribute(Constants.USERS_SES_PORTAL);

        if (!pForm.isOkClicked()) {
            forwardStr="review";
        }

        return mapping.findForward(forwardStr);
    }
}
