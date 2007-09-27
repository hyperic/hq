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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.util.JavaBeanPropertyComparator;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;


/**
 * View an alert definition -- notified roles.
 *
 */
public class ViewDefinitionNotificationsOthersAction
    extends ViewDefinitionNotificationsAction
{
    private static final String[] SORT_ATTRS = { "label" };

    private Log log =
        LogFactory.getLog( ViewDefinitionNotificationsOthersAction.class.getName() );

    public int getNotificationType() { return EmailActionConfig.TYPE_EMAILS; }

    protected PageList getPageList(int sessionID, AuthzBoss ab,
                                   EmailActionConfig ea, PageControl pc)
        throws NamingException,
               FinderException,
               CreateException,
               SessionTimeoutException,
               SessionNotFoundException,
               PermissionException,
               RemoteException
    {
        PageList notifyList = new PageList();
        for (Iterator it=ea.getUsers().iterator(); it.hasNext();) {
            String email = (String)it.next();
            LabelValueBean lvb = new LabelValueBean(email, email);
            notifyList.add(lvb);
        }

        int sortOrder = pc.isAscending() ?
            JavaBeanPropertyComparator.ASCENDING :
            JavaBeanPropertyComparator.DESCENDING;
        JavaBeanPropertyComparator c =
            new JavaBeanPropertyComparator(SORT_ATTRS[pc.getSortattribute()], sortOrder);
        Collections.sort(notifyList, c);

        return notifyList;
    }
}

// EOF
