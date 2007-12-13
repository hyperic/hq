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
package org.hyperic.hq.measurement.server.session;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.ext.depgraph.DerivedNode;
import org.hyperic.hq.measurement.ext.depgraph.Graph;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.ext.depgraph.Node;
import org.hyperic.hq.measurement.ext.depgraph.RawNode;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
public class AgentScheduleSynchronizer {
    
    private static final Log _log =
        LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private static AgentScheduleSynchronizer SINGLETON =
        new AgentScheduleSynchronizer();

    /**
     * Cache of {@link AppdefEntityID}s onto themselves, of ents which are
     * currently in the zevent queue for processing. 
     */
    private final Cache _inQueueCache = 
        CacheManager.getInstance().getCache("AgentScheduleInQueue");
    
    public static AgentScheduleSynchronizer getInstance() {
        return AgentScheduleSynchronizer.SINGLETON;
    }

    private AgentScheduleSynchronizer() {
    }


    public static void scheduleSynchronous(AppdefEntityID eid) {
        try {
            SRNManagerEJBImpl.getOne().reschedule(eid);
        } catch(Exception e) {
            _log.warn("Exception, scheduling [" + eid + "]", e);
        }
    }

    void initialize() {
        ZeventListener l = new ZeventListener() {
            public void processEvents(List events) {
                for (Iterator i=events.iterator(); i.hasNext(); ) {
                    AgentScheduleSyncZevent z = 
                        (AgentScheduleSyncZevent)i.next();
                    
                    if (_inQueueCache.remove(z.getEntityId()) == false) {
                        _log.warn("Received eid=[" + z.getEntityId() + 
                                  "] but was not found in cache");
                    }
                    scheduleSynchronous(z.getEntityId());
                }
            }
            
            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };
        
        ZeventManager.getInstance()
            .addBufferedListener(AgentScheduleSyncZevent.class, l);
    }
    
    private void _scheduleBuffered(AppdefEntityID eid) {
        /**
         * The following is done immediately (not in a tx listener), since
         * otherwise we will need to make sure that the entire thing behaves
         * transactionally.  The worst case, here (if the tx is rolled back)
         * is that we have excess things in the queue.
         */
        synchronized (_inQueueCache) {
            if (_inQueueCache.get(eid) != null)
                return;
    
            Element e = new Element(eid, eid);
            _inQueueCache.put(e);
        }
        
        Zevent z = new AgentScheduleSyncZevent(eid);
        try {
            ZeventManager.getInstance().enqueueEvent(z);
        } catch(InterruptedException e) {
            _inQueueCache.remove(eid);
            _log.warn("Interrupted while trying to enqueue event");
        }
    }
    
    public static void scheduleBuffered(AppdefEntityID eid) {
        AgentScheduleSynchronizer.SINGLETON._scheduleBuffered(eid);
    }
}
