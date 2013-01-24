package org.hyperic.hq.notifications;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.RemovedResourceNotification;
import org.hyperic.hq.zevents.ZeventListener;

public class RemovedResourceNotificationsZeventListener extends InventoryNotificationsZeventListener<ResourceDeletedZevent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(ResourceDeletedZevent.class, (ZeventListener<ResourceDeletedZevent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    protected String getListenersBeanName() {
        return "RemovedResourceNotificationsZeventListener";
    }
    @Override
    protected RemovedResourceNotification createNotification(ResourceDeletedZevent event, Resource r) {
        return new RemovedResourceNotification(r);
    }
}
