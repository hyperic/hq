package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class ApplicationType extends AppdefResourceType
{
    private Collection services;
    private Collection applications;

    /**
     * default constructor
     */
    public ApplicationType()
    {
        super();
    }

    // Property accessors
    public Collection getServices()
    {
        return this.services;
    }

    public void setServices(Collection services)
    {
        this.services = services;
    }

    public Collection getApplications()
    {
        return this.applications;
    }

    public void setApplications(Collection applications)
    {
        this.applications = applications;
    }
}
