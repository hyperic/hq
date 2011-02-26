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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="QRTZ_FIRED_TRIGGERS")
public class QzFiredTrigger  implements Serializable {

    @Id
    @Column(name="ENTRY_ID",length=95,nullable=false)
    private String entryId;
    
    @Column(name="TRIGGER_NAME",nullable=false,length=200)
    private String triggerName;
    
    @Column(name="TRIGGER_GROUP",nullable=false,length=200)
    private String triggerGroup;
    
    @Column(name="INSTANCE_NAME",nullable=false,length=200)
    private String instanceName;
    
    @Column(name="FIRED_TIME",nullable=false)
    private long firedTime;
    
    @Column(name="STATE",nullable=false,length=16)
    private String state;
    
    @Column(name="IS_VOLATILE",nullable=false)
    private boolean isVolatile;
    
    @Column(name="JOB_NAME",length=200)
    private String jobName;
    
    @Column(name="JOB_GROUP",length=200)
    private String jobGroup;
    
    @Column(name="IS_STATEFUL")
    private boolean stateful;
    
    @Column(name="REQUESTS_RECOVERY")
    private boolean requestsRecovery;
    
    @Column(name="PRIORITY",nullable=false)
    private int     priority;

  
    public QzFiredTrigger() {
    }

    public String getEntryId() {
        return entryId;
    }
    
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getTriggerName() {
        return triggerName;
    }
    
    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }
    
    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    public String getInstanceName() {
        return instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public long getFiredTime() {
        return firedTime;
    }
    
    public void setFiredTime(long firedTime) {
        this.firedTime = firedTime;
    }

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }

    public boolean isIsVolatile() {
        return isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        this.isVolatile = isVolatile;
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

    public boolean isStateful() {
        return stateful;
    }
    
    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public boolean isRequestsRecovery() {
        return requestsRecovery;
    }
    
    public void setRequestsRecovery(boolean requestsRecovery) {
        this.requestsRecovery = requestsRecovery;
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
        result = prime * result + ((entryId == null) ? 0 : entryId.hashCode());
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
        QzFiredTrigger other = (QzFiredTrigger) obj;
        if (entryId == null) {
            if (other.entryId != null)
                return false;
        } else if (!entryId.equals(other.entryId))
            return false;
        return true;
    }

}
