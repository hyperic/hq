/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMWare, Inc.
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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.session.AgentDataTransferJob;
import org.hyperic.hq.agent.server.session.AgentSynchronizer;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to schedule and unschedule metrics for a given entity. The
 * schedule operation is synchronized to throttle rescheduling.
 */
@Component
public class AgentScheduleSynchronizer {

    private final Log log = LogFactory.getLog(AgentScheduleSynchronizer.class);

    @Autowired
    private ZeventEnqueuer zEventManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private ConcurrentStatsCollector concurrentStatsCollector;
    @Autowired
    private AgentSynchronizer agentSynchronizer;
    @Autowired
    private TransactionRetry transactionRetry;
    @Autowired
    private SRNManager srnManager;
    @Autowired
    private MeasurementProcessor measurementProcessor;

    private final Map<Integer, Collection<AppdefEntityID>> scheduleAeids =
        new HashMap<Integer, Collection<AppdefEntityID>>();

    private final Map<Integer, Collection<AppdefEntityID>> unscheduleAeids =
        new HashMap<Integer, Collection<AppdefEntityID>>();
    
    @PostConstruct
    void initialize() {
        ZeventListener<Zevent> l = getScheduleListener();
        concurrentStatsCollector.register(new StatCollector() {
            public long getVal() throws StatUnreachableException {
                synchronized (unscheduleAeids) {
                    return unscheduleAeids.size();
                }
            }
            public String getId() {
                return ConcurrentStatsCollector.UNSCHEDULE_QUEUE_SIZE;
            }
        });
        concurrentStatsCollector.register(new StatCollector() {
            public long getVal() throws StatUnreachableException {
                synchronized (scheduleAeids) {
                    return scheduleAeids.size();
                }
            }
            public String getId() {
                return ConcurrentStatsCollector.SCHEDULE_QUEUE_SIZE;
            }
        });
        zEventManager.addBufferedListener(AgentScheduleSyncZevent.class, l);
        zEventManager.addBufferedListener(AgentUnscheduleZevent.class, l);
    }

    public void unschedule(String agentToken, Collection<AppdefEntityID> aeids) {
        if (aeids == null || aeids.isEmpty()) {
            return;
        }
        final Integer agentId = getAgentId(agentToken);
        if (agentId == null) {
            return;
        }
        synchronized (unscheduleAeids) {
            Collection<AppdefEntityID> c = unscheduleAeids.get(agentId);
            if (c == null) {
                unscheduleAeids.put(agentId, aeids);
            } else {
                c.addAll(aeids);
            }
        }
        addScheduleJob(false, agentId);
    }

    private Integer getAgentId(String agentToken) {
        try {
            // will throw an AgentNotFoundException if the agent is not found
            return agentManager.getAgent(agentToken).getId();
        } catch (AgentNotFoundException e) {
            log.debug(e,e);
            return null;
        }
    }

    public Collection<AppdefEntityID> unscheduleNonEntities(String agentToken, Collection<AppdefEntityID> aeids) {
        if (aeids == null || aeids.isEmpty()) {
            return Collections.emptyList();
        }
        final boolean debug = log.isDebugEnabled();
        final Collection<AppdefEntityID> toUnschedule = new HashSet<AppdefEntityID>();
        final Integer agentId = getAgentId(agentToken);
        if (agentId == null) {
            return Collections.emptyList();
        }
        for (final AppdefEntityID aeid : aeids) {
            if (null == resourceManager.findResource(aeid)) {
                toUnschedule.add(aeid);
                if (debug) log.debug("unscheduling non-entity=" + aeid);
            }
        }
        if (!toUnschedule.isEmpty()) {
            synchronized (unscheduleAeids) {
                Collection<AppdefEntityID> c = unscheduleAeids.get(agentId);
                if (c == null) {
                    unscheduleAeids.put(agentId, aeids);
                } else {
                    c.addAll(aeids);
                }
            }
            addScheduleJob(false, agentId);
        }
        return new ArrayList<AppdefEntityID>(toUnschedule);
    }

    private ZeventListener<Zevent> getScheduleListener() {
        return new ZeventListener<Zevent>() {
            public void processEvents(List<Zevent> events) {
                final List<AppdefEntityID> toSchedule = new ArrayList<AppdefEntityID>(events.size());
                final Map<String, Collection<AppdefEntityID>> unscheduleMap =
                    new HashMap<String, Collection<AppdefEntityID>>(events.size());
                final boolean debug = log.isDebugEnabled();
                for (final Zevent z : events) {
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
                final Map<Integer, Collection<AppdefEntityID>> agentAppdefIds =
                    agentManager.getAgentMap(toSchedule);
                synchronized (scheduleAeids) {
                    for (final Map.Entry<Integer, Collection<AppdefEntityID>> entry : agentAppdefIds.entrySet()) {
                        final Integer agentId = entry.getKey();
                        final Collection<AppdefEntityID> eids = entry.getValue();
                        Collection<AppdefEntityID> tmp;
                        if (null == (tmp = scheduleAeids.get(agentId))) {
                            tmp = new HashSet<AppdefEntityID>(eids.size());
                            scheduleAeids.put(agentId, tmp);
                        }
                        tmp.addAll(eids);
                        addScheduleJob(true, agentId);
                    }
                }
                synchronized (unscheduleAeids) {
                    for (Map.Entry<String, Collection<AppdefEntityID>> entry : unscheduleMap.entrySet()) {
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
                        addScheduleJob(false, agentId);
                    }
                }
            }
            public String toString() {
                return "AgentScheduleSyncListener";
            }
        };
    }

    private void addScheduleJob(final boolean schedule, final Integer agentId) {
        if (agentId == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("adding " + (schedule ? "schedule" : "unschedule") + " job for agentId=" + agentId);
        }
        final AgentDataTransferJob job = new AgentDataTransferJob() {
            private Collection<AppdefEntityID> aeids;
            private AtomicBoolean success = new AtomicBoolean(false);
            public String toString() {
                return getJobDescription() + ", agentId=" + agentId;
            }
            public String getJobDescription() {
                if (schedule) {
                    return "Agent Schedule Job";
                } else {
                    return "Agent UnSchedule Job";
                }
            }
            public int getAgentId() {
                return agentId;
            }
            public void execute() {
                final Map<Integer, Collection<AppdefEntityID>> aeidMap =
                    (schedule) ? scheduleAeids : unscheduleAeids;
                synchronized (aeidMap) {
                    aeids = aeidMap.remove(agentId);
                }
                if (aeids != null && !aeids.isEmpty()) {
                    runSchedule(schedule, agentId, aeids);
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                success.set(true);
            }
            public void onFailure(String reason) {
                log.warn("could not schedule aeids=" + aeids + " to agentId=" + agentId + ": " + reason);
            }
            public boolean wasSuccessful() {
                return success.get();
            }
        };
        agentSynchronizer.addAgentJob(job);
    }
    
    private void runSchedule(final boolean schedule, final Integer agentId,
                             final Collection<AppdefEntityID> aeids) {
        final Runnable runner = new Runnable() {
            public void run() {
                _runSchedule(schedule, agentId, aeids);
            }
        };
        transactionRetry.runTransaction(runner, 3, 1000);
    }

    private void _runSchedule(final boolean schedule, final Integer agentId,
                              final Collection<AppdefEntityID> aeids) {
        try {
            SessionManager.runInSession(new SessionRunner() {
                public void run() throws Exception {
                    final Agent agent = agentManager.getAgent(agentId);
                    if (agent == null) {
                        return;
                    }
                    if (schedule) {
                        if (log.isDebugEnabled()) {
                            log.debug("scheduling " + aeids.size() + " resources to agentid=" +
                                       agent.getId());
                        }
                        srnManager.schedule(aeids, false, true);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("unscheduling " + aeids.size() + " resources to agentid=" +
                                       agent.getId());
                        }
                        measurementProcessor.unschedule(agent.getAgentToken(), aeids);
                    }
                }
                public String getName() {
                    if (schedule) {
                        return "Schedule";
                    } else {
                        return "Unschedule";
                    }
                }
            });
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

}
