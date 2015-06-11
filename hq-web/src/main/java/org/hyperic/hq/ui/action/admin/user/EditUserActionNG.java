package org.hyperic.hq.ui.action.admin.user;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ModelDriven;

@Component(value = "editUserActionNG")
@Scope(value = "prototype")
public class EditUserActionNG extends BaseActionNG implements
		ModelDriven<UserNG> {

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

	protected final Log log = LogFactory.getLog(EditUserActionNG.class
			.getName());

	@Resource
	private AuthBoss authBoss;

	@Resource
	private AuthzBoss authzBoss;

	private Map<String, Object> userSession;

	private UserNG user = new UserNG();

	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@SkipValidation
	public String load() throws Exception {

		setUser();
		setHeaderResources();

		WebUser webUser = (WebUser) getServletRequest().getAttribute(
				Constants.USER_ATTR);

		if (user.getId() == null) {
			user.setId(webUser.getId());
		}
		if (user.getDepartment() == null) {
			user.setDepartment(webUser.getDepartment());
		}
		if (user.getFirstName() == null) {
			user.setFirstName(webUser.getFirstName());
		}
		if (user.getLastName()== null) {
			user.setLastName(webUser.getLastName());
		}
		if (user.getEmailAddress() == null) {
			user.setEmailAddress(webUser.getEmailAddress());
		}
		if (user.getPhoneNumber() == null) {
			user.setPhoneNumber(webUser.getPhoneNumber());
		}
		if (user.getSmsAddress() == null) {
			user.setSmsAddress(webUser.getSmsaddress());
		}

		user.setHtmlEmail(webUser.isHtmlEmail());
		if (webUser.getActive()) {
			user.setEnableLogin("yes");
		} else {
			user.setEnableLogin("no");
		}
		Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_EDIT);
		portal.setWorkflowPortal(true);

		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
		userId = user.getId().toString();
		return "editUserForm";
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_LIST);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
		
		userId = RequestUtils.getUserId(getServletRequest()).toString();
		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		userId = RequestUtils.getUserId(getServletRequest()).toString();
		
		user.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	public String save() throws Exception {

		setHeaderResources();

		String checkResult = checkSubmit(user);
		if (checkResult != null) {
			request.setAttribute(Constants.USER_ATTR, user);
			return checkResult;
		}

		Integer sessionId = RequestUtils.getSessionId(getServletRequest());

		AuthzSubject subject = authzBoss.findSubjectById(
				RequestUtils.getSessionId(getServletRequest()), user.getId());

		authzBoss.updateSubject(sessionId, subject, new Boolean(user
				.getEnableLogin().equals("yes")), null, user.getDepartment(),
				user.getEmailAddress(), user.getFirstName(),
				user.getLastName(), user.getPhoneNumber(),
				user.getSmsAddress(), new Boolean(user.isHtmlEmail()));
		getServletRequest().setAttribute(Constants.USER_PARAM, user.getId());

		ActionContext.getContext().put(Constants.USER_PARAM, user.getId());

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_EDIT);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		userId = user.getId().toString();

		return "showEdited";
	}

	public String execute() throws Exception {

		return null;
	}

	public UserNG getUser() {
		return user;
	}

	public void setUser(UserNG user) {
		this.user = user;
	}

	public UserNG getModel() {
		return user;
	}

}
