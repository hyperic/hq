package org.hyperic.hq.notifications.filtering;

public abstract class FilteringCondition<E> {
    public abstract boolean check(E entity);
}