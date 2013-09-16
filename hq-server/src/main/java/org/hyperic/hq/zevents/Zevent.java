/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.zevents;

/**
 * Represents an event which can interact with the Zevent subsystem. 
 */
public abstract class Zevent {
    protected ZeventSourceId _sourceId;
    protected ZeventPayload  _payload;
    private final Object _timeLock = new Object();
    private long _queueEntryTime;
    private long _queueExitTime;
    protected final static String EVENT_TYPE = "ZEvent";
    protected String getEventType() {
        return EVENT_TYPE;
    }
    
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
        synchronized (_timeLock) { 
            _queueEntryTime = System.currentTimeMillis();
        }
    }
    
    void leaveQueue() {
        synchronized (_timeLock) {
            _queueExitTime = System.currentTimeMillis();
        }
    }

    /**
     * Get the time that the event entered the queue (in ms)
     */
    public long getQueueEntryTime() {
        synchronized (_timeLock) {
            return _queueEntryTime;
        }
    }
    
    /**
     * Get the time that the event left the queue and went to be dispatched 
     */
    public long getQueueExitTime() {
        synchronized (_timeLock) {
            return _queueExitTime;
        }
    }
    
    public String toString() {
        return getEventType() + "[srcId=" + _sourceId + ", payload=" + _payload + "]";
    }
}
