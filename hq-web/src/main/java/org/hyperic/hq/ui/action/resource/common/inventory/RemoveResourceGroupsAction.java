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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class RemoveResourceGroupsAction
    extends BaseAction {
    private final Log log = LogFactory.getLog(RemoveResourceGroupsAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public RemoveResourceGroupsAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    /**
     * Removes the servers identified in the
     * <code>RemoveResourceGroupsForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        RemoveResourceGroupsForm rmForm = (RemoveResourceGroupsForm) form;
        Integer resourceId = rmForm.getRid();
        Integer resourceType = rmForm.getType();

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefEntityID entityId = new AppdefEntityID(resourceType.intValue(), resourceId);
        
        try {
            Integer[] groups = rmForm.getG();
            if (groups != null) {
                log.trace("removing groups " + groups + " for resource [" + resourceId + "]");
                appdefBoss.batchGroupRemove(sessionId.intValue(), entityId, groups);
    
                RequestUtils.setConfirmation(request, "resource.common.inventory.confirm.RemoveResourceGroups");
            }
    
            return returnSuccess(request, mapping, forwardParams);
        } catch (VetoException ve) {
            RequestUtils.setErrorObject(request,
                "resource.group.inventory.error.UpdateResourceListVetoed",ve.getMessage());
            return returnFailure(request, mapping, forwardParams);
        }        
    }

}
