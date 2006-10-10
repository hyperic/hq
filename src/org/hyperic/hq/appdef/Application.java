package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class Application extends AppdefResource
{
    private String engContact;
    private String opsContact;
    private String busContact;
    private ApplicationType applicationType;
    private Collection appServices;

    /**
     * default constructor
     */
    public Application()
    {
        super();
    }

    public String getEngContact()
    {
        return this.engContact;
    }

    public void setEngContact(String engContact)
    {
        this.engContact = engContact;
    }

    public String getOpsContact()
    {
        return this.opsContact;
    }

    public void setOpsContact(String opsContact)
    {
        this.opsContact = opsContact;
    }

    public String getBusContact()
    {
        return this.busContact;
    }

    public void setBusContact(String busContact)
    {
        this.busContact = busContact;
    }

    public ApplicationType getApplicationType()
    {
        return this.applicationType;
    }

    public void setApplicationType(ApplicationType applicationType)
    {
        this.applicationType = applicationType;
    }

    public Collection getAppServices()
    {
        return this.appServices;
    }

    public void setAppServices(Collection appServices)
    {
        this.appServices = appServices;
    }
}
