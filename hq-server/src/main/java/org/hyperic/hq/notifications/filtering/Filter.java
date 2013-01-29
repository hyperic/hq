package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.notifications.model.BaseNotification;

/**
 * 
 * @author yakarn
 *
 * @param <N> defines the type of entities this filter handles
 */
public abstract class Filter<N extends BaseNotification, C extends FilteringCondition<?>> {
    protected C cond;

    public Filter(C cond) {
        this.cond=cond;
    }

    public Collection<N> filter(Collection<N> notifications) {
        Collection<N> notificationsLeftIn = new ArrayList<N>();
        for(N notification:notifications) {
            N notificationLeftIn = this.filter(notification);
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