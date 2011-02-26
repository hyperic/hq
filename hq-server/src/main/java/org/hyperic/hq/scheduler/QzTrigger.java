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

package org.hyperic.hq.scheduler;

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="QRTZ_TRIGGERS")
@Inheritance(strategy=InheritanceType.JOINED)
public class QzTrigger  implements java.io.Serializable {

    @EmbeddedId
    private QzTriggerId id;
    
    @Column(name="JOB_NAME",nullable=false,length=200)
    private String jobName;
    
    @Column(name="JOB_GROUP",nullable=false,length=200)
    private String jobGroup;
    
    @Column(name="IS_VOLATILE",nullable=false)
    private boolean isVolatile;
    
    @Column(name="DESCRIPTION",length=250)
    private String description;
    
    @Column(name="NEXT_FIRE_TIME")
    private long nextFireTime;
    
    @Column(name="PREV_FIRE_TIME")
    private long prevFireTime;
    
    @Column(name="TRIGGER_STATE",nullable=false,length=16)
    private String triggerState;
    
    @Column(name="TRIGGER_TYPE",nullable=false,length=8)
    private String triggerType;
    
    @Column(name="START_TIME",nullable=false)
    private long startTime;
    
    @Column(name="END_TIME")
    private long endTime;
    
    @Column(name="CALENDAR_NAME",length=200)
    private String calendarName;
    
    @Column(name="MISFIRE_INSTR")
    private Integer misFireInstr;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="JOB_DATA")
    private byte[] jobData;
    
    @ManyToOne(cascade=CascadeType.ALL,fetch=FetchType.LAZY)
     @JoinColumns({@JoinColumn(name="JOB_NAME",referencedColumnName="JOB_NAME",insertable=false,updatable=false),
        @JoinColumn(name="JOB_GROUP",referencedColumnName="JOB_GROUP",insertable=false,updatable=false)})
    private QzJobDetail jobDetail;
    
    @OneToMany(mappedBy="trigger",cascade=CascadeType.ALL,fetch=FetchType.LAZY)
    private Collection<QzTriggerListener> triggerListeners;
    
    @Column(name="PRIORITY")
    private int     priority;

   
    public QzTrigger() {
    }

  
    public QzTriggerId getId() {
        return id;
    }
    
    public void setId(QzTriggerId id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }
    
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }
    
    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public boolean isIsVolatile() {
        return isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        this.isVolatile = isVolatile;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public long getNextFireTime() {
        return nextFireTime;
    }
    
    public void setNextFireTime(long nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public long getPrevFireTime() {
        return prevFireTime;
    }
    
    public void setPrevFireTime(long prevFireTime) {
        this.prevFireTime = prevFireTime;
    }

    public String getTriggerState() {
        return triggerState;
    }
    
    public void setTriggerState(String triggerState) {
        this.triggerState = triggerState;
    }

    public String getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getCalendarName() {
        return calendarName;
    }
    
    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public Integer getMisFireInstr() {
        return misFireInstr;
    }
    
    public void setMisFireInstr(Integer misFireInstr) {
        this.misFireInstr = misFireInstr;
    }

    public byte[] getJobData() {
        return jobData;
    }
    
    public void setJobData(byte[] jobData) {
        this.jobData = jobData;
    }

    public QzJobDetail getJobDetail() {
        return jobDetail;
    }
    
    public void setJobDetail(QzJobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public Collection<QzTriggerListener> getTriggerListeners() {
        return triggerListeners;
    }
    
    public void setTriggerListeners(Collection<QzTriggerListener> triggerListeners) {
        this.triggerListeners = triggerListeners;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QzTrigger other = (QzTrigger) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
    
}