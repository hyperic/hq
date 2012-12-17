package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.notifications.model.INotification;

/**
 * 
 * @author yakarn
 *
 * @param <T> defines the type of entities this filter handles
 */
public abstract class Filter<T extends INotification, C extends INotificationFilteringCondition> {
    protected C cond;

    public Filter(C cond) {
        this.cond=cond;
    }

    public Collection<T> filter(Collection<T> entities) {
        Collection<T> rscsLeftIn = new ArrayList<T>();
        for(T entity:entities) {
            T entityLeftIn = this.filter(entity);
            if (entityLeftIn!=null) {
                rscsLeftIn.add(entityLeftIn);
            }
        }
        return rscsLeftIn;
    }
    protected abstract T filter(T metricNotification);
}