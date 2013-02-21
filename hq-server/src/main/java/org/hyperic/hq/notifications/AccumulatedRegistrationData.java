package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class AccumulatedRegistrationData {
    private final InternalResourceDetailsType resourceContentType;
    private final LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue;
    private final NotificationEndpoint endpoint;

    public AccumulatedRegistrationData(NotificationEndpoint endpoint, int queueLimit,
                                       InternalResourceDetailsType resourceDetailsType) {
        this.accumulatedNotificationsQueue = new LinkedBlockingQueue<BaseNotification>(queueLimit);
        this.resourceContentType = resourceDetailsType;
        this.endpoint = endpoint;
    }
    
    public NotificationEndpoint getNotificationEndpoint() {
        return endpoint;
    }

    public InternalResourceDetailsType getResourceContentType() {
        return resourceContentType;
    }

//    public void setResourceContentType(InternalResourceDetailsType resourceContentType) {
//        this.resourceContentType = resourceContentType;
//    }

    public LinkedBlockingQueue<BaseNotification> getAccumulatedNotificationsQueue() {
        return accumulatedNotificationsQueue;
    }

//    public void setAccumulatedNotificationsQueue(LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue) {
//        this.accumulatedNotificationsQueue = accumulatedNotificationsQueue;
//    }

    public <T extends BaseNotification> void addAll(Collection<T> c) {
        accumulatedNotificationsQueue.addAll(c);
    }

    public void drainTo(Collection<BaseNotification> c) {
        accumulatedNotificationsQueue.drainTo(c);
    }
}
