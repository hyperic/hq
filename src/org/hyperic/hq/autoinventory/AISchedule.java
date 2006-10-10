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