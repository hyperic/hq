package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Listens for trigger creation events
 * @author jhickey
 *
 */
public class TriggersCreatedListener implements ZeventListener<TriggersCreatedZevent> {

    @Autowired
    private RegisteredTriggerManager registeredTriggerManager;
    
    public void processEvents(List<TriggersCreatedZevent> events) {
      registeredTriggerManager.handleTriggerCreatedEvents(events);
    }

}
