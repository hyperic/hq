package org.hyperic.hq.notifications.filtering;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.notifications.model.BaseNotification;

public abstract class FilteringCondition<E>  extends PersistedObject {
    protected Filter<? extends BaseNotification, ? extends FilteringCondition<E>> filter;
    public abstract boolean check(E entity);
    
    public Filter<? extends BaseNotification, ? extends FilteringCondition<E>> getFilter() {
        return filter;
    }
    public void setFilter(Filter<? extends BaseNotification, ? extends FilteringCondition<E>> filter) {
        this.filter = filter;
    }
}