package org.hyperic.hq.notifications;

import java.util.concurrent.LinkedBlockingQueue;

import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class AccumulatedRegistrationData {
    protected InternalResourceDetailsType resourceContentType;
    protected LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue;

    public AccumulatedRegistrationData(int queueLimit, InternalResourceDetailsType resourceDetailsType) {
        this.accumulatedNotificationsQueue = new LinkedBlockingQueue<BaseNotification>(queueLimit) {
            public boolean offer(BaseNotification e) {
                if (this.contains(e)) {
                    return false;
                }
                return super.offer(e);
            }
        };
        this.resourceContentType = resourceDetailsType;
    }
    public InternalResourceDetailsType getResourceContentType() {
        return resourceContentType;
    }
    public void setResourceContentType(InternalResourceDetailsType resourceContentType) {
        this.resourceContentType = resourceContentType;
    }
    public LinkedBlockingQueue<BaseNotification> getAccumulatedNotificationsQueue() {
        return accumulatedNotificationsQueue;
    }
    public void setAccumulatedNotificationsQueue(LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue) {
        this.accumulatedNotificationsQueue = accumulatedNotificationsQueue;
    }
}
