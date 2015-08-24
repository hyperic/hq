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
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component(value = "UserPasswordActionNG")
@Scope(value = "prototype")
public class UserPasswordActionNG extends BaseActionNG implements
		ModelDriven<UserNG> {

	private static final String PORTLET_LIST = ".admin.user.List";

	private static final String TITLE_NEW = "admin.user.NewUserTitle";

	private static final String PORTLET_NEW = ".admin.user.New";

	private static final String TITLE_CHANGE_PASSWORD = "admin.user.ChangeUserPasswordTitle";
	private static final String PORTLET_CHANGE_PASSWORD = ".admin.user.EditPassword";

	private static final String TITLE_REGISTER = "admin.user.RegisterUserTitle";

	private static final String PORTLET_REGISTER = ".admin.user.RegisterUser";

	protected final Log log = LogFactory.getLog(UserPasswordActionNG.class
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
	public String cancel() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_LIST);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		userId = RequestUtils.getUserId(getServletRequest()).toString();
		
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

		return "reset";
	}

	@SkipValidation
	public String startEdit() throws Exception {
		setUser();
		setHeaderResources();
		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_CHANGE_PASSWORD);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		userId = RequestUtils.getUserId(getServletRequest()).toString();
		return "startEditPassword";
	}

	public String edit() throws Exception {
		log.trace("Editing password for user.");

		if ( !UserAdminPortalActionNG.validatePasswordNoSpaces(user.getNewPassword()) ) {
			addFieldError("newPassword", getText("admin.user.changePassword.NoSpaces"));
			return INPUT;
		}
		
		String checkResult = checkSubmit(user);
		if (checkResult != null) {
			return checkResult;
		}
		userId = user.getId().toString();
		Integer sessionId = RequestUtils.getSessionId(getServletRequest());

		AuthzSubject subject = authzBoss.findSubjectById(
				RequestUtils.getSessionId(getServletRequest()), user.getId());

		log.trace("Editing user's password.");
		try {
			authBoss.authenticate(subject.getName(), user.getCurrentPassword());
		} catch (Exception e) {
			addFieldError("currentPassword",
					getText("admin.user.error.WrongPassword"));
			return "editPasswordFailed";
		}
		authBoss.changePassword(sessionId.intValue(), subject.getName(),
				user.getNewPassword());

		return "passwordEdited";
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
