package org.hyperic.hq.autoinventory;

import java.io.Serializable;

/**
 *
 */
public class AIHistory implements Serializable
{
    private Integer id;
    private long _version_;
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

    // Property accessors
    public Integer getId()
    {
        return this.id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    private void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public long get_version_()
    {
        return this._version_;
    }

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

    public void setScheduled(boolean scheduled)
    {
        this.scheduled = scheduled;
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
}
