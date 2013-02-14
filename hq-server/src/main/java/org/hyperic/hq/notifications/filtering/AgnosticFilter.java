package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.notifications.model.BaseNotification;

public class AgnosticFilter<N extends BaseNotification, C extends FilteringCondition<?>> extends Filter<N, C> {
    public AgnosticFilter() {
        super(null);
    }

    @Override
    protected BaseNotification filter(BaseNotification notification) {
        return notification;
    }
    @Override
    protected Class<? extends N> getHandledNotificationClass() {
        return (Class<? extends N>) BaseNotification.class;
    }
}
