package org.hyperic.hq.autoinventory.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener class that enables runtime-AI on newly created resources.
 */
@Component
public class RuntimeAIEnabler implements ZeventListener<ResourceZevent> {
    private AutoinventoryManager autoinventoryManager;
    private ZeventEnqueuer zEventManager;
    
    @Autowired
    public RuntimeAIEnabler(AutoinventoryManager autoinventoryManager,
                             ZeventEnqueuer zEventManager) {
        this.autoinventoryManager = autoinventoryManager;
        this.zEventManager = zEventManager;
    }
    
    @PostConstruct
    public void subscribe() {
        Set<Class<?>> events = new HashSet<Class<?>>();
        events.add(ResourceCreatedZevent.class);
        events.add(ResourceUpdatedZevent.class);
        events.add(ResourceRefreshZevent.class);
        zEventManager.addBufferedListener(events, this);
    }
    public void processEvents(List<ResourceZevent> events) {
        autoinventoryManager.handleResourceEvents(events);
    }

    public String toString() {
        return "RuntimeAIEnabler";
    }
}
