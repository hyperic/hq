package org.hyperic.hq.notifications;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.RemovedResourceEvent;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.RemovedResourceNotification;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.stereotype.Component;

@Component("RemovedResourceNotificationsZeventListener")
public class RemovedResourceNotificationsZeventListener extends InventoryNotificationsZeventListener<RemovedResourceEvent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(RemovedResourceEvent.class, (ZeventListener<RemovedResourceEvent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    public String getListenersBeanName() {
        return "RemovedResourceNotificationsZeventListener";
    }
    @Override
    protected RemovedResourceNotification createNotification(RemovedResourceEvent event) {
        return new RemovedResourceNotification(event.getID());
    }
}
