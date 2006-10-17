package org.hyperic.hibernate;

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
}
