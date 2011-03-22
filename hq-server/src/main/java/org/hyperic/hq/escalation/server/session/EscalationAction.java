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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parent;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.util.json.JSON;
import org.json.JSONException;
import org.json.JSONObject;

@Embeddable
public class EscalationAction implements Serializable, JSON {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACTION_ID", nullable = false)
    @Index(name = "ESC_ACTION_ID_IDX")
    private Action action;

    @Parent
    private Escalation parent;

    @Column(name = "WAIT_TIME", nullable = false)
    private long waitTime;

    protected EscalationAction() {
    }

    protected EscalationAction(Escalation parent, Action action, long waitTime) {
        this.parent = parent;
        this.action = action;
        this.waitTime = waitTime;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof EscalationAction)) {
            return false;
        }

        EscalationAction o = (EscalationAction) obj;
        return parent.equals(o.getParent()) && waitTime == o.getWaitTime() &&
               action.equals(o.getAction());
    }

    public Action getAction() {
        return action;
    }

    public String getJsonName() {
        return "escalationAction";
    }

    public Escalation getParent() {
        return parent;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + parent.hashCode();
        result = 37 * result + (int) (waitTime ^ (waitTime >>> 32));
        result = 37 * result + action.hashCode();

        return result;
    }

    protected void setAction(Action action) {
        this.action = action;
    }

    protected void setParent(Escalation parent) {
        this.parent = parent;
    }

    protected void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public JSONObject toJSON() {
        try {
            return new JSONObject().put(getAction().getJsonName(), getAction().toJSON()).put(
                "waitTime", getWaitTime());
        } catch (JSONException e) {
            throw new SystemException(e);
        }
    }
}
