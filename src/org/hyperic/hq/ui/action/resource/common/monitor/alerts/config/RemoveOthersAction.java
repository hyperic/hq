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
import java.util.HashSet;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.ui.exception.ServiceLocatorException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;


/**
 * An Action that removes emails for an alert definition.
 *
 */
public class RemoveOthersAction extends RemoveNotificationsAction {
    Log log = LogFactory.getLog(RemoveOthersAction.class.getName());

    public int getNotificationType() { return EmailActionConfig.TYPE_EMAILS; }

    /**
     * Handles the actual work of removing emails from the action.
     */
    protected ActionForward handleRemove(ActionMapping mapping,
                                         HttpServletRequest request,
                                         Map params,
                                         Integer sessionID,
                                         ActionValue action,
                                         EmailActionConfig ea,
                                         EventsBoss eb,
                                         RemoveNotificationsForm rnForm)
     throws Exception {
        String[] emails = rnForm.getEmails();
        if (null != emails) {
            log.debug("emails.length=" + emails.length);
            HashSet storedEmails = new HashSet();
            storedEmails.addAll( ea.getUsers() );
            log.debug("storedEmails (pre): " + storedEmails);
            for (int i=0; i<emails.length; ++i) {
                storedEmails.remove(emails[i]);
            }
            log.debug("storedEmails (post): " + storedEmails);
            ea.setNames( StringUtil.iteratorToString(storedEmails.iterator(), ",") );
            action.setConfig( ea.getConfigResponse().encode() );
            eb.updateAction(sessionID.intValue(), action);
        }

        return returnSuccess(request, mapping, params); 
    }
}

// EOF
