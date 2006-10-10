package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class TierType extends AppdefBean
{
    private String name;
    private String description;
    private Collection serviceTypes;

    /**
     * default constructor
     */
    public TierType()
    {
        super();
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Collection getServiceTypes()
    {
        return this.serviceTypes;
    }

    public void setServiceTypes(Collection serviceTypes)
    {
        this.serviceTypes = serviceTypes;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof TierType)) return false;
        TierType castOther = (TierType) other;

        return ((this.getName() == castOther.getName()) || (this.getName() != null && castOther.getName() != null && this.getName().equals(castOther.getName())));
    }

    public int hashCode()
    {
        int result = 17;

        return result;
    }
}
