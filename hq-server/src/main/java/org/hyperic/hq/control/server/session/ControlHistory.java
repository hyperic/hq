/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.control.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.control.shared.ControlConstants;

@Entity
@Table(name="EAM_CONTROL_HISTORY")
public class ControlHistory implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;

    @Column(name="GROUP_ID")
    private Integer groupId;
    
    @Column(name="BATCH_ID")
    private Integer batchId;
    
    @Column(name="ENTITY_TYPE",nullable=false)
    private Integer entityType;
    
    @Column(name="ENTITY_ID",nullable=false)
    private Integer entityId;
    
    private transient String entityName;
    
    @Column(name="SUBJECT",nullable=false,length=32)
    private String subject;
    
    @Column(name="SCHEDULED",nullable=false)
    private boolean scheduled;
    
    @Column(name="DATE_SCHEDULED",nullable=false)
    private long dateScheduled;
    
    @Column(name="STARTTIME",nullable=false)
    @Index(name="CTL_HISTORY_STARTTIME_IDX")
    private long startTime;
    
    @Column(name="STATUS",nullable=false,length=64)
    private String status;
    
    @Column(name="ENDTIME",nullable=false)
    private long endTime;
    
    @Column(name="DESCRIPTION",length=500)
    private String description;
    
    @Column(name="MESSAGE",length=500)
    private String message;
    
    @Column(name="ACTION",length=32,nullable=false)
    private String action;
    
    @Column(name="ARGS",length=500)
    private String args;

    /**
     * default constructor
     */
    public ControlHistory()
    {
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



    public Integer getGroupId()
    {
        return this.groupId;
    }

    protected void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public Integer getBatchId()
    {
        return this.batchId;
    }

    protected void setBatchId(Integer batchId)
    {
        this.batchId = batchId;
    }

    public Integer getEntityType()
    {
        return this.entityType;
    }

    protected void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public Integer getEntityId()
    {
        return this.entityId;
    }

    protected void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public String getSubject()
    {
        return this.subject;
    }

    protected void setSubject(String subject)
    {
        this.subject = subject;
    }

    public boolean isScheduled()
    {
        return this.scheduled;
    }

    protected void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
    }

    public Boolean getScheduled()
    {
        return new Boolean(isScheduled());
    }

    protected void setScheduled(Boolean scheduled)
    {
        setScheduled(scheduled.booleanValue());
    }

    public long getDateScheduled()
    {
        return this.dateScheduled;
    }

    protected void setDateScheduled(long dateScheduled)
    {
        this.dateScheduled = dateScheduled;
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    protected void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public String getStatus()
    {
        return this.status;
    }

    protected void setStatus(String status)
    {
        this.status = status;
    }

    public long getEndTime()
    {
        return this.endTime;
    }

    protected void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public long getDuration()
    {
        if (getStatus().equals(ControlConstants.STATUS_INPROGRESS)) {
            return System.currentTimeMillis() - getStartTime();
        } else {
            return getEndTime() - getStartTime();
        }
    }

    public String getDescription()
    {
        return this.description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public String getMessage()
    {
        return this.message;
    }

    protected void setMessage(String message)
    {
        this.message = message;
    }

    public String getAction()
    {
        return this.action;
    }

    protected void setAction(String action)
    {
        this.action = action;
    }

    public String getArgs()
    {
        return this.args;
    }

    protected void setArgs(String args)
    {
        this.args = args;
    }

    private String getEntityIdString()
    {
        AppdefEntityID id = new AppdefEntityID(getEntityType().intValue(),
                                               getEntityId());
        return id.getAppdefKey();
    }

    public String getEntityName() {
        if (entityName == null)
            return getEntityIdString();
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof ControlHistory) && super.equals(obj);
    }
    
    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }
}


