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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.ArrayUtil;
import org.json.JSONObject;

public class UpdateEscalation 
    extends BaseAction
{
    private final Log _log = LogFactory.getLog(SaveAction.class);

    public void execute(JsonActionContext context) throws Exception {
        Map p = context.getParameterMap();

        for (Iterator i=p.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            
            _log.warn("key=" + ent.getKey() + " val=" + 
                      ArrayUtil.toString((Object[])ent.getValue()));
        }
        
        String  name     = ((String[])p.get("name"))[0];
        String  desc     = ((String[])p.get("description"))[0];
        long maxWait     = Long.parseLong(((String[])p.get("maxWaitTime"))[0]); 
        boolean pausable =
            Boolean.valueOf(((String[])p.get("allowPause"))[0]).booleanValue();
        boolean notifyAll = 
            Boolean.valueOf(((String[])p.get("notifyAll"))[0]).booleanValue();

        Integer id = Integer.valueOf(((String[]) p.get(ID))[0]);

        EventsBoss eBoss = 
            ContextUtils.getEventsBoss(context.getServletContext());
        Escalation escalation = eBoss.findEscalationById(context.getSessionId(),
                                                         id);
        eBoss.updateEscalation(context.getSessionId(), escalation,
                               name, desc, maxWait, pausable, notifyAll);
        
        JSONObject result = Escalation.getJSON(escalation);
        context.setJSONResult(new JSONResult(result));
        context.getRequest().setAttribute(Escalation.JSON_NAME, result);
    }
}
