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
        setSortName(name);
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        if (sortName != null) {
            this.sortName = sortName.toUpperCase();
        } else {
            this.sortName = null;
        }
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean equals(Object o)
    {
        if (!super.equals(o) || !(o instanceof AppdefResourceType)) {
            return false;
        }
        AppdefResourceType a = (AppdefResourceType)o;
        return
            ((name==a.getName()) || (name!=null && a.getName()!=null &&
                                     name.equals(a.getName())))
            &&
            ((description==a.getDescription()) ||
             (description!=null && a.getDescription()!=null &&
              description.equals(a.getDescription())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (name!=null ? name.hashCode() : 0);
        result = 37*result + (description!=null ? description.hashCode() : 0);

        return result;
    }
}
