package org.hyperic.hq.appdef.server.session;

public class RemovedResourceEvent extends InventoryEvent {
    protected Integer rid;
    
    public RemovedResourceEvent(Integer rid) {
        super();
        this.rid=rid;
    }
    public Integer getID() {
        return rid;
    }
}
