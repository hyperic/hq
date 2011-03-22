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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IndexColumn;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.util.json.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name = "EAM_ESCALATION")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Escalation implements ContainerManagedTimestampTrackable, JSON, Serializable {
    public static final String JSON_NAME = "escalation";

    public static JSONObject getJSON(Escalation e) throws JSONException {
        if (e == null)
            return null;

        return new JSONObject().put(e.getJsonName(), e.toJSON());
    }

    @ElementCollection(fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(name = "EAM_ESCALATION_ACTION", joinColumns = { @JoinColumn(name = "ESCALATION_ID") })
    @IndexColumn(name = "IDX")
    private List<EscalationAction> actions = new ArrayList<EscalationAction>();

    @Column(name = "CTIME", nullable = false)
    private long creationTime;

    @Column(name = "DESCRIPTION", length = 250)
    private String description;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MAX_WAIT_TIME", nullable = false)
    private long maxPauseTime;

    @Column(name = "MTIME", nullable = false)
    private long modifiedTime;

    @Column(name = "NAME", nullable = false, length = 200, unique = true)
    private String name;

    @Column(name = "NOTIFY_ALL", nullable = false)
    private boolean notifyAll;

    @Column(name = "ALLOW_PAUSE", nullable = false)
    private boolean pauseAllowed;

    @Column(name = "FREPEAT", nullable = false)
    private boolean repeat;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected Escalation() {
    }

    public Escalation(String name, String description, boolean pauseAllowed, long maxPauseTime,
                      boolean notifyAll, boolean repeat) {
        this.name = name;
        this.description = description;
        this.pauseAllowed = pauseAllowed;
        this.maxPauseTime = maxPauseTime;
        this.notifyAll = notifyAll;
        creationTime = System.currentTimeMillis();
        modifiedTime = creationTime;
        this.repeat = repeat;
    }

    protected EscalationAction addAction(long waitTime, Action a) {
        EscalationAction res = new EscalationAction(this, a, waitTime);

        getActionsList().add(res);
        return res;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Escalation other = (Escalation) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * Find an escalation action based on the ID of its associated action.
     */
    public EscalationAction getAction(Integer id) {
        List<EscalationAction> a = getActions();

        for (Iterator<EscalationAction> i = a.iterator(); i.hasNext();) {
            EscalationAction ea = (EscalationAction) i.next();

            if (ea.getAction().getId().equals(id))
                return ea;
        }
        return null;
    }

    public List<EscalationAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    protected List<EscalationAction> getActionsList() {
        return actions;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
    }

    public String getJsonName() {
        return JSON_NAME;
    }

    public long getMaxPauseTime() {
        return maxPauseTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean isNotifyAll() {
        return notifyAll;
    }

    public boolean isPauseAllowed() {
        return pauseAllowed;
    }

    public boolean isRepeat() {
        return repeat;
    }

    protected void setActionsList(List<EscalationAction> actions) {
        this.actions = actions;
    }

    protected void setCreationTime(long ctime) {
        creationTime = ctime;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setMaxPauseTime(long pauseTime) {
        maxPauseTime = pauseTime;
    }

    public void setModifiedTime(long mtime) {
        modifiedTime = mtime;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setNotifyAll(boolean notifyAll) {
        this.notifyAll = notifyAll;
    }

    protected void setPauseAllowed(boolean allowed) {
        pauseAllowed = allowed;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public JSONObject toJSON() {
        try {
            JSONArray actions = new JSONArray();
            for (Iterator<EscalationAction> i = getActions().iterator(); i.hasNext();) {
                EscalationAction action = (EscalationAction) i.next();

                actions.put(action.toJSON());
            }

            JSONObject json = new JSONObject().put("name", getName())
                .put("description", getDescription() != null ? getDescription() : "")
                .put("allowPause", isPauseAllowed()).put("maxWaitTime", getMaxPauseTime())
                .put("notifyAll", isNotifyAll()).put("creationTime", getCreationTime())
                .put("modifiedTime", getModifiedTime()).put("repeat", isRepeat())
                .put("actions", actions);

            if (getId() != null) {
                json.put("id", getId());
                json.put("_version_", getVersion());
            }
            return json;
        } catch (JSONException e) {
            throw new SystemException(e);
        }
    }

    public String toString() {
        return "(id=" + getId() + ", name=" + name + ", allowPause=" + pauseAllowed +
               ", maxPauseTime=" + maxPauseTime + ", notifyAll=" + notifyAll + ", created=" +
               creationTime + ")";
    }
}
