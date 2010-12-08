/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.web;

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