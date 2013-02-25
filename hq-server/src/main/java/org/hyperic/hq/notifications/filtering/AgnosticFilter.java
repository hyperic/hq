package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.notifications.model.BaseNotification;

public class AgnosticFilter<C extends FilteringCondition<?>> extends Filter<BaseNotification, C> {
    private static final long serialVersionUID = 3362239285653855944L;

    public AgnosticFilter() {
        super(null);
    }

    @Override
    protected BaseNotification filter(BaseNotification notification) {
        return notification;
    }

    @Override
    protected Class<? extends BaseNotification> getHandledNotificationClass() {
        return BaseNotification.class;
    }

    @Override
    protected String initFilterType() {
        return "AGNOSTIC_FILTER";
    }
}
