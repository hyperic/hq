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
    public int getAuthzType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
       return getPlatformTypeValue();
    }

    public PlatformTypeValue getPlatformTypeValue() {
        PlatformTypeValue platformTypeValue = new PlatformTypeValue();
//        _platformTypeValue.setSortName(getSortName());
//        _platformTypeValue.setName(getName());
//        _platformTypeValue.setDescription(getDescription());
//        _platformTypeValue.setPlugin(getPlugin());
//        _platformTypeValue.setId(getId());
//        _platformTypeValue.setMTime(getMTime());
//        _platformTypeValue.setCTime(getCTime());
        return platformTypeValue;
    }

}
