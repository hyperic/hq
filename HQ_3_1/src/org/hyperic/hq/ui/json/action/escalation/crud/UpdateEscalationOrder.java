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

package org.hyperic.hq.ui.json.action.escalation.crud;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAction;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.json.JSONException;

public class UpdateEscalationOrder extends BaseAction {
    private final Log _log = LogFactory.getLog(UpdateEscalationOrder.class);

    public void execute(JsonActionContext context)
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException, JSONException, RemoteException 
    {
        Map map = context.getParameterMap();
        
        if (map.get(ID) == null) {
            throw new IllegalArgumentException("Escalation id not found");
        }
        
        Integer id = context.getId();
        String[] sOrder = (String[]) map.get("viewEscalationUL[]");
        
        EscalationManagerLocal eMan = EscalationManagerEJBImpl.getOne();
        Escalation esc = eMan.findById(id);
        List actions   = new ArrayList(sOrder.length);
        
        for (int i=0; i<sOrder.length; i++) {
            EscalationAction action;
            Integer actionId;
            
            try {
                actionId = Integer.valueOf(sOrder[i]);
            } catch(NumberFormatException e) {
                throw new SystemException("Bad order", e);
            }
            
            action = esc.getAction(actionId);
            if (action == null) {
                throw new IllegalArgumentException("Escalation does not " + 
                                                   "contain an action with " +
                                                   "id=" + actionId);
            }
            actions.add(action);
        }
        
        eMan.updateEscalationOrder(esc, actions);
    }
}
