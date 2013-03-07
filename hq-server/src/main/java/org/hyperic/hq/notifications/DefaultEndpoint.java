package org.hyperic.hq.notifications;


public class DefaultEndpoint extends NotificationEndpoint {

    public DefaultEndpoint(long registrationId) {
        super(registrationId);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[registrationId=").append(getRegistrationId()).append("]").toString();
    }

    @Override
    public boolean canPublish() {
        return false;
    }

    @Override
    public void publishMessage(String message) {}

}
