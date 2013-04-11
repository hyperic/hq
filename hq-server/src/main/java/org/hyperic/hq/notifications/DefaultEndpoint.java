package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;


public class DefaultEndpoint extends NotificationEndpoint {

    public DefaultEndpoint(String regID) {
        super(regID);
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
    public EndpointStatus publishMessagesInBatch(Collection<InternalAndExternalNotificationReports> messages, List<InternalNotificationReport> failedReports) {
        return null;
    }
}
