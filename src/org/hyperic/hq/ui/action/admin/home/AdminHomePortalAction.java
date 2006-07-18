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

package org.hyperic.hq.ui.action.admin.home;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An <code>Action</code> that sets up the Admin Home portal
 */
public class AdminHomePortalAction extends BaseAction {

    private static final String TITLE_HOME = "user.admin.page.title";
    private static final String PORTLET_HOME   = ".admin.home";

    protected static Log log =
        LogFactory.getLog(AdminHomePortalAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /** Set up the Admin Home portal.
     *  
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(AdminHomePortalAction.class.getName());

        Portal portal = Portal.createPortal(TITLE_HOME, PORTLET_HOME);
        request.setAttribute( Constants.PORTAL_KEY, portal );               

        String returnPath = ActionUtils.findReturnPath(mapping, null);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return null;
    }
}
