package org.hyperic.hq.notifications.model;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;

public class CreatedResourceNotification extends InventoryNotification {
    protected AppdefEntityID parentID;
    
    public CreatedResourceNotification(AppdefEntityID parentID, Resource r) {
        super(r);
        this.parentID = parentID;
    }
    public AppdefEntityID getParentID() {
        return parentID;
    }
}
