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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.util.JavaBeanPropertyComparator;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * View an alert definition -- notified roles.
 * 
 */
public class ViewDefinitionNotificationsUsersAction
    extends ViewDefinitionNotificationsAction {
    private static final String[] SORT_ATTRS = { "name", "lastName", "firstName" };

    private final Log log = LogFactory.getLog(ViewDefinitionNotificationsUsersAction.class.getName());

    @Autowired
    public ViewDefinitionNotificationsUsersAction(EventsBoss eventsBoss, AuthzBoss authzBoss) {
        super(eventsBoss, authzBoss);
    }

    public int getNotificationType() {
        return EmailActionConfig.TYPE_USERS;
    }

    protected PageList getPageList(int sessionID, EmailActionConfig ea, PageControl pc) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException, RemoteException {
        log.debug("userIds: " + ea.getUsers());
        Integer[] userIds = new Integer[ea.getUsers().size()];
        userIds = (Integer[]) ea.getUsers().toArray(userIds);
        PageList<AuthzSubjectValue> notifyList = authzBoss.getSubjectsById(new Integer(sessionID), userIds,
            PageControl.PAGE_ALL);

        int sortOrder = pc.isAscending() ? JavaBeanPropertyComparator.ASCENDING : JavaBeanPropertyComparator.DESCENDING;
        JavaBeanPropertyComparator c = new JavaBeanPropertyComparator(SORT_ATTRS[pc.getSortattribute()], sortOrder);
        Collections.sort(notifyList, c);

        return notifyList;
    }
}
