package org.hyperic.hq.web.controllers;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;

public abstract class BaseController {
	private AppdefBoss appdefBoss;
	private AuthzBoss authzBoss;
	
	public BaseController(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
		this.appdefBoss = appdefBoss;
		this.authzBoss = authzBoss;
	}
	
	protected AuthzSubject getAuthzSubject(HttpSession session)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		WebUser webUser = getWebUser(session);
		
		return getAuthzBoss().findSubjectById(webUser.getSessionId(),
				webUser.getSubject().getId());
	}

	protected WebUser getWebUser(HttpSession session) {
		return (WebUser) session.getAttribute(SessionParameterKeys.WEB_USER);
	}

	protected AppdefBoss getAppdefBoss() {
		return appdefBoss;
	}

	protected AuthzBoss getAuthzBoss() {
		return authzBoss;
	}
}