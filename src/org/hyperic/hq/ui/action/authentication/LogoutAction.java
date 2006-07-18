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

package org.hyperic.hq.ui.action.authentication;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.Constants;

/**
 * An <code>Action</code> subclass that authenticates the web user's
 * credentials and establishes his identity.
 */

public class LogoutAction extends Action {

    // ---------------------------------------------------- Public Methods

    /**
     * log a user out of the system.
     */
    public ActionForward execute(ActionMapping mapping,
                            ActionForm form,
                            HttpServletRequest request,
                            HttpServletResponse response)
    throws Exception {
        Log log = LogFactory.getLog( LogoutAction.class.getName() );
       
        ServletContext ctx = getServlet().getServletContext();
        AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
        Integer sessionId =  RequestUtils.getSessionId(request);
        authBoss.logout(sessionId.intValue());

        HttpSession session = request.getSession();              

        session.removeAttribute( Constants.USER_PARAM );
        session.removeAttribute(Constants.WEBUSER_SES_ATTR);        
        session.invalidate();            

        return mapping.findForward("success");        
        
    }
}
