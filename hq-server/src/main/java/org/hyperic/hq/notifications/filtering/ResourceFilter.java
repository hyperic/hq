package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.notifications.model.InventoryNotification;

public class ResourceFilter<C extends ResourceFilteringCondition> extends Filter<InventoryNotification,C> {

    public ResourceFilter(C cond) {
        super(cond);
    }
    protected InventoryNotification filter(InventoryNotification inventoryNotification) {
        Integer rid = inventoryNotification.getResourceID();
        return (cond.check(rid)) ? inventoryNotification : null;
    }
    @Override
    protected Class<? extends InventoryNotification> getHandledNotificationClass() {
        return InventoryNotification.class;
    }
}
