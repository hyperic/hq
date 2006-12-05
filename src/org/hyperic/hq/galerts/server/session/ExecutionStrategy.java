package org.hyperic.hq.galerts.server.session;

import java.util.List;

import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;


public interface ExecutionStrategy {
    /**
     * Determine if the execution strategy should fire.  
     * 
     * @return null if the strategy should not fire
     */
    ExecutionReason shouldFire();

    /**
     * Reset the internal state of the execution strategy.  This should not  
     * affect the data setup by {@link #configure(String, List)}, only
     * the internal state (such as time of last fired trigger, etc.)
     * This method will be invoked after the alert definition has fired and
     * needs to be reset.
     */
    void reset();
    
    /**
     * Inform the strategy about the state in which it is running.  This 
     * method will be invoked when the the framework detects a change in
     * configuration of any of the triggers.
     */
    void configure(GalertDefPartition partition, String defName, List triggers);
    
    /**
     * Called when a trigger has fired.   
     */
    void triggerFired(Gtrigger trigger, FireReason reason);
    
    /**
     * Called when a condition is no longer firing (i.e. its condition is no
     * longer met)
     */
    void triggerNotFired(Gtrigger trigger);
}
