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

package org.hyperic.hq.ui.json.action.escalation;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.MEscalationAlertType;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.util.ContextUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.rmi.RemoteException;

import javax.ejb.FinderException;

public class EscalationWebMediator {
    private static final EscalationWebMediator ourInstance =
        new EscalationWebMediator();

    public static EscalationWebMediator getInstance() { 
        return ourInstance;
    }

    private EscalationWebMediator() {}

    private EventsBoss getEventsBoss(JsonActionContext context) {
        return ContextUtils.getEventsBoss(context.getServletContext());
    }

    public JSONObject jsonByEscalationName(JsonActionContext context,
                                           int sessionId, String name)
        throws JSONException, PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        return getEventsBoss(context).jsonByEscalationName(sessionId, name);
    }

    public JSONObject jsonByEscalationId(JsonActionContext context,
                                         int sessionId, Integer id)
        throws JSONException, PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        return getEventsBoss(context).jsonByEscalationId(sessionId, id);
    }

    public JSONObject jsonEscalationByAlertDefId(JsonActionContext context,
                                                 int sessionId,
                                                 Integer alertDefId,
                                                 MEscalationAlertType alertType)
        throws JSONException, PermissionException, SessionException,
               RemoteException, FinderException, DuplicateObjectException
    {
        return getEventsBoss(context)
            .jsonEscalationByAlertDefId(sessionId, alertDefId, alertType);
    }

    public JSONArray listAllEscalationName(JsonActionContext context,
                                           int sessionId)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return getEventsBoss(context).listAllEscalationName(sessionId);
    }

    public void removeEscalation(JsonActionContext context,
                                 int sessionId, Integer id)
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        getEventsBoss(context)
            .deleteEscalationById(sessionId, new Integer[]{id});
    }

    public JSONObject saveEscalation(JsonActionContext context,
                                     int sessionId, Integer alertDefId,
                                     MEscalationAlertType alertType, 
                                     JSONObject json)
        throws PermissionException,
               SessionException, FinderException, 
               JSONException, RemoteException, DuplicateObjectException
    {
        getEventsBoss(context).saveEscalation(sessionId, alertDefId,
                                              alertType, json);
        return getEventsBoss(context)
            .jsonEscalationByAlertDefId(sessionId, alertDefId, alertType);
    }
}
