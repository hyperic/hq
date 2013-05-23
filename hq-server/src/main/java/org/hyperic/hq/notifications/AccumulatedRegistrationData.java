package org.hyperic.hq.notifications;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class AccumulatedRegistrationData {
    private static final Log log = LogFactory.getLog(AccumulatedRegistrationData.class);
    private final InternalResourceDetailsType resourceContentType;
    private final LinkedBlockingQueue<BaseNotification> accumulatedNotificationsQueue;
    private final NotificationEndpoint endpoint;
    private AtomicBoolean isValid = new AtomicBoolean(true);
    private ScheduledFuture<?> schedule;
    private final int queueLimit;
    protected EndpointStatus batchPostingStatus;
    protected long creationTime;
    private Object lock;
    
    public AccumulatedRegistrationData(NotificationEndpoint endpoint, int queueLimit,
                                       InternalResourceDetailsType resourceDetailsType,
                                       Object lock) {
        this.queueLimit = queueLimit;
        this.accumulatedNotificationsQueue = new LinkedBlockingQueue<BaseNotification>(queueLimit);
        this.resourceContentType = resourceDetailsType;
        this.endpoint = endpoint;
        this.creationTime = System.currentTimeMillis();
        this.lock = lock;
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
        if (!isValid()) {
            return;
        }
        synchronized (lock) {
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
        synchronized (lock) {
            accumulatedNotificationsQueue.drainTo(c);
        }
    }
    
    public boolean isValid() {
        return isValid.get();
    }
    
    public void markInvalid() {
        isValid.set(false);
    }

    public void clear() {
        synchronized (lock) {
            accumulatedNotificationsQueue.clear();
        }
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

    public void merge(EndpointStatus batchPostingStatus) {
        if (this.batchPostingStatus==null) {
            this.batchPostingStatus=batchPostingStatus;
        }
        this.batchPostingStatus.merge(batchPostingStatus);
    }

    public EndpointStatus getEndpointStatus() {
        return this.batchPostingStatus;
    }

    public long getCreationTime() {
        return this.creationTime;
    }
}
