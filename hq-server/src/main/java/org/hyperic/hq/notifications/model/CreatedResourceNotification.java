package org.hyperic.hq.notifications.model;

import org.hyperic.hq.authz.server.session.Resource;

public class CreatedResourceNotification extends InventoryNotification {
    protected Integer parentID;
    protected Resource r;

    public CreatedResourceNotification(Integer parentID, Resource r) {
        this.parentID = parentID;
        this.r=r;
    }
    public Resource getResource() {
        return r;
    }
    public Integer getParentID() {
        return parentID;
    }
    @Override
    public Integer getResourceID() {
        if (this.r==null) {
            return null;
        }
        return this.r.getId();
    }
}
