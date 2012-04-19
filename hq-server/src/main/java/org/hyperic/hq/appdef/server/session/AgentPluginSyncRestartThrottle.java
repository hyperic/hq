/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.appdef.server.session;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class AgentPluginSyncRestartThrottle {
    
    private static final Log log = LogFactory.getLog(AgentPluginSyncRestartThrottle.class);
    private static final int MAX_CONCURRENT_RESTARTS = 20;
    private static final long RECORD_TIMEOUT = 10 * MeasurementConstants.MINUTE;
    private static final long RESTART_PAUSE_TIME = 60000;
    /**
     *  agentId to timestamp of agent reboot attempt time
     *  if agent does not check in by RECORD_TIMEOUT then the record is expired
     */
    private final Map<Integer, Long> agentRestartTimestampMap = new HashMap<Integer, Long>();
    /** agentIds */
    private final TreeSet<Integer> pendingRestarts = new TreeSet<Integer>();
    /** [HHQ-4882] - agents need to be up for at least 60 secs before being restarted */
    private final HashMap<Integer, Long> lastCheckin = new HashMap<Integer, Long>();
    private final TaskScheduler taskScheduler;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object LOCK = new Object();
    private final AuthzSubject overlord;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final TransactionRetry transactionRetry;
    
    @Autowired
    public AgentPluginSyncRestartThrottle(AuthzSubjectManager authzSubjectManager,
                                          ConcurrentStatsCollector concurrentStatsCollector,
                                          TransactionRetry transactionRetry,
                                          @Value("#{scheduler}")TaskScheduler taskScheduler) {
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.transactionRetry = transactionRetry;
        this.taskScheduler = taskScheduler;
    }
    
    @PostConstruct
    public void initialize() {
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_RESTARTS);
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_PENDING_RESTARTS);
        startThrottlerThread();
        taskScheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    final boolean debug = log.isDebugEnabled();
                    if (debug) log.debug("starting PluginSyncRestartInvalidator");
                    final Runnable runner = new Runnable() {
                        public void run() {
                            getNumRecords(true);
                        }
                    };
                    transactionRetry.runTransaction(runner, 3, 1000);
                    if (debug) log.debug("done PluginSyncRestartInvalidator");
                } catch (Throwable t) {
                    log.error("ERROR running PluginSyncRestartInvalidator: " + t,t);
                }
            }
        }, new Date(System.currentTimeMillis() + RECORD_TIMEOUT),  RECORD_TIMEOUT);
    }
    
    public Set<Integer> getQueuedAgentIds() {
        synchronized (LOCK) {
            return new HashSet<Integer>(pendingRestarts);
        }
    }
    
    /**
     * @return {@link Map} of {@link Integer} agentId to {@link Long} time (ms) which represents
     * the last time the agentId checked in after a restart
     */
    public Map<Integer, Long> getLastCheckinInfo() {
        synchronized (LOCK) {
            return new HashMap<Integer, Long>(lastCheckin);
        }
    }
    
    /**
     * @return {@link Map} of {@link Integer} agentId to {@link Long} time (ms) which represents
     * when a restart was initiated on the agentId 
     */
    public Map<Integer, Long> getAgentIdsInRestartState() {
        synchronized (LOCK) {
            return new HashMap<Integer, Long>(agentRestartTimestampMap);
        }
    }
    
    private void startThrottlerThread() {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
                while (!shutdown.get()) {
                    try {
                        Set<Integer> toRestart;
                        synchronized (LOCK) {
                            // wait 10 secs
                            LOCK.wait(10000);
                            toRestart = getAgentsToRestart();
                        }
                        final long now = now();
                        for (final Integer agentId : toRestart) {
                            try {
                                // restart agents out of the LOCK since it blocks while
                                // communicating to them
                                agentManager.restartAgent(overlord, agentId);
                            } catch (Exception e) {
                                log.error(e,e);
                            } finally {
                                synchronized (LOCK) {
                                    agentRestartTimestampMap.put(agentId, now);
                                }
                            }
                        }
                        if (!toRestart.isEmpty()) {
                            concurrentStatsCollector.addStat(
                                toRestart.size(), ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_RESTARTS);
                        }
                    } catch (Throwable t) {
                        log.error(t,t);
                    }
                }
            }
        }, new Date(System.currentTimeMillis() + 5000));
    }

    private Set<Integer> getAgentsToRestart() {
        final Set<Integer> rtn = new HashSet<Integer>();
        synchronized (LOCK) {
            final int numRestarts = getNumRecords(false);
            if (pendingRestarts.isEmpty()) {
                return Collections.emptySet();
            }
            if (numRestarts >= MAX_CONCURRENT_RESTARTS) {
                return Collections.emptySet();
            }
            final int max = MAX_CONCURRENT_RESTARTS - numRestarts;
            int i=0;
            for (i=0; i<max; i++) {
                Integer agentId = pendingRestarts.pollFirst();
                if (agentId == null) {
                    break;
                }
                if (!canRestart(agentId)) {
                    pendingRestarts.add(agentId);
                    continue;
                }
                rtn.add(agentId);
            }
            return rtn;
        }
    }
    
    private boolean canRestart(Integer agentId) {
        synchronized (LOCK) {
            final Long restartTime = agentRestartTimestampMap.get(agentId);
            if (restartTime != null) {
                // Agent is currently restarting
                return false;
            }
            final Long last = lastCheckin.get(agentId);
            final long now = now();
            if (log.isDebugEnabled()) {
                log.debug("agentId=" + agentId +
                    " lastCheckin=" + ((last == null) ? null : TimeUtil.toString(last)) +
                    ", minRestartTime=" + ((last == null) ? TimeUtil.toString(now) : TimeUtil.toString(last+RESTART_PAUSE_TIME)));
            }
            if (last == null || now > (last + RESTART_PAUSE_TIME)) {
                return true;
            }
            return false;
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private int getNumRecords(final boolean invalidate) {
        final long now = System.currentTimeMillis();
        int rtn = 0;
        final Set<Integer> restartFailures = new HashSet<Integer>();
        final boolean debug = log.isDebugEnabled();
        synchronized (LOCK) {
            final Iterator<Entry<Integer, Long>> it=agentRestartTimestampMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<Integer, Long> entry = it.next();
                final Integer agentId = entry.getKey();
                final Long timestamp = entry.getValue();
                if ((now - timestamp) >= RECORD_TIMEOUT) {
                    if (invalidate) {
                        restartFailures.add(agentId);
                        it.remove();
                    }
                    continue;
                }
                rtn++;
            }
        }
        if (invalidate) {
            if (!restartFailures.isEmpty()) {
                final PluginManager pm = Bootstrap.getBean(PluginManager.class);
                for (final Integer agentId : restartFailures) {
                    if (debug) log.debug("invalidating restart status for agentId=" + agentId);
                    pm.updateAgentPluginSyncStatus(
                        agentId, AgentPluginStatusEnum.SYNC_IN_PROGRESS, AgentPluginStatusEnum.SYNC_FAILURE);
                }
            }
        }
        return rtn;
    }

    public void checkinAfterRestart(Integer agentId) {
        final boolean debug = log.isDebugEnabled();
        boolean removed = false;
        final long now = now();
        synchronized (LOCK) {
            removed = agentRestartTimestampMap.remove(agentId) != null;
            lastCheckin.put(agentId, now);
        }
        if (debug) log.debug("agentId=" + agentId + " checking in after reboot, removed = " + removed);
    }

    public void restartAgent(Integer agentId) {
        if (log.isDebugEnabled()) {
            log.debug("agentId=" + agentId + " added to list of pending reboots");
        }
        concurrentStatsCollector.addStat(1, ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_PENDING_RESTARTS);
        synchronized (LOCK) {
            pendingRestarts.add(agentId);
            LOCK.notifyAll();
        }
    }
    
    @PreDestroy
    public void shutdown() {
        shutdown.set(true);
        synchronized(LOCK) { 
            LOCK.notifyAll();
        }//EO synchronized block 
    }

}
