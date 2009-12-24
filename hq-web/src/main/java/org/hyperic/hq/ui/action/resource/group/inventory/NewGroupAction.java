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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class NewGroupAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(NewGroupAction.class.getName());
    private AppdefBoss appdefBoss;

    @Autowired
    public NewGroupAction(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }

    /**
     * Create the group with the attributes specified in the given
     * <code>GroupForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        final GroupForm newForm = (GroupForm) form;

        // Clean up after ourselves first
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.ENTITY_IDS_ATTR);
        session.removeAttribute(Constants.RESOURCE_TYPE_ATTR);

        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        try {

            Integer sessionId = RequestUtils.getSessionId(request);
            ResourceGroup newGroup;

            final Integer entType = newForm.getEntityTypeId();

            // Append username to private groups
            if (newForm.isPrivateGroup()) {
                final WebUser user = RequestUtils.getWebUser(session);
                final String privateName = RequestUtils.message(request, "resource.group.name.private",
                    new Object[] { user.getName() });
                newForm.setName(newForm.getName() + " " + privateName);
            }

            if (newForm.getGroupType() == Constants.APPDEF_TYPE_GROUP_COMPAT) {
                newGroup = appdefBoss.createGroup(sessionId, entType, newForm.getResourceTypeId(), newForm.getName(),
                    newForm.getDescription(), newForm.getLocation(), newForm.getEntityIds(), newForm.isPrivateGroup());
            } else {
                // Constants.APPDEF_TYPE_GROUP_ADHOC
                if (entType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION ||
                    entType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                    newGroup = appdefBoss.createGroup(sessionId, entType, newForm.getName(), newForm.getDescription(),
                        newForm.getLocation(), newForm.getEntityIds(), newForm.isPrivateGroup());
                } else {
                    // otherwise, create a mixed group
                    newGroup = appdefBoss.createGroup(sessionId, newForm.getName(), newForm.getDescription(), newForm
                        .getLocation(), newForm.getEntityIds(), newForm.isPrivateGroup());
                }
            }

            log.trace("creating group [" + newForm.getName() + "] with attributes " + newForm);

            HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
            forwardParams.put(Constants.RESOURCE_PARAM, newGroup.getId());
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, AppdefEntityConstants.APPDEF_TYPE_GROUP);

            newForm.setRid(newGroup.getId());

            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.CreateGroup", newForm.getName());

            return returnNew(request, mapping, forwardParams);
        } catch (GroupDuplicateNameException ex) {
            log.debug("group creation failed:", ex);
            RequestUtils.setError(request, "resource.group.inventory.error.DuplicateGroupName");
            return returnFailure(request, mapping);
        }
    }
}
