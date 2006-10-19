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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;

/**
 *
 */
public class AISchedule implements Serializable
{
    private Integer id;
    private long _version_;
    private Integer entityType;
    private Integer entityId;
    private String subject;
    private byte[] scheduleValueBytes;
    private long nextFireTime;
    private String triggerName;
    private String jobName;
    private String jobOrderData;
    private String scanName;
    private String scanDesc;
    private byte[] config;

    /**
     * default constructor
     */
    public AISchedule()
    {
        super();
    }

    // Property accessors
    public Integer getId()
    {
        return this.id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    public long get_version_()
    {
        return this._version_;
    }

    private void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public Integer getEntityType()
    {
        return this.entityType;
    }

    public void setEntityType(Integer entityType)
    {
        this.entityType = entityType;
    }

    public Integer getEntityId()
    {
        return this.entityId;
    }

    public void setEntityId(Integer entityId)
    {
        this.entityId = entityId;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public byte[] getScheduleValueBytes()
    {
        return this.scheduleValueBytes;
    }

    public void setScheduleValueBytes(byte[] scheduleValueBytes)
    {
        this.scheduleValueBytes = scheduleValueBytes;
    }

    public long getNextFireTime()
    {
        return this.nextFireTime;
    }

    public void setNextFireTime(long nextFireTime)
    {
        this.nextFireTime = nextFireTime;
    }

    public String getTriggerName()
    {
        return this.triggerName;
    }

    public void setTriggerName(String triggerName)
    {
        this.triggerName = triggerName;
    }

    public String getJobName()
    {
        return this.jobName;
    }

    public void setJobName(String jobName)
    {
        this.jobName = jobName;
    }

    public String getJobOrderData()
    {
        return this.jobOrderData;
    }

    public void setJobOrderData(String jobOrderData)
    {
        this.jobOrderData = jobOrderData;
    }

    public String getScanName()
    {
        return this.scanName;
    }

    public void setScanName(String scanName)
    {
        this.scanName = scanName;
    }

    public String getScanDesc()
    {
        return this.scanDesc;
    }

    public void setScanDesc(String scanDesc)
    {
        this.scanDesc = scanDesc;
    }

    public byte[] getConfig()
    {
        return this.config;
    }

    public void setConfig(byte[] config)
    {
        this.config = config;
    }
}