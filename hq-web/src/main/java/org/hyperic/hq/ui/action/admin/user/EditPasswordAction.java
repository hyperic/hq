/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>BaseAction</code> subclass that edit's a user's passworrd in the
 * BizApp.
 */
public class EditPasswordAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(NewAction.class.getName());
    private final AuthBoss authBoss;
    private final AuthzBoss authzBoss;
    

    @Autowired
    public EditPasswordAction(AuthBoss authBoss, AuthzBoss authzBoss) {
        super();
        this.authBoss = authBoss;
        this.authzBoss = authzBoss;
    }

    /**
     * Edit the user's password. Make sure that the <code>currentPassword</code>
     * is valid.
     */
    @Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        log.trace("Editing password for user.");

        EditPasswordForm pForm = (EditPasswordForm) form;
        ActionForward forward = checkSubmit(request, mapping, form, Constants.USER_PARAM, pForm.getId());

        if (forward != null) {
            return forward;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        AuthzSubject user = authzBoss.findSubjectById(RequestUtils.getSessionId(request), pForm.getId());

        log.trace("Editing user's password.");

        boolean admin = false;

        for (Operation operation : authzBoss.getAllOperations(sessionId)) {
            if (admin = AuthzConstants.subjectOpModifySubject.equals(operation.getName())) {
                break;
            }
        }

        String [] vals = request.getParameterValues("u");
        String val = null;
        if (vals != null && vals.length > 0)
        	val = vals[0];
   	    // if the admin user is changing her own pw, require authentication of current pw 
		if (val != null)
			admin = admin & String.valueOf(user.getId()).equals(val) == false;

        if (!admin) {
            try {
                authBoss.authenticate(user.getName(), pForm.getCurrentPassword());
            } catch (Exception e) {
                RequestUtils.setError(request, "admin.user.error.WrongPassword", "currentPassword");
                return returnFailure(request, mapping, Constants.USER_PARAM, pForm.getId());
            }
        }
        authBoss.changePassword(sessionId.intValue(), user.getName(), pForm.getNewPassword());

        return returnSuccess(request, mapping, Constants.USER_PARAM,

        pForm.getId());

    }
}
