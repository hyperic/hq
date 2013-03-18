package org.hyperic.hq.notifications.filtering;


import java.util.Collection;

import org.hyperic.hq.notifications.model.InventoryNotification;
import org.springframework.stereotype.Component;

@Component("resourceDestinationEvaluator")
public class ResourceDestinationEvaluator extends DestinationEvaluator<InventoryNotification> {

    @Override
    protected FilterChain<InventoryNotification> instantiateFilterChain(Collection<Filter<InventoryNotification,? extends FilteringCondition<?>>> filters) {
        return new FilterChain<InventoryNotification>(filters);
    }
}