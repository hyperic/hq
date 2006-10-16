package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;

import java.io.Serializable;

/**
 * This is the base abstract class for all appdef pojos.
 * This is modeled after the AppdefEntityBean less the EJB code.
 */
public abstract class AppdefBean implements Serializable
{
    protected Integer id;
    protected Long creationTime;
    protected Long modifiedTime;

    // for hibernate optimistic locks
    // don't mess with this.
    protected long _version_;

    // legacy stuff, do we really need this?
    protected Integer cid;

    protected AppdefBean()
    {
        super();
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getId()
    {
        return id;
    }

    public long getCreationTime()
    {
        return creationTime != null ? creationTime.longValue() : 0;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public long getModifiedTime()
    {
        return modifiedTime != null ? modifiedTime.longValue() : 0;
    }

    public void setModifiedTime(Long modifiedTime)
    {
        this.modifiedTime = modifiedTime;
    }

    // for legacy EJB assessor
    /**
     * @deprecated
     * @return
     */
    public Long getCTime()
    {
        return creationTime;
    }
    /**
     * @deprecated
     * @return
     */
    public Long getMTime()
    {
        return modifiedTime;
    }
    // end legacy EJB assessors

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

    /**
     * legacy EJB entity bean code
     * @param obj
     * @return
     */
    public boolean matchesValueObject(AppdefResourceValue obj)
    {
        boolean matches = true;
        if (obj.getId() != null) {
            matches = (obj.getId().intValue() == this.getId().intValue());
        } else {
            matches = (this.getId() == null);
        }
        if (obj.getCTime() != null) {
            matches = (obj.getCTime().floatValue() == getCTime().floatValue());
        } else {
            matches = (this.getCreationTime() == 0);
        }
        return matches;
    }
}
