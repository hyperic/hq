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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action which saves the general properties for a group
 */
public class EditGeneralAction extends BaseAction {

    /**
     * Create the server with the attributes specified in the given
     * <code>GroupForm</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(EditGeneralAction.class.getName());
        
        ResourceForm rForm = (ResourceForm) form;

        Integer rid;
        Integer entityType;
        HashMap forwardParams = new HashMap(2);
            
        rid = rForm.getRid();
        entityType = rForm.getType();
        forwardParams.put(Constants.RESOURCE_PARAM, rid);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
            
        ActionForward forward = checkSubmit(request, mapping, form,
            forwardParams, BaseAction.YES_RETURN_PATH );
            
        if (forward != null) {
            return forward;
        }
                 
        AppdefGroupValue rValue;
        ServletContext ctx = getServlet().getServletContext();

        try {
            Integer sessionId = RequestUtils.getSessionId(request);
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            
            Integer groupId = RequestUtils.getResourceId(request);
            
            rValue = boss.findGroup(sessionId.intValue(), groupId);
            
            rForm.updateResourceValue(rValue);            
            
            boss.saveGroup(sessionId.intValue(), rValue);
              
            // XXX: enable when we have a confirmed functioning API
            log.trace("saving group [" + rValue.getName()
                               + "]" + " with attributes " + rForm);

            RequestUtils.setConfirmation(request,
                   "resource.group.inventory.confirm.EditGeneralProperties");
                                         
            return returnSuccess(request, mapping, forwardParams, 
                BaseAction.YES_RETURN_PATH);
        }
        catch (AppdefGroupNotFoundException e1) {
            log.debug("group update failed:", e1);
            RequestUtils
                .setError(request,
                          "resource.group.inventory.error.GroupNotFound");
            return returnFailure(request, mapping);
        } 
        catch (ParameterNotFoundException e1) {
            RequestUtils
                .setError(request,
                          Constants.ERR_RESOURCE_ID_FOUND);
            return returnFailure(request, mapping);
        }
        catch (GroupDuplicateNameException ex) {
            log.debug("group creation failed:", ex);
            RequestUtils
                .setError(request,
                          "resource.group.inventory.error.DuplicateGroupName");
            return returnFailure(request, mapping);
        }
    }
}
