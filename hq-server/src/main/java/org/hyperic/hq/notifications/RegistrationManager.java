package org.hyperic.hq.notifications;

import java.util.List;

import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.model.BaseNotification;

public interface RegistrationManager {
    Integer register(Class<? extends BaseNotification> entityType,
                     List<? extends Filter<? extends BaseNotification, ? extends FilteringCondition<?>>> userFilters);
    void unregister(Integer regID);
    public boolean loadRegistrations();
}
