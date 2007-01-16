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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;


/**
 * An Action that retrieves data from the BizApp to facilitate display
 * of the form to add users to AlertDefinition.
 *
 */
public class AddUsersFormPrepareAction extends AddNotificationsFormPrepareAction {
    private Log log = LogFactory.getLog( AddUsersFormPrepareAction.class.getName() );

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve this data and store it in the specified request
     * parameters:
     *
     * <ul>
     *   <li><code>AlertDefinitionValue</code> object identified by
     *     <code>Constants.ALERT_DEFS_ATTR</code></li>
     *   <li><code>List</code> of available <code>AuthzSubjectValue</code>
     *     objects (those not already associated with the alert def) in
     *     <code>Constants.AVAIL_USERS_ATTR</code></li>
     *   <li><code>Integer</code> number of available users in
     *     <code>Constants.NUM_AVAIL_USERS_ATTR</code></li>
     *   <li><code>List</code> of pending <code>AuthzSubjectValue</code>
     *     objects (those in queue to be associated with the definition) in
     *     <code>Constants.PENDING_USERS_ATTR</code></li>
     *   <li><code>Integer</code> number of pending users in
     *     <code>Constants.NUM_PENDING_USERS_ATTR</code></li>
     * </ul>
     */
    public ActionForward execute(ComponentContext context,
                                  ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception
    {
        AddUsersForm addForm = (AddUsersForm) form;
 
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);

        AppdefEntityID aeid;
        try {
            aeid = RequestUtils.getEntityTypeId(request);
            addForm.setAetid(aeid.getAppdefKey());
        } catch (ParameterNotFoundException e) {
            aeid = RequestUtils.getEntityId(request);
            addForm.setType(new Integer(aeid.getType()));
            addForm.setRid(aeid.getId());
        }        

        // pending users are those on the right side of the "add
        // to list" widget- awaiting association with the Alert
        // Definition when the form's "ok" button is clicked.
        Integer[] pendingUserIds =
            SessionUtils.getList(request.getSession(),
                                 Constants.PENDING_USERS_SES_ATTR);

        Integer[] userIds = getNotificationIds(request, addForm, aeid,
                                               EmailActionConfig.TYPE_USERS);

        PageControl pcp =
            RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");
        PageList pendingUsers =
            authzBoss.getSubjectsById(sessionId, pendingUserIds, pcp);

        // available users are all users in the system that are
        // _not_ associated with the definition and are not
        // pending
        PageControl pca =
            RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
        
        ArrayList excludes =
            new ArrayList(pendingUserIds.length + userIds.length);
        excludes.addAll(Arrays.asList(pendingUserIds));
        excludes.addAll(Arrays.asList(userIds));
        
        PageList availableUsers =
            authzBoss.getAllSubjects(sessionId, excludes, pca);

        request.setAttribute(Constants.PENDING_USERS_ATTR, pendingUsers);
        request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

        return null;
    }
    
}

// EOF
