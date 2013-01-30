package org.hyperic.hq.api.transfer;

import javax.jms.Destination;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.notifications.UnregisteredException;

public interface NotificationsTransfer {
    public NotificationsReport poll(ApiMessageContext apiMessageContext) throws UnregisteredException;
    public void unregister();
    public Destination getDummyDestination();
}
