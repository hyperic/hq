package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.ApplicationValue;

public class Application {

    private Integer id;

    private String name;
    
    private Collection<AppService> appServices = new HashSet<AppService>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    

    public Collection<AppService> getAppServices() {
        return appServices;
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
