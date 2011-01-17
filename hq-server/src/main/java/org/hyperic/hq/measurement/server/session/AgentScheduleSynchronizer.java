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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is used to schedule and unschedule metrics for a given entity. The
 * schedule operation is synchronized to throttle rescheduling.
 */
@Component
public class AgentScheduleSynchronizer {

    private final Log log = LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private ZeventEnqueuer zEventManager;

    private AgentManager agentManager;

    private ResourceManager resourceManager;

    private final Map<Integer, Collection<AppdefEntityID>> scheduleAeids = new HashMap<Integer, Collection<AppdefEntityID>>();

    private final Map<Integer, Collection<AppdefEntityID>> unscheduleAeids = new HashMap<Integer, Collection<AppdefEntityID>>();

    private final Set<Integer> activeAgents = Collections.synchronizedSet(new HashSet<Integer>());

    private ConcurrentStatsCollector concurrentStatsCollector;
    private MeasurementProcessor measurementProcessor;

    private static final int NUM_WORKERS = 4;

    @Autowired
    public AgentScheduleSynchronizer(ZeventEnqueuer zEventManager, AgentManager agentManager,
                                     MeasurementProcessor measurementProcessor,
                                     ConcurrentStatsCollector concurrentStatsCollector,
                                     ResourceManager resourceManager) {
        this.zEventManager = zEventManager;
        this.agentManager = agentManager;
        this.measurementProcessor = measurementProcessor;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.resourceManager = resourceManager;
    }

    @PostConstruct
    void initialize() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(NUM_WORKERS,
            new ThreadFactory() {
                private AtomicLong i = new AtomicLong(0);

                public Thread newThread(Runnable r) {
                    return new Thread(r, "AgentScheduler" + i.getAndIncrement());
                }
            });
        for (int i = 0; i < NUM_WORKERS; i++) {
            SchedulerThread worker = new SchedulerThread("AgentScheduler" + i);
            executor.scheduleWithFixedDelay(worker, i + 1, NUM_WORKERS, TimeUnit.SECONDS);
        }

        ZeventListener<Zevent> l = new ZeventListener<Zevent>() {

            public void processEvents(List<Zevent> events) {
                final List<AppdefEntityID> toSchedule = new ArrayList<AppdefEntityID>(events.size());
                final Map<String, Collection<AppdefEntityID>> unscheduleMap = new HashMap<String, Collection<AppdefEntityID>>(
                    events.size());
                final boolean debug = log.isDebugEnabled();
                for (final Zevent z : events) {
                    if (z instanceof AgentScheduleSyncZevent) {
                        AgentScheduleSyncZevent event = (AgentScheduleSyncZevent) z;
                        toSchedule.addAll(event.getEntityIds());
                        if (debug)
                            log.debug("Schduling eids=[" + event.getEntityIds() + "]");
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
                        if (debug)
                            log.debug("Unschduling eids=[" + event.getEntityIds() + "]");
                    }
                }

                synchronized (scheduleAeids) {
                    for (AppdefEntityID id : toSchedule) {
                        Resource resource = resourceManager.findResourceById(id.getId());
                        // Resource may have been deleted while we are
                        // processing this
                        if (resource != null) {
                            Collection<AppdefEntityID> tmp;
                            final int agentId = resource.getAgent().getId();
                            if (null == (tmp = scheduleAeids.get(agentId))) {
                                tmp = new HashSet<AppdefEntityID>();
                                scheduleAeids.put(agentId, tmp);
                            }
                            tmp.add(id);
                        }
                    }
                }
                synchronized (unscheduleAeids) {
                    for (final Map.Entry<String, Collection<AppdefEntityID>> entry : unscheduleMap
                        .entrySet()) {
                        final String token = entry.getKey();
                        final Collection<AppdefEntityID> eids = entry.getValue();
                        Integer agentId;
                        try {
                            agentId = agentManager.getAgent(token).getId();
                        } catch (AgentNotFoundException e) {
                            log.warn("Could not get agentToken=" + token +
                                     " from db to unschedule: " + e);
                            continue;
                        }
                        Collection<AppdefEntityID> tmp;
                        if (null == (tmp = unscheduleAeids.get(agentId))) {
                            tmp = new HashSet<AppdefEntityID>(eids.size());
                            unscheduleAeids.put(agentId, tmp);
                        }
                        tmp.addAll(eids);
                    }
                }
            }

            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };

        zEventManager.addBufferedListener(AgentScheduleSyncZevent.class, l);
        zEventManager.addBufferedListener(AgentUnscheduleZevent.class, l);
        concurrentStatsCollector.register(ConcurrentStatsCollector.SCHEDULE_QUEUE_SIZE);
        concurrentStatsCollector.register(ConcurrentStatsCollector.UNSCHEDULE_QUEUE_SIZE);
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
                synchronized (scheduleAeids) {
                    concurrentStatsCollector.addStat(scheduleAeids.size(),
                        ConcurrentStatsCollector.SCHEDULE_QUEUE_SIZE);
                }
                synchronized (unscheduleAeids) {
                    concurrentStatsCollector.addStat(unscheduleAeids.size(),
                        ConcurrentStatsCollector.UNSCHEDULE_QUEUE_SIZE);
                }
                while (hasMoreToSchedule || hasMoreToUnschedule) {
                    hasMoreToUnschedule = syncMetrics(unscheduleAeids, false);
                    hasMoreToSchedule = syncMetrics(scheduleAeids, true);
                }
            } catch (Throwable t) {
                log.error(t, t);
            }
        }

        private boolean syncMetrics(Map<Integer, Collection<AppdefEntityID>> scheduleAeids,
                                    final boolean schedule) throws Exception {
            Integer agentId = null;
            Collection<AppdefEntityID> aeids = null;
            final boolean debug = log.isDebugEnabled();
            try {
                synchronized (scheduleAeids) {
                    if (scheduleAeids.isEmpty()) {
                        return false;
                    }
                    for (Map.Entry<Integer, Collection<AppdefEntityID>> entry : scheduleAeids
                        .entrySet()) {
                        agentId = entry.getKey();
                        aeids = entry.getValue();
                        boolean added = activeAgents.add(agentId);
                        if (added) {
                            if (debug)
                                log.debug("scheduling agentId=" + agentId);
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
                    if (debug)
                        log.debug("agentId=" + agentId + " is finished scheduling");
                    activeAgents.remove(agentId);
                }
            }
        }

        @Transactional
        private void runSchedule(final boolean schedule, final Integer agentId,
                                 final Collection<AppdefEntityID> aeids) throws Exception {

            final Agent agent = agentManager.findAgent(agentId);
            if (schedule) {
                if (log.isDebugEnabled()) {
                    log.debug("scheduling " + aeids.size() + " resources to agentid=" +
                              agent.getId());
                }
                measurementProcessor.scheduleEnabled(agent, aeids);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("unscheduling " + aeids.size() + " resources to agentid=" +
                              agent.getId());
                }
                measurementProcessor.unschedule(agent.getAgentToken(), aeids);
            }

        }
    }

}
