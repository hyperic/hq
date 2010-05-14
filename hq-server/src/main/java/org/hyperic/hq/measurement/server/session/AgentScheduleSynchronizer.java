/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
@Component
public class AgentScheduleSynchronizer {
    
    private final Log log =
        LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private ZeventEnqueuer zEventManager;
    
    private MeasurementProcessor measurementProcessor;
     
   
    @Autowired
    public AgentScheduleSynchronizer(ZeventEnqueuer zEventManager, MeasurementProcessor measurementProcessor) {
        this.zEventManager = zEventManager;
        this.measurementProcessor = measurementProcessor;
    }

    @PostConstruct
    void initialize() {
        ZeventListener<Zevent> l = new ZeventListener<Zevent>() {
            
            public void processEvents(List<Zevent> events) {
                final List<AppdefEntityID> toSchedule = new ArrayList<AppdefEntityID>(events.size());
                final Map<String,Collection<AppdefEntityID>> unscheduleMap = new HashMap<String,Collection<AppdefEntityID>>(events.size());
                final boolean debug = log.isDebugEnabled();
                for (final Zevent z : events ) {
                    if (z instanceof AgentScheduleSyncZevent) {
                        AgentScheduleSyncZevent event = (AgentScheduleSyncZevent) z;
                        toSchedule.addAll(event.getEntityIds());
                        if (debug) log.debug("Schduling eids=[" + event.getEntityIds() + "]");
                    } else if (z instanceof AgentUnscheduleZevent) {
                        AgentUnscheduleZevent event = (AgentUnscheduleZevent) z;
                        String token = event.getAgentToken();
                        if (token == null) {
                            continue;
                        }
                        Collection<AppdefEntityID> tmp;
                        if (null == (tmp = unscheduleMap.get(token))) {
                            tmp = new HashSet<AppdefEntityID>();
                            unscheduleMap.put(token, tmp);
                        }
                        tmp.addAll(event.getEntityIds());
                        if (debug) log.debug("Unschduling eids=[" + event.getEntityIds() + "]");
                    }
                }
                if (!unscheduleMap.isEmpty()) {
                    for (Map.Entry<String,Collection<AppdefEntityID>> entry : unscheduleMap.entrySet()) {
                        String token = (String) entry.getKey();
                        List<AppdefEntityID> list = new ArrayList<AppdefEntityID>(entry.getValue());
                        try {
                            measurementProcessor.unschedule(token, list);
                        } catch (MeasurementUnscheduleException e) {
                            log.error(e,e);
                        }
                    }
                }
                if (!toSchedule.isEmpty()) {
                    measurementProcessor.scheduleSynchronous(toSchedule);
                }
            }
            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };
        
        zEventManager.addBufferedListener(AgentScheduleSyncZevent.class, l);
        zEventManager.addBufferedListener(AgentUnscheduleZevent.class, l);
    }
  
}
