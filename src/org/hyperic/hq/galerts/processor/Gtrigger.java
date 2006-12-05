package org.hyperic.hq.galerts.processor;

import java.util.Set;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.zevents.Zevent;


/**
 * The Gtrigger is a trigger which can participate in group-alerting activities.
 * 
 * The purpose of the trigger is to monitor events, and 'fire' when it deems
 * necessary.
 * 
 * The lifecycle of the trigger is as follows:
 * 
 *   - setGroup is called to inform the trigger of the environment it is in
 *   - getInterestedEvents is then called to tell the alerting system which
 *     events the trigger will be listenting to.
 */
public abstract class Gtrigger {
    private MemGalertDef      _alertDef;
    private ExecutionStrategy _strategy;
    
    public abstract void processEvent(Zevent event);

    public abstract Set getInterestedEvents();

    public abstract void setGroup(ResourceGroup g);
    
    void setAlertDef(MemGalertDef def) {
        _alertDef = def;
    }

    void setStrategy(ExecutionStrategy strat) {
        _strategy = strat;
    }
    
    ExecutionStrategy getStrategy() {
        return _strategy;
    }
    
    /**
     * Called by the subclass to fire based on the passed reason. 
     * 
     * @param reason Reason the trigger fired.  This is used to compose
     *               alert messages, etc.
     */
    protected void setFired(FireReason reason) {
        _alertDef.triggerFired(this, reason);
    }
    
    protected void setNotFired() {
        _alertDef.triggerNotFired(this);
    }
}
