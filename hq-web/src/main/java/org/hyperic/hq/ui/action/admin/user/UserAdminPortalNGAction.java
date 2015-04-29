package org.hyperic.hq.ui.action.admin.user;

import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.SessionAware;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

@Component(value = "userAdminPortalNGAction")
public class UserAdminPortalNGAction extends BaseActionNG {

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

	protected final Log log = LogFactory.getLog(UserAdminPortalNGAction.class
			.getName());

	@Resource
	private AuthBoss authBoss;

	@Resource
	private AuthzBoss authzBoss;

	private Map<String, Object> userSession;

	private HttpServletRequest request;

	public String view() throws Exception {

		setUser();
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
		portal.setWorkflowPortal(true);
		// ActionContext.getContext().getParameters().put(Constants.PORTAL_KEY,
		// portal);

		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "displayUser";
	}

	public String create() throws Exception {
		
		
		setHeaderResources();
		
		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "newUserForm";
	}

	public String execute() throws Exception {
		
		return null;
	}

}
