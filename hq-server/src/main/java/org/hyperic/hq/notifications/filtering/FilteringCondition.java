package org.hyperic.hq.notifications.filtering;

public abstract class FilteringCondition<T> {
    public abstract boolean check(T entity);
}
