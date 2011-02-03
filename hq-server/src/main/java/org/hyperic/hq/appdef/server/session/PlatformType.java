package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;

public class PlatformType extends AppdefResourceType{

    private String plugin;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    }

    @Override
    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
       return getPlatformTypeValue();
    }

    public PlatformTypeValue getPlatformTypeValue() {
        PlatformTypeValue platformTypeValue = new PlatformTypeValue();
        platformTypeValue.setSortName(getSortName());
        platformTypeValue.setName(getName());
        platformTypeValue.setDescription(getDescription());
        platformTypeValue.setPlugin(getPlugin());
        platformTypeValue.setId(getId());
        platformTypeValue.setMTime(getMTime());
        platformTypeValue.setCTime(getCTime());
        return platformTypeValue;
    }

}
