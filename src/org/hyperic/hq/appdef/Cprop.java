package org.hyperic.hq.appdef;

import java.io.Serializable;

/**
 *
 */
public class Cprop implements Serializable
{
    private Integer id;
    private long _version_;
    private Integer key;
    private Integer appdefId;
    private Integer valueIdx;
    private String propValue;

    // Constructors

    /**
     * default constructor
     */
    public Cprop()
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

    public Integer getKey()
    {
        return this.key;
    }

    public void setKey(Integer keyId)
    {
        this.key = keyId;
    }

    public Integer getAppdefId()
    {
        return this.appdefId;
    }

    public void setAppdefId(Integer appdefId)
    {
        this.appdefId = appdefId;
    }

    public Integer getValueIdx()
    {
        return this.valueIdx;
    }

    public void setValueIdx(Integer valueIdx)
    {
        this.valueIdx = valueIdx;
    }

    public String getPropValue()
    {
        return this.propValue;
    }

    public void setPropValue(String propValue)
    {
        this.propValue = propValue;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof Cprop)) return false;
        Cprop castOther = (Cprop) other;

        return ((this.getKey() == castOther.getKey()) || (this.getKey() != null && castOther.getKey() != null && this.getKey().equals(castOther.getKey())))
               && ((this.getAppdefId() == castOther.getAppdefId()) || (this.getAppdefId() != null && castOther.getAppdefId() != null && this.getAppdefId().equals(castOther.getAppdefId())))
               && ((this.getValueIdx() == castOther.getValueIdx()) || (this.getValueIdx() != null && castOther.getValueIdx() != null && this.getValueIdx().equals(castOther.getValueIdx())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }

}
