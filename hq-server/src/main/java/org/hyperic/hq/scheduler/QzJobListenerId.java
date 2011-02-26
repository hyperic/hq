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
import javax.persistence.Embeddable;

@Embeddable
public class QzJobListenerId  implements Serializable {

    @Column(name="JOB_NAME",nullable=false,length=200)
    private String jobName;
    
    @Column(name="JOB_GROUP",nullable=false,length=200)
    private String jobGroup;
    
    @Column(name="JOB_LISTENER",nullable=false,length=200)
    private String jobListener;

    
    public QzJobListenerId() {
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

    public String getJobListener() {
        return jobListener;
    }
    
    public void setJobListener(String jobListener) {
        this.jobListener = jobListener;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((jobGroup == null) ? 0 : jobGroup.hashCode());
        result = prime * result + ((jobListener == null) ? 0 : jobListener.hashCode());
        result = prime * result + ((jobName == null) ? 0 : jobName.hashCode());
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
        QzJobListenerId other = (QzJobListenerId) obj;
        if (jobGroup == null) {
            if (other.jobGroup != null)
                return false;
        } else if (!jobGroup.equals(other.jobGroup))
            return false;
        if (jobListener == null) {
            if (other.jobListener != null)
                return false;
        } else if (!jobListener.equals(other.jobListener))
            return false;
        if (jobName == null) {
            if (other.jobName != null)
                return false;
        } else if (!jobName.equals(other.jobName))
            return false;
        return true;
    }
    
    
}
