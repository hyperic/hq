package org.hyperic.hq.appdef.server.session;

import java.util.Map;

import org.hyperic.hq.bizapp.shared.AllConfigDiff;

public class ResourceContentChangedEvent extends InventoryEvent {
    protected Integer resourceID;
    protected AllConfigDiff allConfigs;
    protected Map<String, String> cProps;
    
    public ResourceContentChangedEvent(Integer rid, AllConfigDiff allConfigs, Map<String, String> cProps) {
        this.resourceID=rid;
        this.allConfigs=allConfigs;
        this.cProps=cProps;
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
}
