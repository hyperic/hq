/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.admin.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("registerUserActionNG")
@Scope("prototype")
public class RegisterActionNG extends BaseActionNG implements ModelDriven<UserNG> {

	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private AuthBoss authBoss;
	@Resource
	private AuthzSubjectManager authzSubjectManager;
	@Resource
	private SessionManager sessionManager;
	
	private final Log log = LogFactory.getLog(RegisterActionNG.class.getName());
		
	private UserNG user = new UserNG();

	/**
     * Create the user with the attributes specified in the given
     * <code>NewForm</code> and save it into the session attribute
     * <code>Constants.USER_ATTR</code>.
     */
	public String save() throws Exception {

        this.request = getServletRequest();
        
		final boolean debug = log.isDebugEnabled();
		setHeaderResources();

		String checkResult = checkSubmit(user);
		if (checkResult != null) {
			request.setAttribute(Constants.USER_ATTR, user);
			return checkResult;
		}

		Integer sessionId = RequestUtils.getSessionId(getServletRequest());
        
        HttpSession session = request.getSession(false);

        WebUser webUser = RequestUtils.getWebUser(session);

        // password was saved off when the user logged in
        session.removeAttribute(Constants.PASSWORD_SES_ATTR);

        //get the spiderSubjectValue of the user to be deleted.
		ServletContext ctx = ServletActionContext.getServletContext();

        // use the overlord to register the subject, and don't add
        // a principal
        if (debug) log.debug("registering subject [" + webUser.getUsername() + "]");
        
        Integer authzSubjectId = user.getId();
        AuthzSubject target = authzSubjectManager.findSubjectById(authzSubjectId); 
        
        authzBoss.updateSubject(sessionId, target, Boolean.TRUE,
                                HQConstants.ApplicationName,
                                user.getDepartment(),
                                user.getEmailAddress(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getPhoneNumber(),
                                user.getSmsAddress(), null);
                                

        // nuke the temporary bizapp session and establish a new
        // one for this subject.. must be done before pulling the
        // new subject in order to do it with his own credentials
        // TODO need to make sure this is valid
        sessionManager.invalidate(sessionId);
        
        sessionId = sessionManager.put(authzSubjectManager.findSubjectById(authzSubjectId));
        
        if (debug) log.debug("finding subject [" + webUser.getUsername() + "]");

        // the new user has no prefs, but we still want to pick up
        // the defaults
        ConfigResponse preferences = 
            (ConfigResponse)ctx.getAttribute(Constants.DEF_USER_PREFS);

        // look up the user's permissions
        if (debug) log.debug("getting all operations");
        
        Map<String, Boolean> userOpsMap = new HashMap<String, Boolean>();
        List<Operation> userOps = authzBoss.getAllOperations(sessionId);
        
        // TODO come back to this and see why this is done...
        for (Operation op : userOps) {
            userOpsMap.put(op.getName(), Boolean.TRUE);
        }

        // we also need to create up a new web user
        webUser = new WebUser(target, sessionId, preferences, false);
        
        session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
        session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOpsMap);

        Map<String, Object> parms = new HashMap<String, Object>(1);
        
        parms.put(Constants.USER_PARAM, target.getId());

        return SUCCESS;
    }

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		user.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	@Override
	public UserNG getModel() {
		return user;
	}

	public UserNG getUser() {
		return user;
	}

	public void setUser(UserNG user) {
		this.user = user;
	}
	
	public Integer getUserId () {
		return user.getId();
	}
	
}
