package org.hyperic.hq.notifications.filtering;

import java.util.Collection;

/**
 * 
 * @author yakarn
 *
 * @param <T> defines the type of entities this filter handles
 */
public interface IFilter<T> {
    public void setCondition(INotificationFilteringCondition cond);
    public INotificationFilteringCondition getCondition();
    public Collection<T> filter(Collection<T> entities);
}