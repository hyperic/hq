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

import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.json.JSONArray;
import org.json.JSONObject;

public class ListActiveEscalations extends BaseAction {
    public void execute(JsonActionContext ctx) 
        throws Exception 
    {
        EventsBoss eBoss = Bootstrap.getBean(EventsBoss.class);
        AuthzBoss aBoss = Bootstrap.getBean(AuthzBoss.class);
        AuthzSubject me = aBoss.getCurrentSubject(ctx.getSessionId());
        List states = eBoss.getActiveEscalations(ctx.getSessionId(),
                                                 10);  // XXX

        
        JSONArray resArr = new JSONArray();
        
        for (Iterator i=states.iterator(); i.hasNext(); ) {
            EscalationState s = (EscalationState)i.next();
            JSONObject sj = new JSONObject();
            Escalatable alert = eBoss.getEscalatable(ctx.getSessionId(), s);
            AlertDefinitionInterface defInfo = 
                alert.getAlertInfo().getAlertDefinitionInterface();
            AppdefEntityID entId = 
                AppdefUtil.newAppdefEntityId(defInfo.getResource());
            AppdefEntityValue ent = new AppdefEntityValue(entId, me);
                
            sj.put(Escalation.JSON_NAME, s.getEscalation().toJSON());
            sj.put("alertId", alert.getId());
            sj.put("alertType", s.getAlertType().getCode());
            sj.put("alertName", defInfo.getName()); 
            sj.put("entName", ent.getName());
            AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(defInfo.getResource());
            sj.put("entType", aeid.getType());
            sj.put("entId", aeid.getID()); 
            sj.put("timeUntilNext", 
                   System.currentTimeMillis() - s.getNextActionTime());
            sj.put("nextActionIdx", s.getNextAction());
            sj.put("timeOutstanding", System.currentTimeMillis() - 
                                      alert.getAlertInfo().getTimestamp());
            sj.put("acked", s.isPaused());
            if (s.isPaused())
                sj.put("ackedBy", s.getAcknowledgedBy().getFullName());
                   
            resArr.put(sj);
        }
        
        JSONObject res = new JSONObject();
        
        res.put("escSummary", resArr);
        ctx.setJSONResult(new JSONResult(res));
    }
}
