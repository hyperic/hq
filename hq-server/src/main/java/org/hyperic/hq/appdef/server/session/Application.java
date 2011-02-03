package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;

public class Application
    extends AppdefResource {

    private Collection<AppService> appServices = new HashSet<AppService>();

    private ApplicationType applicationType;

    private String engContact;

    private String opsContact;

    private String businessContact;

    public Collection<AppService> getAppServices() {
        return appServices;
    }
    
    public void addAppService(AppService appService) {
        appServices.add(appService);
    }

    @Override
    public AppdefEntityID getEntityId() {
        return AppdefEntityID.newAppID(getId());
    }
    
    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
        return applicationType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getApplicationValue();
    }

    public String getEngContact() {
        return engContact;
    }

    public void setEngContact(String engContact) {
        this.engContact = engContact;
    }

    public String getOpsContact() {
        return opsContact;
    }

    public void setOpsContact(String opsContact) {
        this.opsContact = opsContact;
    }

    public String getBusinessContact() {
        return businessContact;
    }

    public void setBusinessContact(String businessContact) {
        this.businessContact = businessContact;
    }

    public ApplicationValue getApplicationValue() {
        ApplicationValue applicationValue = new ApplicationValue();
        applicationValue.setName(getName());
        applicationValue.setSortName(getSortName());
        applicationValue.setModifiedBy(getModifiedBy());
        applicationValue.setOwner(getResource().getOwner().getName());
        applicationValue.setLocation(getLocation());
        applicationValue.setEngContact(getEngContact());
        applicationValue.setOpsContact(getOpsContact());
        applicationValue.setBusinessContact(getBusinessContact());
        applicationValue.setDescription(getDescription());
        applicationValue.setId(getId());
        applicationValue.setMTime(getMTime());
        applicationValue.setCTime(getCTime());
        applicationValue.removeAllAppServiceValues();
        if (getAppServices() != null) {
            Iterator<AppService> iAppServiceValue = getAppServices().iterator();
            while (iAppServiceValue.hasNext()) {
                applicationValue.addAppServiceValue(((AppService) iAppServiceValue.next())
                    .getAppServiceValue());
            }
        }
        applicationValue.cleanAppServiceValue();
        applicationValue.setApplicationType(applicationType);
        return applicationValue;
    }

}
