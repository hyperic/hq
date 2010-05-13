package org.hyperic.hq.ha;

/**
 * Service responsible for HA operations
 * @author jhickey
 * 
 */
public interface HAService {

    boolean isMasterNode();
    
    void start();
    
    void stop();

    boolean alertTriggersHaveInitialized();
}
