package org.hyperic.hq.galerts.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GtriggerInfo;


/**
 * Represents an in-memory alert definition.  Different than the persisted
 * objects such as {@link GalertDef}
 * 
 * This class is immutable
 */
class MemGalertDef {
    private final Log _log = LogFactory.getLog(MemGalertDef.class);

    private Integer _id;
    private String  _name;
    private Set     _strategies = new HashSet();
    private Map     _interestedEvents = new HashMap();
    
    MemGalertDef(GalertDef def) {
        _id    = def.getId();
        _name  = def.getName();
            
        for (Iterator i=def.getStrategies().iterator(); i.hasNext(); ) {
            ExecutionStrategyInfo sInfo = (ExecutionStrategyInfo)i.next();

            initializeStrategy(sInfo, def.getGroup());
        }
    }

    Integer getId() {
        return _id;
    }
    
    private void initializeStrategy(ExecutionStrategyInfo sInfo,
                                    ResourceGroup group) 
    {
        ExecutionStrategy strat = sInfo.getStrategy();
        List triggers = new ArrayList();
        
        for (Iterator i=sInfo.getTriggers().iterator(); i.hasNext(); ) {
            GtriggerInfo tInfo = (GtriggerInfo)i.next();
            Gtrigger trigger = tInfo.getTrigger();
            Set eventIds;
            
            triggers.add(trigger);
            trigger.setAlertDef(this);
            trigger.setStrategy(strat);
            trigger.setGroup(group);
            
            eventIds = trigger.getInterestedEvents();
            if (eventIds == null) {
                throw new IllegalStateException("Triggers must define a " +
                                                "non-null set of eventIds");
            }
            
            _interestedEvents.put(trigger, 
                                  new HashSet(trigger.getInterestedEvents()));
        }
        strat.configure(sInfo.getPartition(), _name, triggers);
        _strategies.add(strat);
    }
    
    String getName() {
        return _name;
    }
    
    /**
     * Get a Map of {@link Gtrigger}s onto {@link Set}s of 
     * {@link ZeventResourceId}s which the triggers are interested in.
     */
    Map getInterestedEvents() {
        return Collections.unmodifiableMap(_interestedEvents);
    }
    
    void triggerFired(Gtrigger trigger, FireReason reason) {
        ExecutionStrategy strat = trigger.getStrategy();

        _log.info("Trigger[" + _name + "," + trigger + "] fired");
        strat.triggerFired(trigger, reason);
        fireWhenReady(strat);
    }
    
    void triggerNotFired(Gtrigger trigger) {
        ExecutionStrategy strat = trigger.getStrategy();

        _log.info("Trigger[" + _name + ", " + trigger + "] no longer fired");
        strat.triggerNotFired(trigger);
        fireWhenReady(strat);
    }
    
    private void fireWhenReady(ExecutionStrategy strat) {
        ExecutionReason execReason = strat.shouldFire();
        
        if (execReason == null) 
            return;
        
        _log.info("Alert def [" + _name + " firing");
        _log.info(execReason);

        // TODO: Dispatch to alert escalation
        strat.reset();
    }
}
