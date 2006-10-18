package org.hyperic.hq.events;

import org.hyperic.hibernate.PersistedObject;

/**
 * Not used.
 * @see TriggerEvent
 */
public class TriggerEventId 
    extends PersistedObject  // We don't use anything here, but it's nice anyway
{
    private Integer _triggerId;
    private Event   _eventId;

    public TriggerEventId() {
    }

    public TriggerEventId(Integer triggerId, Event eventId) {
        _triggerId = triggerId;
        _eventId   = eventId;
    }
   
    public Integer getTriggerId() {
        return _triggerId;
    }
    
    public void setTriggerId(Integer triggerId) {
        _triggerId = triggerId;
    }

    public Event getEventId() {
        return _eventId;
    }
    
    public void setEventId(Event eventId) {
        _eventId = eventId;
    }
}
