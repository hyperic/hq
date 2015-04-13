package org.hyperic.hq.ui.action.admin.user;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts2.ServletActionContext;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class UserAdminPortalNGAction extends ActionSupport {

    private static final String TITLE_LIST = "admin.user.ListUsersTitle";

    private static final String PORTLET_LIST = ".admin.user.List";

    private static final String TITLE_ADD_ROLES = "admin.user.AddUserRolesTitle";

    private static final String PORTLET_ADD_ROLES = ".admin.user.UserRoles";

    private static final String TITLE_EDIT = "admin.user.EditUserTitle";

    private static final String PORTLET_EDIT = ".admin.user.Edit";

    private static final String TITLE_NEW = "admin.user.NewUserTitle";

    private static final String PORTLET_NEW = ".admin.user.New";

    private static final String TITLE_VIEW = "admin.user.ViewUserTitle";

    private static final String PORTLET_VIEW = ".admin.user.View";

    private static final String TITLE_CHANGE_PASSWORD = "admin.user.ChangeUserPasswordTitle";
    private static final String PORTLET_CHANGE_PASSWORD = ".admin.user.EditPassword";

    private static final String TITLE_REGISTER = "admin.user.RegisterUserTitle";

    private static final String PORTLET_REGISTER = ".admin.user.RegisterUser";

    protected final Log log = LogFactory.getLog(UserAdminPortalAction.class.getName());
    
    private AuthBoss authBoss;

    private AuthzBoss authzBoss;
    
    public String viewUser() throws Exception {
    	
		setUser();
		// setReturnPath(request, mapping, Constants.MODE_VIEW);
		
		Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
		portal.setWorkflowPortal(true);
		ActionContext.getContext().getParameters().put(Constants.PORTAL_KEY, portal);
		
		return null;
    }



	public String execute() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	
    /**
     * Set the user for the current action.
     * 
     * @param request The request to get the session to store the returnPath
     *        into.
     * 
     */
    protected void setUser() throws Exception {
    	Map<String,Object> parameters = ActionContext.getContext().getParameters();
        Integer userId = RequestUtils.getUserId(parameters);
        Integer sessionId = Integer.valueOf(ServletActionContext.getRequest().getSession().getId());

        if (log.isTraceEnabled()) {
            log.trace("finding user [" + userId + "]");
        }
        AuthzSubject user = authzBoss.findSubjectById(sessionId, userId);

        // when CAM is in LDAP mode, we may still have
        // users logging in with JDBC. the only way we can
        // distinguish these users is by checking to see
        // if they have an entry in the principals table.
        WebUser webUser = new WebUser(user.getAuthzSubjectValue());
        boolean hasPrincipal = authBoss.isUser(sessionId.intValue(), user.getName());
        webUser.setHasPrincipal(hasPrincipal);

        parameters.put(Constants.USER_ATTR, webUser);
        parameters.put(Constants.TITLE_PARAM_ATTR, BizappUtils.makeSubjectFullName(user));
    }
    
    /**
     * Set the return path for the current action, including the mode and (if
     * necessary) user id request parameters.
     * 
     * @param request The request to get the session to store the return path
     *        into.
     * @param mapping The ActionMapping to get the return path from.
     * @param mode The name of the current display mode.
     * 
     */
    /*
    protected void setReturnPath(String mode) throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.MODE_PARAM, mode);
        try {
            params.put(Constants.USER_PARAM, RequestUtils.getUserId(ActionContext.getContext().getParameters()));
        } catch (ParameterNotFoundException e) {
            ; // not in a specific user's context
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }
        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
    */
}
