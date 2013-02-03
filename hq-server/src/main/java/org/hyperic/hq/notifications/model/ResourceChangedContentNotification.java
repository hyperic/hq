package org.hyperic.hq.notifications.model;

import java.util.Map;

public class ResourceChangedContentNotification extends InventoryNotification {
    protected Integer resourceID;
    protected Map<String,String> changedProps;

    public ResourceChangedContentNotification(Integer rid, Map<String,String> changedProps) {
        this.resourceID=rid;
        this.changedProps=changedProps;
    }
    public Integer getResourceID() {
        return resourceID;
    }
    public Map<String,String> getChangedProps() {
        return changedProps;
    }
}