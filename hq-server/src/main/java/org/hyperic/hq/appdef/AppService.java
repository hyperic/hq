package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AppServiceValue;

public class AppService {
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AppServiceValue getAppServiceValue() {
        AppServiceValue appServiceValue = new AppServiceValue();
        // appServiceValue.setIsCluster(isIsGroup());
        // appServiceValue.setIsEntryPoint(isEntryPoint());
        // appServiceValue.setId(getId());
        // appServiceValue.setMTime(getMTime());
        // appServiceValue.setCTime(getCTime());
        // if (getService() != null)
        // appServiceValue.setService(getService());
        // else
        // appServiceValue.setService(null);
        // if (getServiceType() != null)
        // appServiceValue.setServiceType(getServiceType());
        // else
        // appServiceValue.setServiceType(null);
        return appServiceValue;
    }
}
