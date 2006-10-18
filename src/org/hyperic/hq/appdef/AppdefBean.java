package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;

import java.io.Serializable;

import org.hyperic.hibernate.PersistedObject;

/**
 * This is the base abstract class for all appdef pojos.
 * This is modeled after the AppdefEntityBean less the EJB code.
 */
public abstract class AppdefBean 
    extends PersistedObject
    implements Serializable
{
    // XXX -- Can we make these private?  We have accessors.  -- JMT
    protected Long creationTime;
    protected Long modifiedTime;

    // legacy stuff, do we really need this?
    protected Integer cid;

    protected AppdefBean() {
        super();
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

    public boolean equals(Object obj)
    {
        if (!super.equals(obj) || !(obj instanceof AppdefBean)) {
            return false;
        }
        AppdefBean o = (AppdefBean)obj;
        return
            (creationTime!=null && o.getCTime()!=null && creationTime.equals(o));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result +
                 (creationTime != null ? creationTime.hashCode() : 0);

        return result;
    }

}
