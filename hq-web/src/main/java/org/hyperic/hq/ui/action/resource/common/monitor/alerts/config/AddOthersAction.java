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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An action that adds other email addresses ( those that are not in CAM ) to an
 * alert definition in the BizApp.
 * 
 */
public class AddOthersAction
    extends AddNotificationsAction {

    @Autowired
    public AddOthersAction(EventsBoss eventsBoss) {
        super(eventsBoss);
    }

    protected ActionForward preProcess(HttpServletRequest request, ActionMapping mapping, AddNotificationsForm form,
                                       Map<String, Object> params, HttpSession session) throws Exception {
        return checkSubmit(request, mapping, form, params);
    }

    protected void postProcess(HttpServletRequest request, HttpSession session) {
        RequestUtils.setConfirmation(request, "alerts.config.confirm.AddOthers");
    }

    protected Set<Object> getNotifications(AddNotificationsForm form, HttpSession session) {
        AddOthersForm addForm = (AddOthersForm) form;
        String emailAddresses = addForm.getEmailAddresses();
        StringTokenizer token = new StringTokenizer(emailAddresses, ",;");
        Set<Object> emails = new HashSet<Object>();
        while (token.hasMoreTokens()) {
            emails.add(token.nextToken().trim());
        }

        return emails;
    }

    public int getNotificationType() {
        return EmailActionConfig.TYPE_EMAILS;
    }
}
