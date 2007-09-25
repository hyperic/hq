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
 */
class MemGalertDef {   
    private final Log _log = LogFactory.getLog(MemGalertDef.class);
    
    private final Log _triggerFiredLog = 
        LogFactory.getLog(MemGalertDef.class.getName()+".Fired");

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

    void setName(String name) {
        _name = name;
        
        synchronized (_strategies) {
            for (Iterator i=_strategies.iterator(); i.hasNext(); ) {
                ExecutionStrategy strat = (ExecutionStrategy)i.next();
                
                strat.setDefinitionName(name);
            }
        }
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

        _log.debug("Trigger[" + _name + "," + trigger + "] fired");
        strat.triggerFired(trigger, reason);
        fireWhenReady(strat);
    }
    
    void triggerNotFired(Gtrigger trigger) {
        ExecutionStrategy strat = trigger.getStrategy();

        _log.debug("Trigger[" + _name + ", " + trigger + "] no longer fired");
        strat.triggerNotFired(trigger);
        fireWhenReady(strat);
    }
    
    private void fireWhenReady(ExecutionStrategy strat) {
        ExecutionReason execReason = strat.shouldFire();
        
        if (execReason == null) 
            return;
        
        if (_triggerFiredLog.isDebugEnabled()) {
            _triggerFiredLog.debug("Alert def [" + _name + "] with id="+
                                      _id+" firing");            
        }
        
        _log.debug(execReason);

        strat.getPartition().execute(_id, execReason);
        strat.reset();
    }
}
