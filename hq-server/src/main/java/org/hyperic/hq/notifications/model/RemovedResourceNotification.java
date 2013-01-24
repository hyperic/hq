package org.hyperic.hq.notifications.model;

import org.hyperic.hq.authz.server.session.Resource;

public class RemovedResourceNotification extends InventoryNotification {
    public RemovedResourceNotification(Resource r) {
        super(r);
    }
}
