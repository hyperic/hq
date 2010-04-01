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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.stats.ConcurrentStatsCollector;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
public class AgentScheduleSynchronizer {
    
    private static final Log _log = LogFactory.getLog(AgentScheduleSynchronizer.class.getName());
    private static final int NUM_WORKERS = 4;
    private final ScheduledThreadPoolExecutor _executor;
    private final List _workers = new ArrayList(NUM_WORKERS);
    /** {@link Map} of {@link Integer}=AgentId to {@link Collection} of {@link AppdefEntityID}s */
    private final Map _scheduleAeids = new HashMap();
    /** {@link Map} of {@link Integer}=AgentId to {@link Collection} of {@link AppdefEntityID}s */
    private final Map _unscheduleAeids = new HashMap();
    private final Set _activeAgents = Collections.synchronizedSet(new HashSet());
    private final ConcurrentStatsCollector _stats = ConcurrentStatsCollector.getInstance();
    private final String SCHEDULE_QUEUE_SIZE = ConcurrentStatsCollector.SCHEDULE_QUEUE_SIZE;
    private final String UNSCHEDULE_QUEUE_SIZE = ConcurrentStatsCollector.UNSCHEDULE_QUEUE_SIZE;
    private ScheduledFuture _schedule;

    private static AgentScheduleSynchronizer SINGLETON = new AgentScheduleSynchronizer();
    
    public static AgentScheduleSynchronizer getInstance() {
        return AgentScheduleSynchronizer.SINGLETON;
    }

    private AgentScheduleSynchronizer() {
        _executor = new ScheduledThreadPoolExecutor(NUM_WORKERS, new ThreadFactory() {
            private AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "AgentScheduler" + i.getAndIncrement());
            }
        });
        for (int i=0; i<NUM_WORKERS; i++) {
            SchedulerThread worker = new SchedulerThread("AgentScheduler" + i);
            _workers.add(worker);
            _schedule = _executor.scheduleWithFixedDelay(worker, i+1, NUM_WORKERS, TimeUnit.SECONDS);
        }
    }
    
    private class SchedulerThread implements Runnable {
        private final String _name;
        private SchedulerThread(String name) {
            _name = name;
        }
        public String toString() {
            return _name;
        }
        public synchronized void run() {
            try {
                boolean hasMoreToSchedule = true;
                boolean hasMoreToUnschedule = true;
                synchronized (_scheduleAeids) {
                    _stats.addStat(_scheduleAeids.size(), SCHEDULE_QUEUE_SIZE);
                }
                synchronized (_unscheduleAeids) {
                    _stats.addStat(_unscheduleAeids.size(), UNSCHEDULE_QUEUE_SIZE);
                }
                while (hasMoreToSchedule || hasMoreToUnschedule) {
                    hasMoreToUnschedule = syncMetrics(_unscheduleAeids, false);
                    hasMoreToSchedule = syncMetrics(_scheduleAeids, true);
                }
            } catch (Throwable t) {
                _log.error(t,t);
            }
        }
        
        private boolean syncMetrics(Map scheduleAeids, final boolean schedule)
        throws Exception {
            Integer agentId = null;
            Collection aeids = null;
            final boolean debug = _log.isDebugEnabled();
            try {
                synchronized (scheduleAeids) {
                    if (scheduleAeids.isEmpty()) {
                        return false;
                    }
                    for (Iterator it=scheduleAeids.entrySet().iterator(); it.hasNext(); ) {
                        Entry entry = (Entry) it.next();
                        agentId = (Integer) entry.getKey();
                        aeids = (Collection) entry.getValue();
                        boolean added = _activeAgents.add(agentId);
                        if (added) {
                            if (debug) _log.debug("scheduling agentId=" + agentId);
                            scheduleAeids.remove(agentId);
                            break;
                        } else {
                            agentId = null;
                            aeids = null;
                        }
                    }
                }
                if (aeids == null || agentId == null || aeids.isEmpty()) {
                    return false;
                }
                runSchedule(schedule, agentId, aeids);
                return true;
            } finally {
                if (agentId != null) {
                    if (debug) _log.debug("agentId=" + agentId + " is finished scheduling");
                    _activeAgents.remove(agentId);
                }
            }
        }

        private void runSchedule(final boolean schedule, final Integer agentId,
                                 final Collection aeids)
        throws Exception {
            SessionManager.runInSession(new SessionRunner() {
                public void run() throws Exception {
                    final MeasurementProcessorLocal mProc = MeasurementProcessorEJBImpl.getOne();
                    final AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
                    final Agent agent = aMan.findAgent(agentId);
                    final AuthzSubject subj = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
                    try {
                        aMan.pingAgent(subj, agent);
                    } catch (AgentConnectionException e) {
                        // this only occurs when ping failed (why isn't there a better exception??)
                        _log.debug(e,e);
                    }
                    if (schedule) {
                        _log.info("scheduling " + aeids.size() + 
                                  " resources to agentid=" + agent.getId());
                        mProc.scheduleEnabled(agent, aeids);
                    } else {
                        _log.info("unscheduling " + aeids.size() + 
                                  " resources to agentid=" + agent.getId());
                        mProc.unschedule(agent.getAgentToken(), aeids);
                    }
                }
                public String getName() {
                    return _name;
                }
            });
        }
    }

    void initialize() {
        ZeventListener l = new ZeventListener() {
            public void processEvents(List events) {
                final List toSchedule = new ArrayList(events.size());
                final Map unscheduleMap = new HashMap(events.size());
                final boolean debug = _log.isDebugEnabled();
                for (final Iterator i=events.iterator(); i.hasNext(); ) {
                    final Zevent z = (Zevent)i.next();
                    if (z instanceof AgentScheduleSyncZevent) {
                        AgentScheduleSyncZevent event = (AgentScheduleSyncZevent) z;
                        toSchedule.addAll(event.getEntityIds());
                        if (debug) _log.debug("Schduling eids=[" + event.getEntityIds() + "]");
                    } else if (z instanceof AgentUnscheduleZevent) {
                        AgentUnscheduleZevent event = (AgentUnscheduleZevent) z;
                        String token = event.getAgentToken();
                        if (token == null) {
                            continue;
                        }
                        Collection tmp;
                        if (null == (tmp = (Collection) unscheduleMap.get(token))) {
                            tmp = new HashSet();
                            unscheduleMap.put(token, tmp);
                        }
                        tmp.addAll(event.getEntityIds());
                        if (debug) _log.debug("Unschduling eids=[" + event.getEntityIds() + "]");
                    }
                }
                final AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
                final Map agentAppdefIds = aMan.getAgentMap(toSchedule);
                synchronized (_scheduleAeids) {
                    for (final Iterator it=agentAppdefIds.entrySet().iterator(); it.hasNext(); ) {
                        final Entry entry = (Entry) it.next();
                        final Integer agentId = (Integer) entry.getKey();
                        final Collection eids = (Collection) entry.getValue();
                        Collection tmp;
                        if (null == (tmp = (Collection)_scheduleAeids.get(agentId))) {
                            tmp = new HashSet(eids.size());
                            _scheduleAeids.put(agentId, tmp);
                        }
                        tmp.addAll(eids);
                    }
                }
                synchronized (_unscheduleAeids) {
                    for (final Iterator it=unscheduleMap.entrySet().iterator(); it.hasNext(); ) {
                        final Entry entry = (Entry) it.next();
                        final String token = (String) entry.getKey();
                        final Collection eids = (Collection) entry.getValue();
                        Integer agentId;
                        try {
                            agentId = aMan.getAgent(token).getId();
                        } catch (AgentNotFoundException e) {
                            _log.warn("Could not get agentToken=" + token +
                                      " from db to unschedule: " + e);
                            continue;
                        }
                        Collection tmp;
                        if (null == (tmp = (Collection)_unscheduleAeids.get(agentId))) {
                            tmp = new HashSet(eids.size());
                            _unscheduleAeids.put(agentId, tmp);
                        }
                        tmp.addAll(eids);
                    }
                }
            }
            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };
        ZeventManager.getInstance().addBufferedListener(AgentScheduleSyncZevent.class, l);
        ZeventManager.getInstance().addBufferedListener(AgentUnscheduleZevent.class, l);
    }
    
}
