package org.hyperic.hibernate;

import org.hyperic.hq.appdef.AppdefBean;

/**
 * Base class for all HQ persisted objects.
 * 
 * Some of these methods are marked as protected.  This allows Hibernate to
 * pull & set values (due to its fancy runtime subclassing), but also 
 * restricts other rogue objects from doing bad things like setting the ID
 * & version #.
 */
public abstract class PersistedObject {
    private Integer _id;

    // for hibernate optimistic locks -- don't mess with this.
    // Named ugly-style since we already use VERSION in some of our tables.
    private long    _version_;

    // XXX -- This is public for now, but should be made more private later
    public void setId(Integer id) {
        _id = id;
    }

    public Integer getId() {
        return _id;
    }

    public long get_version_() {
        return _version_;
    }

    protected void set_version_(long newVer) {
        _version_ = newVer;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof PersistedObject)) {
            return false;
        }
        PersistedObject o = (PersistedObject)obj;
        return
            ((_id == o.getId()) ||
             (_id!=null && o.getId()!=null && _id.equals(o.getId())))
            &&
            (_version_ == o.get_version_());
    }

    public int hashCode()
    {
        int result = 17;

        result = 37*result + (_id != null ? _id.hashCode() : 0);
        result = 37*result + (int)(_version_ ^ (_version_ >>> 32));

        return result;
    }
}
