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

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An Action that changes the owner of a resource in the inventory.
 */
public class ChangeResourceOwnerAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Change the owner of the resource to the user specified in the the
     * given <code>ChangeResourceOwnerForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        Log log =
            LogFactory.getLog(ChangeResourceOwnerAction.class.getName());    

        ChangeResourceOwnerForm chownForm = (ChangeResourceOwnerForm) form;
        Integer resourceId = chownForm.getRid();
        Integer resourceType = chownForm.getType();
        Integer ownerId = chownForm.getOwner();

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

        ActionForward forward =
            checkSubmit(request, mapping, form, forwardParams, YES_RETURN_PATH);
        if (forward != null) {
            return forward;
        }

        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("getting subject [" + ownerId + "]");
        AuthzSubjectValue owner = ContextUtils.getAuthzBoss(ctx)
            .findSubject(sessionId, ownerId);

        AppdefEntityID entityId =
            new AppdefEntityID(resourceType.intValue(),
                               resourceId.intValue());
        log.trace("setting owner [" + ownerId + "] for resource [" +
                  entityId + "]");
        ContextUtils.getAppdefBoss(ctx)
            .changeResourceOwner(sessionId.intValue(), entityId, owner);

        RequestUtils.setConfirmation(request,
                                     "resource.common.inventory.confirm.ChangeResourceOwner");
        // fix for 5265. Check if we've lost viewability of the 
        // resource. If we have, then return to the resource hub
        try {
            ContextUtils.getAppdefBoss(ctx)
                .findById(sessionId.intValue(), entityId);                                         
        } catch (PermissionException e) {
            // looks like we cant see the thing anymore...
            // you, sir, are going to the resource hub
            return mapping.findForward("lostRights");
        }
        return returnSuccess(request, mapping, forwardParams, YES_RETURN_PATH);            
        
    }
}
