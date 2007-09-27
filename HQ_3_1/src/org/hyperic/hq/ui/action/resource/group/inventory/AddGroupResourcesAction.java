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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An Action that adds Resources to a Group in the BizApp. This is first
 * created with AddGroupResourcesFormPrepareAction, which creates the list
 * of pending Resources to add to the group.
 *
 * Heavily based on:
 * @see org.hyperic.hq.ui.action.resource.group.inventory.AddGroupResourcesFormPrepareAction
 */
public class AddGroupResourcesAction extends BaseAction {

    /**
     * Add roles to the user specified in the given
     * <code>AddGroupResourcesForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(AddGroupResourcesAction.class.getName());    
        HttpSession session = request.getSession();

        AddGroupResourcesForm addForm = (AddGroupResourcesForm) form;
        AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(),
                                                 addForm.getRid());

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        forwardParams.put(Constants.ACCORDION_PARAM, "1");
        
        try {
            ActionForward forward = checkSubmit(request, mapping, form,
                                                    forwardParams);
            if (forward != null) {
                BaseValidatorForm spiderForm = (BaseValidatorForm) form;

                if (spiderForm.isCancelClicked() ||
                    spiderForm.isResetClicked()) {
                    log.trace("removing pending/removed resources list");
                    SessionUtils
                        .removeList(session,
                                    Constants.PENDING_RESOURCES_SES_ATTR);
                } else if (spiderForm.isAddClicked()) {
                    log.trace("adding to pending resources list");
                    SessionUtils.addToList(session,
                                           Constants.PENDING_RESOURCES_SES_ATTR,
                                           addForm.getAvailableResources());
                } else if (spiderForm.isRemoveClicked()) {
                    log.trace(
                                       "removing from pending resources list");
                    SessionUtils
                        .removeFromList(session,
                                        Constants.PENDING_RESOURCES_SES_ATTR,
                                        addForm.getPendingResources());
                }
                return forward;
            }

            ServletContext ctx = getServlet().getServletContext();
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            Integer sessionId = RequestUtils.getSessionId(request);

            log.trace("getting pending resource list");
            List pendingResourceIds =
                SessionUtils.getListAsListStr(request.getSession(),
                                     Constants.PENDING_RESOURCES_SES_ATTR);
            
            if (pendingResourceIds.size() == 0)
                return returnSuccess(request, mapping, forwardParams);
                            
            log.trace("getting group [" + aeid.getID() + "]");
            AppdefGroupValue group = boss.findGroup(sessionId.intValue(),
                                                    aeid.getId());

            BizappUtils.addResourcesToGroup(group, pendingResourceIds);

            boss.saveGroup(sessionId.intValue(), group);

            log.trace("removing pending user list");
            SessionUtils.removeList(session,
                                    Constants.PENDING_RESOURCES_SES_ATTR);

            RequestUtils.setConfirmation(request,
                                         "resource.group.inventory.confirm.AddResources");
                                         
            return returnSuccess(request, mapping, forwardParams);
            
        } 
        catch (AppSvcClustDuplicateAssignException e1) {
            log.debug("group update failed:", e1);
            RequestUtils
                .setError(request,
                          Constants.ERR_DUP_CLUSTER_ASSIGNMENT);
            return returnFailure(request, mapping);
        }
        catch (AppdefGroupNotFoundException e) {
            RequestUtils
                .setError(request,
                          "resource.common.inventory.error.ResourceNotFound");
                     
            return returnFailure(request, mapping, forwardParams);
        } 
        catch (GroupVisitorException e) {
            log.error("Unable to add resources to group", e);
            throw e;
        }
    }
}
