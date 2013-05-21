package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.notifications.model.BaseNotification;

/**
 * 
 * @author yakarn
 *
 * @param <N> defines the type of entities this filter handles
 */
public abstract class Filter<N extends BaseNotification, C extends FilteringCondition<?>> {
    protected C cond;
    protected abstract Class<? extends N> getHandledNotificationClass();

    public Filter(C cond) {
        this.cond=cond;
    }

    public List<? extends BaseNotification> filter(List<? extends BaseNotification> notifications) {
        List<BaseNotification> notificationsLeftIn = new ArrayList<BaseNotification>();
        for(BaseNotification notification:notifications) {
            if (getHandledNotificationClass().isAssignableFrom(notification.getClass())) {
                N notificationLeftIn = this.filter((N)notification);
                if (notificationLeftIn!=null) {
                    notificationsLeftIn.add(notificationLeftIn);
                }
            } else {
                notificationsLeftIn.add(notification);
            }
        }
        return notificationsLeftIn;
    }
    /**
     * 
     * @param notification
     * @return the notification in case it passed the filtering condition check, o/w - null
     */
    protected abstract N filter(N notification);
    
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null || !(obj instanceof Filter)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Filter<N, C> other = (Filter<N, C>) obj;
        if (this.cond==null) {
            return other.cond==null;
        }
        return this.cond.equals(other.cond);
    }

    @Override
    public String toString() {
        return cond == null ? "" : cond.toString();
    }
}