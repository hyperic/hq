package org.hyperic.hq.api.transfer;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.notifications.UnregisteredException;

public interface NotificationsTransfer {
    public NotificationsReport poll() throws UnregisteredException;
}
