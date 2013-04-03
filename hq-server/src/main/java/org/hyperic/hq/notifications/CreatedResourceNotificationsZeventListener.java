package org.hyperic.hq.notifications;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.NewResourceEvent;
import org.hyperic.hq.appdef.server.session.NewResourceEvent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.stereotype.Component;

@Component("CreatedResourceNotificationsZeventListener")
public class CreatedResourceNotificationsZeventListener extends InventoryNotificationsZeventListener<NewResourceEvent> {
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(NewResourceEvent.class, (ZeventListener<NewResourceEvent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }
    @Override
    public String getListenersBeanName() {
        return "CreatedResourceNotificationsZeventListener";
    }
    @Override
    protected CreatedResourceNotification createNotification(NewResourceEvent event) {
        Integer parentID = event.getParentID();
        Resource r = event.getResource();
        CreatedResourceNotification n = new CreatedResourceNotification(parentID,r);
        return n;
    }
}