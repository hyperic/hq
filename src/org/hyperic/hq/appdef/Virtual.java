package org.hyperic.hq.appdef;

/**
 *
 */
public class Virtual
{
    private Integer resourceId;
    private long _version_;
    private Integer processId;
    private Integer physicalId;

    /**
     * default constructor
     */
    public Virtual()
    {
        super();
    }

    // Property accessors
    public Integer getResourceId()
    {
        return this.resourceId;
    }

    public void setResourceId(Integer resourceId)
    {
        this.resourceId = resourceId;
    }

    public long get_version_()
    {
        return this._version_;
    }

    public void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public Integer getProcessId()
    {
        return this.processId;
    }

    public void setProcessId(Integer processId)
    {
        this.processId = processId;
    }

    public Integer getPhysicalId()
    {
        return this.physicalId;
    }

    public void setPhysicalId(Integer physicalId)
    {
        this.physicalId = physicalId;
    }
}
