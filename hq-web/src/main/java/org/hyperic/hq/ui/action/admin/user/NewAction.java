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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * An <code>WorkflowAction</code> subclass that creates a user
 * in the BizApp.
 */
public class NewAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

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
        Log log = LogFactory.getLog(NewAction.class.getName());

        Integer sessionId =  RequestUtils.getSessionId(request);
        NewForm userForm = (NewForm)form;

        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        //get the spiderSubjectValue of the user to be deleated.
        ServletContext ctx = getServlet().getServletContext();            
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);             
        AuthBoss authBoss = ContextUtils.getAuthBoss(ctx); 

        // add both a subject and a principal as normal
        log.trace("creating subject [" + userForm.getName() + "]");
        
        authzBoss.createSubject(sessionId, userForm.getName(),
                                userForm.getEnableLogin().equals("yes"),
                                HQConstants.ApplicationName,
                                userForm.getDepartment(),
                                userForm.getEmailAddress(),
                                userForm.getFirstName(),
                                userForm.getLastName(),
                                userForm.getPhoneNumber(),
                                userForm.getSmsAddress(),
                                userForm.isHtmlEmail());

        log.trace("adding user [" + userForm.getName() + "]");
        authBoss.addUser(sessionId.intValue(), userForm.getName(),
                         userForm.getNewPassword());

        log.trace("finding subject [" + userForm.getName() + "]");
        AuthzSubject newUser =
            authzBoss.findSubjectByName(sessionId, userForm.getName());

        HashMap parms = new HashMap(1);
        parms.put(Constants.USER_PARAM, newUser.getId());

        return returnOkAssign(request, mapping, parms, false);
    }
}
