package org.hyperic.hq.notifications;


import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class InventoryNotificationsZeventListener<E extends ResourceZevent> extends BaseNotificationsZeventListener<E,InventoryNotification> {
    @Autowired
    ResourceDestinationEvaluator evaluator;
    @Autowired
    protected ResourceManager resourceMgr;

    @Override
    protected String getConcurrentStatsCollectorType() {
        return ConcurrentStatsCollector.INVENTORY_NOTIFICATION_FILTERING_TIME;
    }
    @Override
    protected DestinationEvaluator<InventoryNotification> getEvaluator() {
        return this.evaluator;
    }
    
    protected abstract InventoryNotification createNotification(E event,Resource r);

    protected List<InventoryNotification> extract(List<E> events) {
        List<InventoryNotification> ns = new ArrayList<InventoryNotification>();
        for(E event:events) {
            AppdefEntityID id = event.getAppdefEntityID();
            Resource r = resourceMgr.getResourceById(id.getId());
            InventoryNotification n = createNotification(event,r);
            ns.add(n);
        }
        return ns;
    } 
}
