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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
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
    private final Map<Integer, Long> agentRestartTimstampMap = new HashMap<Integer, Long>();
    /** agentIds */
    private final TreeSet<Integer> pendingRestarts = new TreeSet<Integer>();
    @SuppressWarnings("unused")
    private final Thread executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object LOCK = new Object();
    private AuthzSubject overlord;
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    @Autowired
    public AgentPluginSyncRestartThrottle(AuthzSubjectManager authzSubjectManager,
                                          ConcurrentStatsCollector concurrentStatsCollector) {
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.executor = startExecutorThread();
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void initialize() {
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_RESTARTS);
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_SYNC_PENDING_RESTARTS);
    }
    
    private Thread startExecutorThread() {
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
        rtn.start();
        return rtn;
    }

    private int restartAgents() {
        final int numRestarts = invalidateAndGetNumRecords();
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
                agentRestartTimstampMap.put(agentId, now);
            } catch (Exception e) {
                log.error(e,e);
            }
        }
        return i;
    }

    private int invalidateAndGetNumRecords() {
        final long now = System.currentTimeMillis();
        int rtn = 0;
        final Iterator<Entry<Integer, Long>> it=agentRestartTimstampMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<Integer, Long> entry = it.next();
            final Long timestamp = entry.getValue();
            if ((now - timestamp) >= RECORD_TIMEOUT) {
                it.remove();
                continue;
            }
            rtn++;
        }
        return rtn;
    }

    public void checkinAfterRestart(Integer agentId) {
        boolean removed = false;
        synchronized (LOCK) {
            removed = agentRestartTimstampMap.remove(agentId) != null;
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
    }

}
