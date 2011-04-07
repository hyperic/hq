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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentPluginSyncRestartThrottle {
    
    private static final Log log = LogFactory.getLog(AgentPluginSyncRestartThrottle.class);
    private static final int MAX_CONCURRENT_RESTARTS = 20;
    private static final long RECORD_TIMEOUT = 10 * MeasurementConstants.MINUTE;
    /**
     *  agentId to timestamp of agent reboot attempt time
     *  if agent does not check in by RECORD_TIMEOUT then the record is expired
     */
    private final Map<Integer, Long> agentRestartTimestampMap = new HashMap<Integer, Long>();
    /** agentIds */
    private final TreeSet<Integer> pendingRestarts = new TreeSet<Integer>();
    @SuppressWarnings("unused")
    private Thread throttler;
    private ScheduledThreadPoolExecutor invalidator;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object LOCK = new Object();
    private AuthzSubject overlord;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private TransactionRetry transactionRetry;
    
    @Autowired
    public AgentPluginSyncRestartThrottle(AuthzSubjectManager authzSubjectManager,
                                          ConcurrentStatsCollector concurrentStatsCollector,
                                          TransactionRetry transactionRetry) {
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.transactionRetry = transactionRetry;
    }
    
    @PostConstruct
    public void initialize() {
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_RESTARTS);
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_PENDING_RESTARTS);
        throttler = startThrottlerThread();
        invalidator = startExecutor();
    }
    
    private ScheduledThreadPoolExecutor startExecutor() {
        ScheduledThreadPoolExecutor rtn = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "PluginSyncRestartInvalidator-" + i.getAndIncrement());
            }
        });
        final Runnable runner = new Runnable() {
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
        };
        rtn.scheduleWithFixedDelay(runner, RECORD_TIMEOUT, RECORD_TIMEOUT, TimeUnit.MILLISECONDS);
        return rtn;
    }

    public Set<Integer> getQueuedAgentIds() {
        synchronized (LOCK) {
            return new HashSet<Integer>(pendingRestarts);
        }
    }
    
    public Map<Integer, Long> getAgentIdsInRestartState() {
        synchronized (LOCK) {
            return new HashMap<Integer, Long>(agentRestartTimestampMap);
        }
    }
    
    private Thread startThrottlerThread() {
        final Thread rtn = new Thread("AgentPluginSyncRestartThrottle") {
            public void run() {
                while (!shutdown.get()) {
                    try {
                        int restarts = 0;
                        synchronized (LOCK) {
                            // wait 10 secs
                            LOCK.wait(10000);
                            restarts = restartAgents();
                        }
                        if (restarts > 0) {
                            concurrentStatsCollector.addStat(
                           	    restarts, ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_RESTARTS);
                        }
                    } catch (Throwable t) {
                        log.error(t,t);
                    }
                }
            }
        };
        rtn.setDaemon(true);
        rtn.start();
        return rtn;
    }

    private int restartAgents() {
        synchronized (LOCK) {
            final int numRestarts = getNumRecords(false);
            if (pendingRestarts.isEmpty()) {
                return 0;
            }
            if (numRestarts >= MAX_CONCURRENT_RESTARTS) {
                return 0;
            }
            final int max = MAX_CONCURRENT_RESTARTS - numRestarts;
            final long now = System.currentTimeMillis();
            final AgentManager agentManager = Bootstrap.getBean(AgentManager.class);
            int i=0;
            for (i=0; i<max; i++) {
                final Integer agentId = pendingRestarts.pollFirst();
                if (agentId == null) {
                    break;
                }
                try {
                    agentManager.restartAgent(overlord, agentId);
                    agentRestartTimestampMap.put(agentId, now);
                } catch (Exception e) {
                    log.error(e,e);
                }
            }
            return i;
        }
    }

    private int getNumRecords(boolean invalidate) {
        final long now = System.currentTimeMillis();
        int rtn = 0;
        final Set<Integer> restartFailures = new HashSet<Integer>();
        synchronized (LOCK) {
            final Iterator<Entry<Integer, Long>> it=agentRestartTimestampMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<Integer, Long> entry = it.next();
                final Long timestamp = entry.getValue();
                if ((now - timestamp) >= RECORD_TIMEOUT) {
                    if (invalidate) {
                        restartFailures.add(entry.getKey());
	                    it.remove();
                    }
                    continue;
                }
                rtn++;
            }
        }
        if (invalidate) {
            final boolean debug = log.isDebugEnabled();
            if (!restartFailures.isEmpty()) {
                final PluginManager pm = Bootstrap.getBean(PluginManager.class);
                for (final Integer agentId : restartFailures) {
                    if (debug) log.debug("invalidating restart status for agentId=" + agentId);
                    pm.updateAgentPluginSyncStatusInNewTran(
                        agentId, AgentPluginStatusEnum.SYNC_IN_PROGRESS, AgentPluginStatusEnum.SYNC_FAILURE);
                }
            }
        }
        return rtn;
    }

    public void checkinAfterRestart(Integer agentId) {
        boolean removed = false;
        synchronized (LOCK) {
            removed = agentRestartTimestampMap.remove(agentId) != null;
        }
        if (log.isDebugEnabled()) {
            log.debug("agentId=" + agentId + " checking in after reboot, removed = " + removed);
        }
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
    
    public void shutdown() {
        shutdown.set(true);
        LOCK.notifyAll();
        invalidator.shutdown();
    }

}
