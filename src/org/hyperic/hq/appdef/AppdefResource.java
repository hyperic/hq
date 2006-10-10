package org.hyperic.hq.appdef;

/**
 * abstract base class for all appdef resources
 */
public abstract class AppdefResource extends AppdefBean
{
    protected String name;
    protected String sortName;
    protected String description;
    protected String modifiedBy;
    protected String owner;
    protected String location;

    /**
     * default constructor
     */
    public AppdefResource()
    {
        super();
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getModifiedBy()
    {
        return this.modifiedBy;
    }

    public void setModifiedBy(String modifiedBy)
    {
        this.modifiedBy = modifiedBy;
    }

    public String getOwner()
    {
        return this.owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getLocation()
    {
        return this.location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    // Property accessors
    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        this.sortName = sortName;
    }
}
