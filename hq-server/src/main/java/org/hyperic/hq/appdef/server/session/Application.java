package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;

public class Application
    extends AppdefResource {

    private Collection<AppService> appServices = new HashSet<AppService>();

    private ApplicationType applicationType;

    public Collection<AppService> getAppServices() {
        return appServices;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public AppdefEntityID getEntityId() {
        return AppdefEntityID.newAppID(getId());
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
       return applicationType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getApplicationValue();
    }

    @Override
    protected String _getAuthzOp(String op) {
        // TODO Auto-generated method stub
        return null;
    }

    public ApplicationValue getApplicationValue() {
        ApplicationValue applicationValue = new ApplicationValue();
        // applicationValue.setName(getName());
        // applicationValue.setSortName(getSortName());
        // applicationValue.setModifiedBy(getModifiedBy());
        // applicationValue.setOwner(getResource().getOwner().getName());
        // applicationValue.setLocation(getLocation());
        // applicationValue.setEngContact(getEngContact());
        // applicationValue.setOpsContact(getOpsContact());
        // applicationValue.setBusinessContact(getBusinessContact());
        // applicationValue.setDescription(getDescription());
        // applicationValue.setId(getId());
        // applicationValue.setMTime(getMTime());
        // applicationValue.setCTime(getCTime());
        // applicationValue.removeAllAppServiceValues();
        // if (getAppServices() != null) {
        // Iterator iAppServiceValue = getAppServices().iterator();
        // while (iAppServiceValue.hasNext()) {
        // applicationValue.addAppServiceValue(((AppService)
        // iAppServiceValue.next())
        // .getAppServiceValue());
        // }
        // }
        // applicationValue.cleanAppServiceValue();
        // if (getApplicationType() != null)
        // applicationValue.setApplicationType(getApplicationType());
        // else
        // applicationValue.setApplicationType(null);
        return applicationValue;
    }

}
