package org.hyperic.hq.ui.action.admin.user;

import java.util.Map;

import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ModelDriven;

@Component(value = "userAdminPortalActionNG")
@Scope(value = "prototype")
public class UserAdminPortalActionNG extends BaseActionNG implements
		ModelDriven<UserNG> {

	protected final Log log = LogFactory.getLog(UserAdminPortalActionNG.class
			.getName());

	@Resource
	private AuthBoss authBoss;

	@Resource
	private AuthzBoss authzBoss;

	private UserNG user = new UserNG();

	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@SkipValidation
	public String view() throws Exception {

		setUser();
		setHeaderResources();

		return "displayUser";
	}

	@SkipValidation
	public String startNew() throws Exception {
		setHeaderResources();

		return "newUserForm";
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();

		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

	user.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	public String create() throws Exception {

		setHeaderResources();
		
		if ( !validatePasswordNoSpaces(user.getNewPassword()) ) {
			addFieldError("newPassword", getText("admin.user.changePassword.NoSpaces"));
			return INPUT;
		}
		String checkResult = checkSubmit(user);
		if (checkResult != null) {
			return checkResult;
		}

		String userName = user.getName();
		// add both a subject and a principal as normal
		log.trace("creating subject [" + userName + "]");

		Integer sessionId = RequestUtils.getSessionId(getServletRequest());
		
		AuthzSubject checkUser = authzBoss.findSubjectByName(sessionId, userName );
		
		if (checkUser != null) {
			log.error("User name '" + userName + "' already exists");
			String msg = getText("exception.user.alreadyExists");
            this.addFieldError("name", msg);
			return INPUT;
		}
		
		authzBoss.createSubject(sessionId, user.getName(), "yes".equals(user.getEnableLogin()), 
				HQConstants.ApplicationName, user.getDepartment(), user.getEmailAddress(),
				user.getFirstName(), user.getLastName(), user.getPhoneNumber(), 
				user.getSmsAddress(), user.isHtmlEmail());
		
		

		log.trace("adding user [" + userName + "]");
		authBoss.addUser(sessionId.intValue(), userName, user.getNewPassword());

		log.trace("finding subject [" + userName + "]");
		AuthzSubject newUser = authzBoss.findSubjectByName(sessionId, userName);

		getServletRequest().setAttribute(Constants.USER_PARAM, newUser.getId());
		ActionContext.getContext().put(Constants.USER_PARAM, newUser.getId());

		userId = newUser.getId().toString();

		return "showCreated";
	}

	@SkipValidation
	public String list() throws Exception {
		setHeaderResources();
		Integer sessionId = RequestUtils.getSessionId(getServletRequest());
		PageControl pc = RequestUtils.getPageControl(getServletRequest());

		if (log.isTraceEnabled()) {
			log.trace("getting all subjects");
		}
		PageList<AuthzSubjectValue> users = authzBoss.getAllSubjects(sessionId,
				null, pc);
		getServletRequest().setAttribute(Constants.ALL_USERS_ATTR, users);
		ActionContext.getContext().put(Constants.ALL_USERS_ATTR, users);

		return "listUsers";

	}

	@SkipValidation
	public String register() throws Exception {

		setHeaderResources();

		return "registerUser";
	}
	
	@SuppressWarnings("unchecked")
	public int getTotalSize() {
		return ((PageList<AuthzSubjectValue>) ActionContext.getContext().get(
				Constants.ALL_USERS_ATTR)).getTotalSize();
	}

	public Map<Integer, String> getPaggingList() {
		return getPaggingList(getTotalSize());
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
	
	public static boolean validatePasswordNoSpaces(String password){
		return !password.contains(" ");
	}

}
