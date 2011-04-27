package org.hyperic.hq.messaging;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.events.AbstractEvent;

public class MockMessagePublisher implements MessagePublisher {

    private Set<AbstractEvent> receivedEvents = new HashSet<AbstractEvent>();

    public void publishMessage(String name, Serializable event) {
        receivedEvents.add((AbstractEvent) event);
    }

    public Set<AbstractEvent> getReceivedEvents() {
        return receivedEvents;
    }
    
    public void clearReceivedEvents() {
        receivedEvents.clear();
    }
    
}
