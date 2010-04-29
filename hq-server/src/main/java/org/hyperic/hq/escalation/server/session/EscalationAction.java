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

package org.hyperic.hq.escalation.server.session;

import java.io.Serializable;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.util.json.JSON;
import org.json.JSONException;
import org.json.JSONObject;

public class EscalationAction 
    implements Serializable, JSON
{
    private Escalation _parent;
    private Action      _action;
    private long        _waitTime;

    protected EscalationAction() {
    }

    protected EscalationAction(Escalation parent, Action action, 
                               long waitTime) 
    {
        _parent   = parent;
        _action   = action;
        _waitTime = waitTime;
    }
    
    protected void setParent(Escalation parent) {
        _parent = parent;
    }

    public Escalation getParent() {
        return _parent;
    }
    
    public Action getAction() {
        return _action;
    }

    protected void setAction(Action action) {
        _action = action;
    }

    public long getWaitTime() {
        return _waitTime;
    }

    protected void setWaitTime(long waitTime) {
        _waitTime = waitTime;
    }

    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put(getAction().getJsonName(), getAction().toJSON())
                .put("waitTime", getWaitTime());
        } catch(JSONException e) {
            throw new SystemException(e);
        }
    }

    public String getJsonName() {
        return "escalationAction";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof EscalationAction)) {
            return false;
        }

        EscalationAction o = (EscalationAction)obj;
        return _parent.equals(o.getParent()) && 
               _waitTime == o.getWaitTime() &&
               _action.equals(o.getAction());
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + _parent.hashCode();
        result = 37*result + (int)(_waitTime ^ (_waitTime >>> 32));
        result = 37*result + _action.hashCode();

        return result;
    }
}


