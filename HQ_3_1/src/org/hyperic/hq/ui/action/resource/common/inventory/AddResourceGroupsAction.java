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

import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * A <code>BaseAction</code> that adds group memberships for a
 * resource.
 */
public class AddResourceGroupsAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Add users to the resource specified in the given
     * <code>AddResourceGroupsForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(AddResourceGroupsAction.class.getName());
        HttpSession session = request.getSession();

        AddResourceGroupsForm addForm = (AddResourceGroupsForm) form;
        AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(),
                                                 addForm.getRid());

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        switch (aeid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            forwardParams.put(Constants.ACCORDION_PARAM, "4");
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            forwardParams.put(Constants.ACCORDION_PARAM, "2");
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            forwardParams.put(Constants.ACCORDION_PARAM, "1");
            break;
        }

        try {
            ActionForward forward =
                checkSubmit(request, mapping, form, forwardParams);
            if (forward != null) {
                BaseValidatorForm spiderForm = (BaseValidatorForm) form;

                if (spiderForm.isCancelClicked() ||
                    spiderForm.isResetClicked()) {
                    log.trace("removing pending group list");
                    SessionUtils
                        .removeList(session,
                                    Constants.PENDING_RESGRPS_SES_ATTR);
                }
                else if (spiderForm.isAddClicked()) {
                    log.trace("adding to pending group list");
                    SessionUtils.addToList(session,
                                           Constants.PENDING_RESGRPS_SES_ATTR,
                                           addForm.getAvailableGroups());
                }
                else if (spiderForm.isRemoveClicked()) {
                    log.trace("removing from pending group list");
                    SessionUtils
                        .removeFromList(session,
                                        Constants.PENDING_RESGRPS_SES_ATTR,
                                        addForm.getPendingGroups());
                }

                return forward;
            }

            ServletContext ctx = getServlet().getServletContext();
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            Integer sessionId = RequestUtils.getSessionId(request);

            log.trace("getting pending group list");
            Integer[] pendingGroupIds =
                SessionUtils.getList(session,
                                     Constants.PENDING_RESGRPS_SES_ATTR);

            if (log.isTraceEnabled())
                log.trace("adding groups " + Arrays.asList(pendingGroupIds) +
                      " for resource [" + aeid + "]");
            boss.batchGroupAdd(sessionId.intValue(), aeid, pendingGroupIds);

            log.trace("removing pending group list");
            SessionUtils.removeList(session,
                                    Constants.PENDING_RESGRPS_SES_ATTR);

            RequestUtils.setConfirmation(request,
                                         "resource.common.inventory.confirm.AddResourceGroups");
            return returnSuccess(request, mapping, forwardParams);
        } 
        catch (AppSvcClustDuplicateAssignException e1) {
            RequestUtils
                .setError(request,
                          "resource.common.inventory.error.DuplicateClusterAssignment");
            return returnFailure(request, mapping);
        }

    }
}
