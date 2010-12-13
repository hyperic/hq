package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.inventory.domain.ResourceGroup;

public class AppService extends Service {
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
   public Service getService() {
       //TODO remove
       return this;
   }
    
    public boolean isIsGroup() {
        //TODO remove - this is now a single service in an App, not a "cluster"
        return false;
    }
    
    public ResourceGroup getResourceGroup() {
        //TODO remove.  Shouldn't matter that we are returning null b/c above always returns false
        return null;
    }
    
    public boolean isEntryPoint() {
        //TODO I have no idea what this did
        return false;
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
