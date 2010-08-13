package org.hyperic.hq.web.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;

/**
 * This abstract class provides base functionality used indirectly (via
 * other base controllers) by all controllers in the system.
 * 
 * @author David Crutchfield
 * 
 */
public abstract class BaseController {
	protected final Map<String, String> EMPTY_RESPONSE;
	protected final Map<String, Boolean> ERROR_RESPONSE;
	
	private AppdefBoss appdefBoss;
	private AuthzBoss authzBoss;
	
	public BaseController(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
		EMPTY_RESPONSE = new HashMap<String, String>();
		ERROR_RESPONSE = new HashMap<String, Boolean>();
		
		// TODO mimics the existing error response, need to make this more robust going forward...
		ERROR_RESPONSE.put("error", true);
		
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