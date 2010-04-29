package org.hyperic.hq.events.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * Listens for trigger creation events
 * @author jhickey
 *
 */
@Component
public class TriggersCreatedListener implements ZeventListener<TriggersCreatedZevent> {

   
    private RegisteredTriggerManager registeredTriggerManager;
    
    private ZeventEnqueuer zEventManager;
    
    
    @Autowired
    public TriggersCreatedListener(RegisteredTriggerManager registeredTriggerManager,
                                   ZeventEnqueuer zEventManager) {
        this.registeredTriggerManager = registeredTriggerManager;
        this.zEventManager = zEventManager;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<?>> triggerEvents = new HashSet<Class<?>>();
        triggerEvents.add(TriggersCreatedZevent.class);

        zEventManager.addBufferedListener(triggerEvents, this);
    }
    
    public void processEvents(List<TriggersCreatedZevent> events) {
      registeredTriggerManager.handleTriggerCreatedEvents(events);
    }

}
