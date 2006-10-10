package org.hyperic.hq.appdef;

import java.io.Serializable;

/**
 * This is the base abstract class for all appdef pojos.
 * This is modeled after the AppdefEntityBean less the EJB code.
 */
public abstract class AppdefBean implements Serializable
{
    protected Integer id;
    protected long creationTime;
    protected long modifiedTime;

    // for hibernate optimistic locks
    // don't mess with this.
    protected long _version_;

    // legacy stuff, do we really need this?
    protected Integer cid;

    protected AppdefBean()
    {
        super();
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getId()
    {
        return id;
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(long creationTime)
    {
        this.creationTime = creationTime;
    }

    public long getModifiedTime()
    {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime)
    {
        this.modifiedTime = modifiedTime;
    }

    public long get_version_()
    {
        return _version_;
    }

    private void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public Integer getCid()
    {
        return cid;
    }

    public void setCid(Integer cid)
    {
        this.cid = cid;
    }
}
