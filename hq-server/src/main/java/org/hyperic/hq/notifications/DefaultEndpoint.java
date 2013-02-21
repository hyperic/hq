package org.hyperic.hq.notifications;

import java.util.List;
import java.util.Map;

public class DefaultEndpoint extends NotificationEndpoint {

    public DefaultEndpoint(long registrationId) {
        super(registrationId);
    }

    @Override
    public void setValues(Map<String, String> values) {}

    @Override
    public void init() {}

    @Override
    public void publishMessages(List<String> messages) {}

    @Override
    public String toString() {
        return "DefaultEndpoint";
    }

}
