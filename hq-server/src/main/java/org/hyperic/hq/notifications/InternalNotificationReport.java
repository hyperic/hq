package org.hyperic.hq.notifications;

import java.util.List;

import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class InternalNotificationReport {
    public void setResourceDetailsType(InternalResourceDetailsType resourceDetailsType) {
        this.resourceDetailsType = resourceDetailsType;
    }

    public void setNotifications(List<? extends BaseNotification> notifications) {
        this.notifications = notifications;
    }

    protected InternalResourceDetailsType resourceDetailsType;
    protected List<? extends BaseNotification> notifications;
    
    public InternalResourceDetailsType getResourceDetailsType() {
        return this.resourceDetailsType;
    }

    public List<? extends BaseNotification> getNotifications() {
        return this.notifications;
    }
}
