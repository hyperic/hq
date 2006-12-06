package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.util.config.InvalidOptionException;
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
                                 PropertiesForm.RESOURCES,
                                 user);
            forwardStr = "review";
        }

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        String token = pForm.getToken();

        // For multi-portlet configuration
        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String resTypeKey = PropertiesForm.RES_TYPE;
        String metricKey = PropertiesForm.METRIC;
        if (token != null) {
            numKey += token;
            resKey += token;
            resTypeKey += token;
            metricKey += token;
        }

        Integer numberToShow = pForm.getNumberToShow();
        String resourceType = pForm.getResourceType();
        String metric = pForm.getMetric();

        // If the selected resource type does not match the previous value,
        // clear out the resources
        try {
            if (!resourceType.equals(user.getPreference(resTypeKey))) {
                user.setPreference(resKey, "");
            }
        } catch (InvalidOptionException e) {
            // Ok, not set yet..
        }

        user.setPreference(resTypeKey, resourceType);
        user.setPreference(numKey, numberToShow.toString());
        user.setPreference(metricKey, metric);

        boss.setUserPrefs(user.getSessionId(), user.getId(),
                          user.getPreferences());
        session.removeAttribute(Constants.USERS_SES_PORTAL);

        if (!pForm.isOkClicked()) {
            forwardStr="review";
        }

        return mapping.findForward(forwardStr);
    }
}
