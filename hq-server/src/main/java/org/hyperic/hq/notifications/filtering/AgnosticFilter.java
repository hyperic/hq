package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.notifications.model.INotification;

public class AgnosticFilter<N extends INotification, C extends FilteringCondition<?>> extends Filter<N, C> {
    public AgnosticFilter() {
        super(null);
    }

    @Override
    protected N filter(N notification) {
        return notification;
    }
}
