package org.hyperic.hq.notifications;

import java.util.Collection;

import edu.emory.mathcs.backport.java.util.Arrays;


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
    public BasePostingStatus[] publishMessagesInBatch(Collection<String> messages) {
        return null;
    }

}
