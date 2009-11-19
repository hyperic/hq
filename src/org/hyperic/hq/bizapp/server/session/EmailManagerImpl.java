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

package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.email.EmailFilter;
import org.hyperic.hq.bizapp.server.action.email.EmailRecipient;
import org.hyperic.hq.bizapp.shared.EmailManager;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This SessionEJB is used to ensure that EmailFilter does not fail since it
 * requires an associated session.
 * Class uses transaction type NotSupported so that callers don't get hung up on
 * Email since it is an I/O operation.
 */
@Service
@Transactional
// Really? TODO
public class EmailManagerImpl implements EmailManager {
    /**
     */
    public void sendAlert(EmailFilter filter, AppdefEntityID appEnt,
                          EmailRecipient[] addresses, String subject,
                          String[] body, String[] htmlBody, int priority,
                          boolean filterNotifications) {
        filter.sendAlert(appEnt, addresses, subject, body, htmlBody, priority,
                         filterNotifications);
    }

    public static EmailManager getOne() {
        return Bootstrap.getBean(EmailManager.class);
    }
}
