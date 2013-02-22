package org.hyperic.hq.notifications;


public abstract class NotificationEndpoint {
    
    protected final Long id;

    public NotificationEndpoint(long registrationId) {
        this.id = registrationId;
    }
    
    public abstract boolean canPublish();

    public abstract void publishMessage(String message);
    
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
