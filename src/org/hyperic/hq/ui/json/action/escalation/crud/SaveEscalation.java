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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.json.JSONObject;

public class SaveEscalation 
    extends BaseAction
{
    private final Log _log = LogFactory.getLog(SaveAction.class);
    
    public void execute(JsonActionContext context) throws Exception {
        Map p = context.getParameterMap();
        
        String  name     = ((String[])p.get("name"))[0];
        String  desc     = ((String[])p.get("description"))[0];
        long maxWait     = Long.parseLong(((String[])p.get("maxWaitTime"))[0]); 
        boolean pausable =
            Boolean.valueOf(((String[])p.get("allowPause"))[0]).booleanValue();
        boolean notifyAll = 
            Boolean.valueOf(((String[])p.get("notifyAll"))[0]).booleanValue();
            
        // These specify an optional alert definition to attach to
        String[] aDef  = (String[])p.get(ALERTDEF_ID);
        String[] gaDef = (String[])p.get(GALERTDEF_ID);
        EscalationAlertType alertType = null;
        Integer alertDefId = null;

        if (aDef != null && !"undefined".equals(aDef[0]) && aDef[0].length() > 0) {
            alertType  = ClassicEscalationAlertType.CLASSIC;
            alertDefId = Integer.valueOf(aDef[0]);
        } else if (gaDef != null && !"undefined".equals(gaDef[0]) && aDef[0].length() > 0) {
            alertType  = GalertEscalationAlertType.GALERT;
            alertDefId = Integer.valueOf(gaDef[0]);
        }
        
        EventsBoss eBoss  = 
            ContextUtils.getEventsBoss(context.getServletContext());
        JSONObject result;
        try {
            Escalation e = eBoss.createEscalation(context.getSessionId(), name, 
                                                  desc, pausable, maxWait, 
                                                  notifyAll, alertType, alertDefId); 
             result = Escalation.getJSON((e));
        } catch (DuplicateObjectException exception) {
            //An escalation by this name already exists show error msg.
            result = new JSONObject();
            result.put("error", "An escalation with this name already exists.");
        }
        context.setJSONResult(new JSONResult(result));
        context.getRequest().setAttribute(Escalation.JSON_NAME, result);
    }
}
