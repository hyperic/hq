package org.hyperic.hq.notifications.filtering;

public interface INotificationFilteringCondition<E> {
    boolean check(E entity); 
}
