package org.hyperic.hq.notifications.model;


public class RemovedResourceNotification extends InventoryNotification {
    protected Integer rid;
    
    public RemovedResourceNotification(Integer rid) {
        this.rid=rid;
    }
    public Integer getResourceID() {
        return rid;
    }
}
