package org.hyperic.hq.bizapp.server.shared;

public interface HeartbeatCurrentTime {
    
    /**
     * @return timestamp in millis of the current time represented by this object
     */
    public long getTimeMillis();

}
