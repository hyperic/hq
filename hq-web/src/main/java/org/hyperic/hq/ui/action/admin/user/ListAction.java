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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An Action that retrieves all users from the BizApp.
 */
public class ListAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ListAction.class.getName());

    private AuthzBoss authzBoss;

    @Autowired
    public ListAction(AuthzBoss authzBoss) {
        super();
        this.authzBoss = authzBoss;
    }

    /**
     * Retrieve a <code>List</code> of all <code>AuthzSubjectValue</code>
     * objects and save it into the request attribute
     * <code>Constants.ALL_USERS_PARAM</code>.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer sessionId = RequestUtils.getSessionId(request);
        PageControl pc = RequestUtils.getPageControl(request);

        if (log.isTraceEnabled()) {
            log.trace("getting all subjects");
        }
        PageList<AuthzSubjectValue> users = authzBoss.getAllSubjects(sessionId, null, pc);
        request.setAttribute(Constants.ALL_USERS_ATTR, users);

        return null;
    }
}
