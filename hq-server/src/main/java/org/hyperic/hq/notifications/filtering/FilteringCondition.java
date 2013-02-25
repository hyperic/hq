package org.hyperic.hq.notifications.filtering;

import org.hyperic.hibernate.PersistedObject;

public abstract class FilteringCondition<E>  extends PersistedObject {
    public abstract boolean check(E entity);
}