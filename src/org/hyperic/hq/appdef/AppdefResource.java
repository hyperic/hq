package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

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
        }
    }

    private AppdefEntityID appdefEntityId;
    /**
     * Get the appdefEntityId for this platform
     * legacy code from EJB entity bean
     */
    public AppdefEntityID getEntityId() {
        if(appdefEntityId == null) {
            appdefEntityId = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                        getId().intValue());
        } else if (!appdefEntityId.getId().equals(getId())) {
            // Sometimes the id object can get stale if this entity bean is
            // being reused from the bean pool and was previously used by
            // a different object.
            appdefEntityId = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                        getId().intValue());
        }
        return appdefEntityId;
    }
}
