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
@Table(name="EAM_ESCALATION")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Escalation implements ContainerManagedTimestampTrackable, JSON, Serializable
{
    public static final String JSON_NAME = "escalation";
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="NAME",nullable=false,length=200,unique=true)
    private String name;

    @Column(name="DESCRIPTION",length=250)
    private String description;

    @Column(name="ALLOW_PAUSE",nullable=false)
    private boolean pauseAllowed;

    @Column(name="MAX_WAIT_TIME",nullable=false)
    private long maxPauseTime;

    @Column(name="NOTIFY_ALL",nullable=false)
    private boolean notifyAll;
    
    @Column(name="FREPEAT",nullable=false)
    private boolean repeat;

    @Column(name="CTIME",nullable=false)
    private long creationTime;
    
    @Column(name="MTIME",nullable=false)
    private long modifiedTime;
    
    @ElementCollection(fetch=FetchType.LAZY)
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(name="EAM_ESCALATION_ACTION",joinColumns = {@JoinColumn(name="ESCALATION_ID")})
    @IndexColumn(name="IDX")
    private List<EscalationAction> actions = new ArrayList<EscalationAction>();

    protected Escalation() {}

    Escalation(String name, String description, boolean pauseAllowed,
                long maxPauseTime, boolean notifyAll, boolean repeat)
    {
        this.name         = name;
        this.description  = description;
        this.pauseAllowed = pauseAllowed;
        this.maxPauseTime = maxPauseTime;
        this.notifyAll    = notifyAll;
        creationTime        = System.currentTimeMillis();
        modifiedTime        = creationTime;
        this.repeat       = repeat;
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public boolean isPauseAllowed() {
        return pauseAllowed;
    }

    protected void setPauseAllowed(boolean allowed) {
        pauseAllowed = allowed;
    }

    public long getMaxPauseTime() {
        return maxPauseTime;
    }

    protected void setMaxPauseTime(long pauseTime) {
        maxPauseTime = pauseTime;
    }

    public boolean isNotifyAll() {
        return notifyAll;
    }

    protected void setNotifyAll(boolean notifyAll) {
        this.notifyAll = notifyAll;
    }

    public long getCreationTime() {
        return creationTime;
    }

    protected void setCreationTime(long ctime) {
        creationTime = ctime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long mtime) {
        modifiedTime = mtime;
    }

    public List<EscalationAction> getActions() {
        return Collections.unmodifiableList(actions);
    }
    
    protected List<EscalationAction> getActionsList() {
        return actions;
    }

    protected void setActionsList(List<EscalationAction> actions) {
        this.actions = actions;
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
        List<EscalationAction> a = getActions();
        
        for (Iterator<EscalationAction> i=a.iterator(); i.hasNext(); ) {
            EscalationAction ea = (EscalationAction)i.next();
            
            if (ea.getAction().getId().equals(id))
                return ea;
        }
        return null;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public JSONObject toJSON() {
        try {
            JSONArray actions = new JSONArray();
            for (Iterator<EscalationAction> i = getActions().iterator(); i.hasNext(); ) {
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
                .put("repeat", isRepeat())
                .put("actions", actions);

            if (getId() != null) {
                json.put("id", getId());
                json.put("_version_", getVersion());
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
        return "(id=" + getId() + ", name=" + name + ", allowPause=" +
            pauseAllowed + ", maxPauseTime=" + maxPauseTime + ", notifyAll=" +
            notifyAll + ", created=" + creationTime + ")";
    }
}
