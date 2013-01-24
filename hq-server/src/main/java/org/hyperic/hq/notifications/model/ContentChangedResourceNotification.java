package org.hyperic.hq.notifications.model;

import org.hyperic.hq.authz.server.session.Resource;

public class ContentChangedResourceNotification extends InventoryNotification {
    public ContentChangedResourceNotification(Resource r) {
        super(r);
    }
}
