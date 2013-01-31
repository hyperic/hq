package org.hyperic.hq.notifications;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceContentChangedEvent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.notifications.model.ResourceChangedContentNotification;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.stereotype.Component;

@Component("resourceContentUpdatedNotificationsZeventListener")
public class ResourceContentUpdatedNotificationsZeventListener extends InventoryNotificationsZeventListener<ResourceContentChangedEvent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(ResourceContentChangedEvent.class, (ZeventListener<ResourceContentChangedEvent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    protected String getListenersBeanName() {
        return "resourceContentUpdatedNotificationsZeventListener";
    }
    @Override
    protected CreatedResourceNotification createNotification(ResourceContentChangedEvent event) {
        Integer rid = event.getResourceID();
        AllConfigResponses allChangedConfigs = event.getChangedContent();
        ResourceChangedContentNotification n = new ResourceChangedContentNotification(rid,allChangedConfigs);
        return n;
    }
}
//