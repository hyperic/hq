package org.hyperic.hq.notifications;


import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.appdef.server.session.InventoryEvent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class InventoryNotificationsZeventListener<E extends InventoryEvent> extends BaseNotificationsZeventListener<E,InventoryNotification> {
    @Autowired
    ResourceDestinationEvaluator evaluator;
    @Autowired
    protected ResourceManager resourceMgr;

    @Override
    public String getConcurrentStatsCollectorType() {
        return ConcurrentStatsCollector.INVENTORY_NOTIFICATION_FILTERING_TIME;
    }
    @Override
    public DestinationEvaluator<InventoryNotification> getEvaluator() {
        return this.evaluator;
    }
    
    protected abstract InventoryNotification createNotification(E event);

    public List<InventoryNotification> extract(List<E> events) {
        List<InventoryNotification> ns = new ArrayList<InventoryNotification>();
        for(E event:events) {
            InventoryNotification n = createNotification(event);
            ns.add(n);
        }
        return ns;
    } 
}
