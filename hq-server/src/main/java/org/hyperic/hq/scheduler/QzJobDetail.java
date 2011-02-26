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
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="QRTZ_JOB_DETAILS")
public class QzJobDetail  implements Serializable {

    @EmbeddedId  
    private QzJobDetailId id;
    
    @Column(name="DESCRIPTION",length=250)
    private String description;
    
    @Column(name="JOB_CLASS_NAME",length=250,nullable=false)
    private String jobClassName;
    
    @Column(name="IS_DURABLE",nullable=false)
    private boolean durable;
    
    @Column(name="IS_VOLATILE",nullable=false)
    private boolean isVolatile;
    
    @Column(name="IS_STATEFUL",nullable=false)
    private boolean stateful;
    
    @Column(name="REQUESTS_RECOVERY",nullable=false)
    private boolean requestsRecovery;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="JOB_DATA",columnDefinition="BLOB")
    private byte[] jobData;
    
    @OneToMany(mappedBy="jobDetails",cascade=CascadeType.ALL,fetch=FetchType.LAZY)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<QzJobListener> jobListeners;
    
    @OneToMany(mappedBy="jobDetail",cascade=CascadeType.ALL,fetch=FetchType.LAZY)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<QzTrigger> triggers;

   
    public QzJobDetail() {
    }

 
    public QzJobDetailId getId() {
        return id;
    }
    
    public void setId(QzJobDetailId id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getJobClassName() {
        return jobClassName;
    }
    
    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public boolean isDurable() {
        return durable;
    }
    
    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isIsVolatile() {
        return isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        this.isVolatile = isVolatile;
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

    public byte[] getJobData() {
        return jobData;
    }
    
    public void setJobData(byte[] jobData) {
        this.jobData = jobData;
    }

    public Collection<QzJobListener> getJobListeners() {
        return jobListeners;
    }
    
    public void setJobListeners(Collection<QzJobListener> jobListeners) {
        this.jobListeners = jobListeners;
    }

    public Collection<QzTrigger> getTriggers() {
        return triggers;
    }
    
    public void setTriggers(Collection<QzTrigger> triggers) {
        this.triggers = triggers;
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
        QzJobDetail other = (QzJobDetail) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
    

}
