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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action which saves the general properties for a group
 */
public class EditGeneralAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(EditGeneralAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public EditGeneralAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    /**
     * Create the server with the attributes specified in the given
     * <code>GroupForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        ResourceForm rForm = (ResourceForm) form;

        Integer rid;
        Integer entityType;
        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);

        rid = rForm.getRid();
        entityType = rForm.getType();
        forwardParams.put(Constants.RESOURCE_PARAM, rid);
        forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

        ActionForward forward = checkSubmit(request, mapping, form, forwardParams, BaseAction.YES_RETURN_PATH);

        if (forward != null) {
            return forward;
        }

        AppdefGroupValue rValue;

        try {
            Integer sessionId = RequestUtils.getSessionId(request);

            Integer groupId = RequestUtils.getResourceId(request);

            rValue = appdefBoss.findGroup(sessionId.intValue(), groupId);

            ResourceGroup group = appdefBoss.findGroupById(sessionId.intValue(), groupId);

            // See if this is a private group
            boolean isPrivate = true;
            Collection<ResourceGroup> groups = appdefBoss.getGroupsForResource(sessionId, group.getResource());
            for (Iterator<ResourceGroup> it = groups.iterator(); it.hasNext();) {
                ResourceGroup g = it.next();
                isPrivate = !g.getId().equals(AuthzConstants.rootResourceGroupId);
                if (!isPrivate)
                    break;
            }

            if (isPrivate) {
                // Make sure the username appears in the name
                final String owner = group.getResource().getOwner().getName();
                if (rForm.getName().indexOf(owner) < 0) {
                    final String privateName = RequestUtils.message(request, "resource.group.name.private",
                        new Object[] { owner });
                    rForm.setName(rForm.getName() + " " + privateName);
                }
            }

            appdefBoss.updateGroup(sessionId.intValue(), group, rForm.getName(), rForm.getDescription(), rForm
                .getLocation());

            // XXX: enable when we have a confirmed functioning API
            log.trace("saving group [" + rValue.getName() + "]" + " with attributes " + rForm);

            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.EditGeneralProperties");

            return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
        } catch (AppdefGroupNotFoundException e1) {
            log.debug("group update failed:", e1);
            RequestUtils.setError(request, "resource.group.inventory.error.GroupNotFound");
            return returnFailure(request, mapping);
        } catch (ParameterNotFoundException e1) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_ID_FOUND);
            return returnFailure(request, mapping);
        } catch (GroupDuplicateNameException ex) {
            log.debug("group creation failed:", ex);
            RequestUtils.setError(request, "resource.group.inventory.error.DuplicateGroupName");
            return returnFailure(request, mapping);
        }
    }
}
