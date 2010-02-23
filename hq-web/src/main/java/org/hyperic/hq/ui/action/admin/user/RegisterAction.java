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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>WorkflowAction</code> subclass that registers a user
 * in the BizApp.
 */
public class RegisterAction extends BaseAction {
	private AuthzBoss authzBoss;
	private AuthBoss authBoss;
	private AuthzSubjectManager authzSubjectManager;
	private SessionManager sessionManager;
    
	@Autowired
	public RegisterAction(AuthzBoss authzBoss, AuthBoss authBoss, AuthzSubjectManager authzSubjectManager, SessionManager sessionManager) {
		super();
		
		this.authBoss = authBoss;
		this.authzBoss = authzBoss;
		this.authzSubjectManager = authzSubjectManager;
		this.sessionManager = sessionManager;
	}
	
    /**
     * Create the user with the attributes specified in the given
     * <code>NewForm</code> and save it into the session attribute
     * <code>Constants.USER_ATTR</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        final Log log = LogFactory.getLog(RegisterAction.class.getName());
        final boolean debug = log.isDebugEnabled();
        
        Integer sessionId =  RequestUtils.getSessionId(request);
        EditForm userForm = (EditForm)form;
        HttpSession session = request.getSession(false);
        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        //get the spiderSubjectValue of the user to be deleated.
        ServletContext ctx = getServlet().getServletContext();            
        WebUser webUser = RequestUtils.getWebUser(session);

        // password was saved off when the user logged in
        String password =
            (String) session.getAttribute(Constants.PASSWORD_SES_ATTR);
        
        session.removeAttribute(Constants.PASSWORD_SES_ATTR);

        // use the overlord to register the subject, and don't add
        // a principal
        if (debug) log.debug("registering subject [" + webUser.getUsername() + "]");
        
        Integer authzSubjectId = userForm.getId();
        AuthzSubject target = authzSubjectManager.findSubjectById(authzSubjectId); 
        
        authzBoss.updateSubject(sessionId, target, Boolean.TRUE,
                                HQConstants.ApplicationName,
                                userForm.getDepartment(),
                                userForm.getEmailAddress(),
                                userForm.getFirstName(),
                                userForm.getLastName(),
                                userForm.getPhoneNumber(),
                                userForm.getSmsAddress(), null);
                                

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

        return returnSuccess(request, mapping, parms, false);
    }
}
