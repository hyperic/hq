package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;


public abstract class NotificationEndpoint {
    
    protected final String id;

    public NotificationEndpoint(String regID) {
        this.id = regID;
    }
    
    public abstract boolean canPublish();

    public abstract EndpointStatus publishMessagesInBatch(Collection<InternalAndExternalNotificationReports> messages, List<InternalNotificationReport> failedReports);
    
    public String getRegistrationId() {
        return id;
    }
    
    public final int hashCode() {
        return id.hashCode();
    }

    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof NotificationEndpoint) {
            NotificationEndpoint endpoint = (NotificationEndpoint) o;
            return id.equals(endpoint.id);
        }
        return false;
    }

    public abstract String toString();

}
