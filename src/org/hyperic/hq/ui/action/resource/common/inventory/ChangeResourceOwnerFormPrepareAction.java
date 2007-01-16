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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

/**
 * An Action that retrieves a resource and a list of subjects from the
 * BizApp to facility display of the <em>Change Resource Owner</em>
 * page.
 */
public class ChangeResourceOwnerFormPrepareAction 
    extends WorkflowPrepareAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve the full <code>List</code> of
     * <code>AuthzSubjectValue</code> objects representing all users
     * in the database excluding the owner of the resource identified by
     * the request parameters <code>Constants.RESOURCE_PARAM</code>
     * and <code>Constants.RESOURCE_TYPE_ID_PARAM</code> and store
     * that list in in the <code>Constants.ALL_USERS_ATTR</code> request
     * attribute. Also store the <code>AppdefResourceValue</code>
     * itself in the <code>Constants.RESOURCE_ATTR</code> request
     * attribute.
     */
    public ActionForward workflow(ComponentContext context,
                                  ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        Log log = LogFactory
            .getLog(ChangeResourceOwnerFormPrepareAction.class.getName());

        ChangeResourceOwnerForm changeForm = (ChangeResourceOwnerForm) form;
        Integer resourceId = changeForm.getRid();
        Integer resourceType = changeForm.getType();

        if (resourceId == null) {
            resourceId = RequestUtils.getResourceId(request);
        }
        if (resourceType == null) {
            resourceType = RequestUtils.getResourceTypeId(request);
        }

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }
        changeForm.setRid(resource.getId());
        changeForm.setType(new Integer(resource.getEntityId().getType()));

        AuthzSubjectValue resourceOwner = (AuthzSubjectValue)
            request.getAttribute(Constants.RESOURCE_OWNER_ATTR);
        if (resourceOwner == null) {
            RequestUtils.setError(request,
                                  "resource.common.inventory.error.ResourceOwnerNotFound");
            return null;
        }

        Integer sessionId = RequestUtils.getSessionId(request);
        PageControl pc = RequestUtils.getPageControl(request);
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);

        log.trace("getting all users");
        PageList allUsers = boss.getAllSubjects(sessionId, null, pc);

        // remove the resource's owner from the list of users
        ArrayList owner = new ArrayList();
        owner.add(resourceOwner);
        List users = BizappUtils.grepSubjects(allUsers, owner);

        request.setAttribute(Constants.ALL_USERS_ATTR, users);
        request.setAttribute(Constants.NUM_USERS_ATTR,
        new Integer( allUsers.getTotalSize() -1 ));

        
        return null;
    }
}
