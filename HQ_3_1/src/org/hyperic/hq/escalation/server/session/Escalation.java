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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.util.json.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Escalation
    extends PersistedObject
    implements ContainerManagedTimestampTrackable, JSON
{
    public static final String JSON_NAME = "escalation";
    
    // Name of the escalation chain
    private String _name;

    // Description of the escalation chain
    private String _description;

    // Allow the escalation to be paused (up to maxWaitTime milliseconds)
    private boolean _pauseAllowed;

    // Max amount of time that the escalation can be paused
    private long _maxPauseTime;

    // If true, notify everyone specified by the chain, else just the previous
    // notifications.
    private boolean _notifyAll;

    private long _ctime;
    private long _mtime;
    private List _actions = new ArrayList();

    protected Escalation() {}

    Escalation(String name, String description, boolean pauseAllowed,
                long maxPauseTime, boolean notifyAll)
    {
        _name         = name;
        _description  = description;
        _pauseAllowed = pauseAllowed;
        _maxPauseTime = maxPauseTime;
        _notifyAll    = notifyAll;
        _ctime        = System.currentTimeMillis();
        _mtime        = _ctime;
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return true;
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }
    
    public String getName() {
        return _name;
    }

    protected void setName(String name) {
        _name = name;
    }

    public String getDescription() {
        return _description;
    }

    protected void setDescription(String description) {
        _description = description;
    }

    public boolean isPauseAllowed() {
        return _pauseAllowed;
    }

    protected void setPauseAllowed(boolean allowed) {
        _pauseAllowed = allowed;
    }

    public long getMaxPauseTime() {
        return _maxPauseTime;
    }

    protected void setMaxPauseTime(long pauseTime) {
        _maxPauseTime = pauseTime;
    }

    public boolean isNotifyAll() {
        return _notifyAll;
    }

    protected void setNotifyAll(boolean notifyAll) {
        _notifyAll = notifyAll;
    }

    public long getCreationTime() {
        return _ctime;
    }

    protected void setCreationTime(long ctime) {
        _ctime = ctime;
    }

    public long getModifiedTime() {
        return _mtime;
    }

    public void setModifiedTime(long mtime) {
        _mtime = mtime;
    }

    public List getActions() {
        return Collections.unmodifiableList(_actions);
    }
    
    protected List getActionsList() {
        return _actions;
    }

    protected void setActionsList(List actions) {
        _actions = actions;
    }
    
    protected EscalationAction addAction(long waitTime, Action a) {
        EscalationAction res = new EscalationAction(this, a, waitTime);

        getActionsList().add(res);
        return res;
    }
    
    /**
     * Find an escalation action based on the ID of its associated action. 
     */
    public EscalationAction getAction(Integer id) {
        List a = getActions();
        
        for (Iterator i=a.iterator(); i.hasNext(); ) {
            EscalationAction ea = (EscalationAction)i.next();
            
            if (ea.getAction().getId().equals(id))
                return ea;
        }
        return null;
    }

    public JSONObject toJSON() {
        try {
            JSONArray actions = new JSONArray();
            for (Iterator i = getActions().iterator(); i.hasNext(); ) {
                EscalationAction action = (EscalationAction)i.next();

                actions.put(action.toJSON());
            }

            JSONObject json = new JSONObject()
                .put("name", getName())
                .put("description",
                     getDescription() != null ? getDescription() : "")
                .put("allowPause", isPauseAllowed())
                .put("maxWaitTime", getMaxPauseTime())
                .put("notifyAll", isNotifyAll())
                .put("creationTime", getCreationTime())
                .put("modifiedTime", getModifiedTime())
                .put("actions", actions);

            if (getId() != null) {
                json.put("id", getId());
                json.put("_version_", get_version_());
            }
            return json;
        } catch(JSONException e) {
            throw new SystemException(e);
        }
    }

    public String getJsonName() {
        return JSON_NAME;
    }
    
    public static JSONObject getJSON(Escalation e) 
        throws JSONException
    {
        if (e == null)
            return null;

        return new JSONObject().put(e.getJsonName(), e.toJSON());
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Escalation) || !super.equals(obj)) {
            return false;
        }

        Escalation o = (Escalation)obj;
        return getName().equals(o.getName());
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + getName().hashCode();

        return result;
    }

    public String toString() {
        return "(id=" + getId() + ", name=" + _name + ", allowPause=" +
            _pauseAllowed + ", maxPauseTime=" + _maxPauseTime + ", notifyAll=" +
            _notifyAll + ", created=" + _ctime + ")";
    }
}
