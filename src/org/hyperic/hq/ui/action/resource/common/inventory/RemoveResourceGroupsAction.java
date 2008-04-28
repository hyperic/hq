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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 *
 */
public class RemoveResourceGroupsAction extends BaseAction {
    /**
     * Removes the servers identified in the
     * <code>RemoveResourceGroupsForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log =
            LogFactory.getLog(RemoveResourceGroupsAction.class.getName());

        RemoveResourceGroupsForm rmForm = (RemoveResourceGroupsForm) form;
        Integer resourceId = rmForm.getRid();
        Integer resourceType = rmForm.getType();

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);            
        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefEntityID entityId = new AppdefEntityID(resourceType.intValue(), 
                                                     resourceId);

        Integer[] groups = rmForm.getG();
        if (groups != null) {
            log.trace("removing groups " + groups +
                      " for resource [" + resourceId + "]");
            boss.batchGroupRemove(sessionId.intValue(), entityId,
                                  groups);

            RequestUtils
                .setConfirmation(request,
                                 "resource.common.inventory.confirm.RemoveResourceGroups");
        }

        return returnSuccess(request, mapping, forwardParams);
        
    }

}
