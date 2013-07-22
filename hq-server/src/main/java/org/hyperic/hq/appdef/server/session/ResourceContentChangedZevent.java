package org.hyperic.hq.appdef.server.session;

import java.util.Map;

import org.hyperic.hq.bizapp.shared.AllConfigDiff;

public class ResourceContentChangedZevent extends InventoryEvent {
    protected Integer resourceID;
    protected String resourceName;
    protected AllConfigDiff allConfigs;
    protected Map<String, String> cProps;
    
    public ResourceContentChangedZevent(Integer rid, String resourceName, AllConfigDiff allConfigs, Map<String, String> cProps) {
        this.resourceID=rid;
        this.allConfigs=allConfigs;
        this.cProps=cProps;
        this.resourceName=resourceName;
    }
    public Integer getResourceID() {
        return resourceID;
    }
    public AllConfigDiff getAllConfigs() {
        return allConfigs;
    }
    public Map<String, String> getCProps() {
        return this.cProps;
    }
    public String getResourceName() {
        return resourceName;
    }
}
