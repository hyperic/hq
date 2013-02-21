package org.hyperic.hq.api.transfer;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.notifications.UnregisteredException;

public interface NotificationsTransfer {
    public NotificationsReport poll(long id, ApiMessageContext apiMessageContext) throws UnregisteredException;
    public void unregister(RegistrationID id);
}
