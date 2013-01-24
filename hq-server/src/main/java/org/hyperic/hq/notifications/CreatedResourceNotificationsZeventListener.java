package org.hyperic.hq.notifications;

import javax.annotation.PostConstruct;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.stereotype.Component;

@Component("CreatedResourceNotificationsZeventListener")
public class CreatedResourceNotificationsZeventListener extends InventoryNotificationsZeventListener<ResourceCreatedZevent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(ResourceCreatedZevent.class, (ZeventListener<ResourceCreatedZevent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    protected String getListenersBeanName() {
        return "CreatedResourceNotificationsZeventListener";
    }
    @Override
    protected CreatedResourceNotification createNotification(ResourceCreatedZevent event, Resource r) {
        AppdefEntityID parentID = event.getParentID();
        CreatedResourceNotification n = new CreatedResourceNotification(parentID,r);
        return n;
    }
}
