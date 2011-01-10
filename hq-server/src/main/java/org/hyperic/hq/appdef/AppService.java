package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.inventory.domain.ResourceGroup;

public class AppService
    extends AppdefBean {

    private Service service;

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isIsGroup() {
        // TODO remove - this is now a single service in an App, not a "cluster"
        return false;
    }

    public ResourceGroup getResourceGroup() {
        // TODO remove. Shouldn't matter that we are returning null b/c above
        // always returns false
        return null;
    }

    public boolean isEntryPoint() {
        // TODO setApplicationServices used to set this to false for everything
        // but groups. How used?
        return false;
    }

    public AppServiceValue getAppServiceValue() {
        AppServiceValue appServiceValue = new AppServiceValue();
        appServiceValue.setIsEntryPoint(isEntryPoint());
        appServiceValue.setId(getId());
        appServiceValue.setMTime(getMTime());
        appServiceValue.setCTime(getCTime());
        appServiceValue.setService(getService());
        appServiceValue.setServiceType(service.getServiceType());
        return appServiceValue;
    }
}
