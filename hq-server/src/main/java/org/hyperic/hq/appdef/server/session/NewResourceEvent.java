package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.authz.server.session.Resource;

public class NewResourceEvent extends InventoryEvent {
    public Integer getParentID() {
        return parentID;
    }

    public Resource getResource() {
        return r;
    }

    protected Integer parentID;
    protected Resource r;
    
    public NewResourceEvent(Integer parentID, Resource r) {
        this.parentID=parentID;
        this.r=r;
    }

}
