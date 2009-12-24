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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An action that adds users to an alert definition in the BizApp.
 * 
 */
public class AddUsersAction
    extends AddNotificationsAction {

    private final Log log = LogFactory.getLog(AddUsersAction.class.getName());

    @Autowired
    public AddUsersAction(EventsBoss eventsBoss) {
        super(eventsBoss);
    }

    protected ActionForward preProcess(HttpServletRequest request, ActionMapping mapping, AddNotificationsForm form,
                                       Map<String, Object> params, HttpSession session) throws Exception {
        AddUsersForm addForm = (AddUsersForm) form;
        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            if (addForm.isCancelClicked() || addForm.isResetClicked()) {
                log.debug("removing pending user list");
                SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
            } else if (addForm.isAddClicked()) {
                log.debug("adding to pending user list");
                SessionUtils.addToList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getAvailableUsers());
                log.debug("@@@@@@@@@@" + addForm.getAvailableUsers().toString());
                for (int i = 0; i < addForm.getAvailableUsers().length; i++) {
                    log.debug("Avalilable Users " + addForm.getAvailableUsers()[i]);
                }
            } else if (addForm.isRemoveClicked()) {
                log.debug("removing from pending user list");
                SessionUtils.removeFromList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getPendingUsers());
            }
        }

        return forward;
    }

    protected void postProcess(HttpServletRequest request, HttpSession session) {
        log.debug("removing pending user list");
        SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
        RequestUtils.setConfirmation(request, "alerts.config.confirm.AddUsers");
    }

    protected Set<Object> getNotifications(AddNotificationsForm form, HttpSession session) {
        log.debug("getting pending user list");
        Integer[] pendingUserIds = SessionUtils.getList(session, Constants.PENDING_USERS_SES_ATTR);
        Set<Object> userIds = new HashSet<Object>();
        for (int i = 0; i < pendingUserIds.length; i++) {
            userIds.add(pendingUserIds[i]);
            log.debug("adding user [" + pendingUserIds[i] + "]");
        }

        return userIds;
    }

    public int getNotificationType() {
        return EmailActionConfig.TYPE_USERS;
    }
}
