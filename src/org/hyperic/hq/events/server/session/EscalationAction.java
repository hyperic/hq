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

package org.hyperic.hq.events.server.session;

import org.json.JSONObject;
import org.json.JSONException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.json.JSON;

import java.util.Set;
import java.io.Serializable;

public class EscalationAction implements Serializable, JSON {
    private Action _action;
    private long   _waitTime;

    public static EscalationAction newInstance(JSONObject json)
        throws JSONException
    {
        return new EscalationAction(json);
    }

    /**
     *
     * @param type  Action Type
     * @param notifs  list of notification types
     * @param waitTime  time to wait until escalating to the next level
     */
    public static EscalationAction newEmailAction(int type, Set notifs,
                                                  long waitTime) 
    {
        Action act = Action.newEmailAction(type, notifs);
        return createEscalationAction(act, waitTime);
    }

    public static EscalationAction newSyslogAction(String metaProject,
                                                   String project,
                                                   String version,
                                                   long waitTime)
    {
        Action act = Action.newSyslogAction(metaProject, project, version);
        return createEscalationAction(act, waitTime);
    }

    private static EscalationAction createEscalationAction(Action act,
                                                           long waitTime)
    {
        EscalationAction eact = new EscalationAction();
        eact.setAction(act);
        eact.setWaitTime(waitTime);
        return eact;
    }

    protected EscalationAction() {
    }

    protected EscalationAction(JSONObject json) throws JSONException {
        setWaitTime(json.optLong("waitTime"));
        setAction(Action.newInstance(
            json.getJSONObject(new Action().getJsonName())));
    }

    protected EscalationAction(Action action, long waitTime) {
        _action = action;
        _waitTime = waitTime;
    }

    public Action getAction() {
        return _action;
    }

    public void setAction(Action action) {
        _action = action;
    }

    public long getWaitTime() {
        return _waitTime;
    }

    public void setWaitTime(long waitTime) {
        _waitTime = waitTime;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof EscalationAction)) {
            return false;
        }
        EscalationAction o = (EscalationAction)obj;
        return _waitTime == o.getWaitTime() &&
               (_action == o.getAction() ||
                (_action!=null && o.getAction()!=null &&
                 _action.equals(o.getAction())
                )
               );
    }

    public JSONObject toJSON() {
        try {
            return new JSONObject()
                            .put(_action.getJsonName(), _action.toJSON())
                            .put("waitTime", _waitTime);
        } catch(JSONException e) {
            throw new SystemException(e);
        }
    }

    public String getJsonName() {
        return "escalationAction";
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (int)(_waitTime ^ (_waitTime >>> 32));
        result = 37*result + (_action != null ? _action.hashCode() : 0);

        return result;
    }

    public String toString() {
        return new StringBuffer()
            .append("(action=")
            .append(_action)
            .append(", waitTime=" + _waitTime)
            .toString();
    }
}


