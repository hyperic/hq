package org.hyperic.hq.zevents;

/**
 * Represents an event which can interact with the Zevent subsystem. 
 */
public abstract class Zevent {
    private ZeventSourceId _sourceId;
    private ZeventPayload  _payload;
    
    private long _queueEntryTime;
    private long _queueExitTime;
    
    public Zevent(ZeventSourceId sourceId, ZeventPayload payload) {
        _sourceId = sourceId;
        _payload  = payload;
    }
                      
    /**
     * Get the ID of the source that generated the event. 
     */
    public ZeventSourceId getSourceId() {
        return _sourceId;
    }
    
    /**
     * Get the event payload
     */
    public ZeventPayload getPayload() {
        return _payload;
    }
    
    void enterQueue() {
        _queueEntryTime = System.currentTimeMillis();
    }
    
    void leaveQueue() {
        _queueExitTime = System.currentTimeMillis();
    }

    /**
     * Get the time that the event entered the queue (in ms)
     */
    public long getQueueEntryTime() {
        return _queueEntryTime;
    }
    
    /**
     * Get the time that the event left the queue and went to be dispatched 
     */
    public long getQueueExitTime() {
        return _queueExitTime;
    }
    
    public String toString() {
        return "ZEvent[resNum=" + _sourceId + ", payload=" + _payload + "]";
    }
}
