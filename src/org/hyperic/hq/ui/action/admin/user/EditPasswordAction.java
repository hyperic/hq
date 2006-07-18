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
        
import java.io.IOException;
import java.util.Iterator;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.ui.action.BaseAction;


import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

/**
 * An <code>BaseAction</code> subclass that edit's a user's
 * passworrd in the BizApp.
 */
public class EditPasswordAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /**
     *  Edit the user's password. Make sure that the
     *  <code>currentPassword</code> is valid.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        
        Log log = LogFactory.getLog(NewAction.class.getName());
        log.trace("Editing password for user." );                    
    
        EditPasswordForm pForm = (EditPasswordForm)form;
        ActionForward forward = checkSubmit(request, mapping, form,
                                            Constants.USER_PARAM, 
                                            pForm.getId() );

        if (forward != null) {
            return forward;
        }         

        //get the spiderSubjectValue of the user to be deleated.
        ServletContext ctx = getServlet().getServletContext();            
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);

        AuthzSubjectValue user 
            = authzBoss.findSubject(RequestUtils.getSessionId(request), pForm.getId());                                                           

        log.trace("Editing user's password.");                                                      

        boolean admin = false;                

        for(Iterator i = authzBoss.getAllOperations( sessionId ).iterator();
            i.hasNext();){
            OperationValue operation = (OperationValue) i.next();            
            if( AuthzConstants.subjectOpModifySubject.equals(operation.getName()) ){
                admin = true;
                break;
            }            
        }

        if(!admin){
            try {
                authBoss.login(user.getName(), pForm.getCurrentPassword());
            } catch ( LoginException e ){
                RequestUtils.setError(request, "admin.user.error.WrongPassword", "currentPassword");
                return returnFailure(request, mapping, Constants.USER_PARAM, pForm.getId());
            }
        }
        authBoss.changePassword(sessionId.intValue(), user.getName(), 
                                    pForm.getNewPassword());

        return returnSuccess(request, mapping, Constants.USER_PARAM,

        pForm.getId());

    } 
}
