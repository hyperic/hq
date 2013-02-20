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
//@Entity
//@Table(name="EAM_NOTIFICATIONS_FILTER")
//@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Filter<N extends BaseNotification, C extends FilteringCondition<?>> {
//    @Column(???)
    protected C cond;
    protected abstract Class<? extends N> getHandledNotificationClass();
    
    public Filter(C cond) {
        this.cond=cond;
    }

    public List<N> filter(List<N> notifications) {
        List<N> notificationsLeftIn = new ArrayList<N>();
        for(N notification:notifications) {
            if (!getHandledNotificationClass().isAssignableFrom(notification.getClass())) {
                notificationsLeftIn.add(notification);
                continue;
            }
            N notificationLeftIn = this.filter((N)notification);
            if (notificationLeftIn!=null) {
                notificationsLeftIn.add(notificationLeftIn);
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
}