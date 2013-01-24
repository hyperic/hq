package org.hyperic.hq.notifications.model;

import org.hyperic.hq.authz.server.session.Resource;

public abstract class InventoryNotification extends BaseNotification {
    protected Resource r;

    public InventoryNotification(Resource r) {
        this.r=r;
    }
    public Resource getResource() {
        return r;
    }
}
