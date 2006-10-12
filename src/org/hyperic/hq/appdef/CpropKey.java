package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.CPropKeyValue;
import org.hyperic.hq.appdef.shared.CPropKeyPK;

import java.io.Serializable;
import java.util.Collection;

/**
 *
 */
public class CpropKey implements Serializable
{
    private Integer id;
    private long _version_;
    private Integer appdefType;
    private Integer appdefTypeId;
    private String key;
    private String description;

    public Collection getCprops()
    {
        return cprops;
    }

    public void setCprops(Collection cprops)
    {
        this.cprops = cprops;
    }

    private Collection cprops;

    /**
     * default constructor
     */
    public CpropKey()
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

    public void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public Integer getAppdefType()
    {
        return this.appdefType;
    }

    public void setAppdefType(Integer appdefType)
    {
        this.appdefType = appdefType;
    }

    public Integer getAppdefTypeId()
    {
        return this.appdefTypeId;
    }

    public void setAppdefTypeId(Integer appdefTypeId)
    {
        this.appdefTypeId = appdefTypeId;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String propKey)
    {
        this.key = propKey;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    private CPropKeyValue _value = new CPropKeyValue();
    /**
     * for legacy EJB Entity Bean compatibility
     * @return
     */
    public CPropKeyValue getCPropKeyValue()
    {
        _value.setAppdefType(appdefType == null ? 0 : appdefType.intValue());
        _value.setAppdefTypeId(appdefTypeId == null ? 0 : appdefTypeId.intValue());
        _value.setDescription(description == null ? "" : description);
        _value.setKey(key == null ? "" : key);
        _value.setId(id);
        return _value;
    }

    private CPropKeyPK _pkey = new CPropKeyPK();
    public CPropKeyPK getPrimaryKey()
    {
        _pkey.setId(getId());
        return _pkey;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof CpropKey)) return false;
        CpropKey castOther = (CpropKey) other;

        return ((this.getAppdefType() == castOther.getAppdefType()) || (this.getAppdefType() != null && castOther.getAppdefType() != null && this.getAppdefType().equals(castOther.getAppdefType())))
               && ((this.getAppdefTypeId() == castOther.getAppdefTypeId()) || (this.getAppdefTypeId() != null && castOther.getAppdefTypeId() != null && this.getAppdefTypeId().equals(castOther.getAppdefTypeId())))
               && ((this.getKey() == castOther.getKey()) || (this.getKey() != null && castOther.getKey() != null && this.getKey().equals(castOther.getKey())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }

}
