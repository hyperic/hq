package org.hyperic.hq.notifications;

import java.util.List;
import java.util.Map;

public abstract class NotificationEndpoint {
    
    protected final Long id;

    public NotificationEndpoint(long registrationId) {
        this.id = registrationId;
    }
    
    public abstract void setValues(Map<String, String> values);
    
    public abstract void init();

    public abstract void publishMessages(List<String> messages);
    
    public long getRegistrationId() {
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
