package org.hyperic.hq.appdef;

/**
 *
 */
public abstract class AppdefResourceType extends AppdefBean
{
    protected String name;
    protected String sortName;
    protected String description;

    /**
     * default constructor
     */
    public AppdefResourceType()
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

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        this.sortName = sortName;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
