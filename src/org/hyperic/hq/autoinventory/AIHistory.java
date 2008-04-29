/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2008], Hyperic, Inc. 
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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.autoinventory.shared.AIHistoryValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Pojo for hibernate hbm mapping file
 */
public class AIHistory extends PersistedObject
{
    private Integer groupId;
    private Integer batchId;
    private Integer entityType;
    private Integer entityId;
    private String subject;
    private boolean scheduled;
    private long dateScheduled;
    private long startTime;
    private String status;
    private long endTime;
    private long duration;
    private String scanName;
    private String scanDesc;
    private String description;
    private String message;
    private byte[] config;

    /**
     * default constructor
     */
    public AIHistory()
    {
        super();
    }

    public AIHistory(AIHistoryValue av)
    {
        super();
        setAIHistoryValue(av);
    }

    // Property accessors
    public Integer getGroupId()
    {
        return this.groupId;
    }

    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public Integer getBatchId()
    {
        return this.batchId;
    }

    public void setBatchId(Integer batchId)
    {
        this.batchId = batchId;
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

    public boolean isScheduled()
    {
        return this.scheduled;
    }

    public Boolean getScheduled()
    {
        return new Boolean(isScheduled());
    }

    public void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
    }

    public void setScheduled(Boolean scheduled)
    {
        setScheduled(scheduled.booleanValue());
    }

    public long getDateScheduled()
    {
        return this.dateScheduled;
    }

    public void setDateScheduled(long dateScheduled)
    {
        this.dateScheduled = dateScheduled;
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public String getStatus()
    {
        return this.status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public long getEndTime()
    {
        return this.endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public long getDuration()
    {
        return this.duration;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
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

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public byte[] getConfig()
    {
        return this.config;
    }

    public void setConfig(byte[] config)
    {
        this.config = config;
    }

    public String getEntityName()
    {
        AppdefEntityID id = new AppdefEntityID(getEntityType().intValue(),
                                               getEntityId());
        return id.toString();
    }

    public void setEntityName()
    {
        // no op
    }

    private AIHistoryValue aIHistoryValue =  new AIHistoryValue();
    /**
     * legacy DTO pattern
     * @deprecated use (this) AIHistory object
     * @return
     */
    public AIHistoryValue getAIHistoryValue()
    {
        try {
            aIHistoryValue.setId(getId());
            aIHistoryValue.setGroupId(getGroupId());
            aIHistoryValue.setBatchId(getBatchId());
            aIHistoryValue.setEntityType(getEntityType());
            aIHistoryValue.setEntityId(getEntityId());
            aIHistoryValue.setEntityName(
                (getEntityName() == null) ? "" : getEntityName());
            aIHistoryValue.setSubject(
                (getSubject() == null) ? "" : getSubject());
            aIHistoryValue.setScheduled(getScheduled());
            aIHistoryValue.setDateScheduled(getDateScheduled());
            aIHistoryValue.setStartTime(getStartTime());
            aIHistoryValue.setEndTime(getEndTime());
            aIHistoryValue.setDuration(getDuration());
            aIHistoryValue.setMessage(
                (getMessage() == null) ? "" : getMessage());
            aIHistoryValue.setDescription(
                (getDescription() == null) ? "" : getDescription());
            aIHistoryValue.setStatus(
                (getStatus() == null) ? "" : getStatus());
            aIHistoryValue.setScanName(
                (getScanName() == null) ? "" : getScanName());
            aIHistoryValue.setScanDesc(
                (getScanDesc() == null) ? "" : getScanDesc());
            aIHistoryValue.setConfig(getConfig());
            aIHistoryValue.setConfigObj(getConfigObj());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return aIHistoryValue;
    }

    public void setAIHistoryValue(AIHistoryValue valueHolder)
    {
        try {
            setGroupId( valueHolder.getGroupId() );
            setBatchId( valueHolder.getBatchId() );
            setEntityType( valueHolder.getEntityType() );
            setEntityId( valueHolder.getEntityId() );
            setSubject( valueHolder.getSubject() );
            setScheduled( valueHolder.getScheduled() );
            setDateScheduled( valueHolder.getDateScheduled() );
            setStartTime( valueHolder.getStartTime() );
            setEndTime( valueHolder.getEndTime() );
            setDuration( valueHolder.getDuration() );
            setMessage( valueHolder.getMessage() );
            setDescription( valueHolder.getDescription() );
            setStatus( valueHolder.getStatus() );
            setScanName( valueHolder.getScanName() );
            setScanDesc( valueHolder.getScanDesc() );
            setConfig( valueHolder.getConfig() );
            setConfigObj( valueHolder.getConfigObj() );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ScanConfigurationCore getConfigObj() throws AutoinventoryException
    {
        return ScanConfigurationCore.deserialize(getConfig());
    }

    public void setConfigObj(ScanConfigurationCore core)
    throws AutoinventoryException
    {
        setConfig(core.serialize());
    }

}
