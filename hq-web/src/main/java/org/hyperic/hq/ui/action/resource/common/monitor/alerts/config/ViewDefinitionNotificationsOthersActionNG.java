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
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.action.portlet.savedqueries.KeyValuePair;
import org.hyperic.util.JavaBeanPropertyComparator;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * View an alert definition -- notified roles.
 * 
 */
@Component ("viewDefinitionNotificationsOthersActionNG")
@Scope("prototype")
public class ViewDefinitionNotificationsOthersActionNG
    extends ViewDefinitionNotificationsActionNG	 {
    
	private static final String[] SORT_ATTRS = { "key" };

   
    

    public int getNotificationType() {
        return EmailActionConfig.TYPE_EMAILS;
    }

    protected PageList getPageList(int sessionID, EmailActionConfig ea, PageControl pc) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException, RemoteException {
        PageList<KeyValuePair> notifyList = new PageList<KeyValuePair>();
        for (Iterator it = ea.getUsers().iterator(); it.hasNext();) {
            String email = (String) it.next();
            KeyValuePair lvb = new KeyValuePair(email, email);
            notifyList.add(lvb);
        }

        int sortOrder = pc.isAscending() ? JavaBeanPropertyComparator.ASCENDING : JavaBeanPropertyComparator.DESCENDING;
        JavaBeanPropertyComparator c = new JavaBeanPropertyComparator(SORT_ATTRS[pc.getSortattribute()], sortOrder);
        Collections.sort(notifyList, c);

        return notifyList;
    }
}
