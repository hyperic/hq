package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class AccumulatedRegistrationData {
    private static final Log log = LogFactory.getLog(AccumulatedRegistrationData.class);
    private final InternalResourceDetailsType resourceContentType;
    private final LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue;
    private final NotificationEndpoint endpoint;
    private boolean isValid = true;
    private ScheduledFuture<?> schedule;
    private final int queueLimit;
    protected BatchPostingStatus batchPostingStatus;
    
    public AccumulatedRegistrationData(NotificationEndpoint endpoint, int queueLimit,
                                       InternalResourceDetailsType resourceDetailsType) {
        this.queueLimit = queueLimit;
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

    public LinkedBlockingQueue<BaseNotification> getAccumulatedNotificationsQueue() {
        return accumulatedNotificationsQueue;
    }

    public <T extends BaseNotification> void addAll(Collection<T> c) {
        synchronized (accumulatedNotificationsQueue) {
            final int size = accumulatedNotificationsQueue.size();
            if ((c.size() + size) > queueLimit) {
                if (log.isDebugEnabled()) {
                    log.debug("cannot add " + c.size() + " elements to the notifications queue, current size=" + size);
                }
            } else {
                accumulatedNotificationsQueue.addAll(c);
            }
        }
    }

    public void drainTo(Collection<BaseNotification> c) {
        accumulatedNotificationsQueue.drainTo(c);
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void markInvalid() {
        isValid = false;
    }

    public void clear() {
        accumulatedNotificationsQueue.clear();
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void drainTo(List<BaseNotification> c, int size) {
        accumulatedNotificationsQueue.drainTo(c, size);
    }

    public void merge(BatchPostingStatus batchPostingStatus) {
        if (this.batchPostingStatus==null) {
            this.batchPostingStatus=batchPostingStatus;
        }
        this.batchPostingStatus.merge(batchPostingStatus);
    }
}
