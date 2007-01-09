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

package org.hyperic.hq.ui.json.action.escalation.finder;

import java.rmi.RemoteException;

import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 */
public class JsonEscalationByAlertDefId extends BaseAction
{
    private Log log = LogFactory.getLog(JsonEscalationByAlertDefId.class);
    
    public void execute(JsonActionContext context)
        throws JSONException,
               PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        JSONObject escalation;
        
        try {
            escalation = EscalationWebMediator.getInstance()
            .jsonEscalationByAlertDefId(
                        context, context.getSessionId(), context.getId(),
                        context.getAlertDefType());
        } catch(SessionException e) {
            throw new SystemException(e);
        } catch(FinderException e) {
            throw new SystemException(e);
        } catch(DuplicateObjectException e) {
            throw new SystemException(e);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("JsonEscalationByAlertDefId: " + context.getId() +
                      ": " + escalation);
        }
        context.setJSONResult(new JSONResult(escalation));
    }
}
