package org.hyperic.hq.ui.action.admin.user;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
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

	protected final Log log = LogFactory.getLog(UserAdminPortalActionNG.class
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
	public String view() throws Exception {

		setUser();
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
		portal.setWorkflowPortal(true);

		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "displayUser";
	}

	@SkipValidation
	public String startNew() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "newUserForm";
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_LIST);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		user.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	public String create() throws Exception {

		setHeaderResources();

		String checkResult = checkSubmit(user);
		if (checkResult != null) {
			return checkResult;
		}

		// add both a subject and a principal as normal
		log.trace("creating subject [" + user.getName() + "]");

		Integer sessionId = RequestUtils.getSessionId(getServletRequest());
		
		AuthzSubject checkUser = authzBoss.findSubjectByName(sessionId,
				user.getName() );
		
		if (checkUser != null) {
			String msg = getText("exception.user.alreadyExists");
			this.addCustomActionErrorMessages(msg);
			log.error("User name " + user.getName() + "Already exists");
			return INPUT;
		}
		
		authzBoss.createSubject(sessionId, user.getName() , "yes".equals(user
				.getEnableLogin()), HQConstants.ApplicationName,
				user.getDepartment(), user.getEmailAddress(),
				user.getFirstName() , user.getLastName() , user
						.getPhoneNumber(), user.getSmsAddress(), user
						.isHtmlEmail());
		
		

		log.trace("adding user [" + user.getName() + "]");
		authBoss.addUser(sessionId.intValue(), user.getName() ,
				user.getNewPassword());

		log.trace("finding subject [" + user.getName() + "]");
		AuthzSubject newUser = authzBoss.findSubjectByName(sessionId,
				user.getName() );

		getServletRequest().setAttribute(Constants.USER_PARAM, newUser.getId());

		ActionContext.getContext().put(Constants.USER_PARAM, newUser.getId());

		Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		userId = newUser.getId().toString();

		return "showCreated";
	}

	@SkipValidation
	public String list() throws Exception {
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

		setUser();
		setHeaderResources();

        Portal portal = Portal.createPortal(TITLE_REGISTER, PORTLET_REGISTER);
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);		
		
		return "registerUser";
	}
	
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

}
