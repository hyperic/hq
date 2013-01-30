package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.notifications.model.BaseNotification;

public class AgnosticFilter<N extends BaseNotification, C extends FilteringCondition<?>> extends Filter<N, C> {
    public AgnosticFilter() {
        super(null);
    }

    @Override
    protected N filter(N notification) {
        return notification;
    }
}
