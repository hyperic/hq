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

package org.hyperic.hq.agent.server.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentSynchronizer implements DiagnosticObject {
    
    private static final int NUM_WORKERS = 4;
    private final Log log = LogFactory.getLog(AgentSynchronizer.class.getName());
    private final Set<Integer> activeAgents = Collections.synchronizedSet(new HashSet<Integer>());
    private final LinkedList<AgentDataTransferJob> agentJobs =
        new LinkedList<AgentDataTransferJob>();
    /** used mainly for diagnostics.  map of job description and number of times it has run */
    private final Map<String, Integer> shortDiagInfo = new HashMap<String, Integer>();
    private final Map<String, Integer> fullDiagInfo = new HashMap<String, Integer>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private ConcurrentStatsCollector concurrentStatsCollector;

    @Autowired
    public AgentSynchronizer(ConcurrentStatsCollector concurrentStatsCollector,
                             DiagnosticsLogger diagnosticsLogger) {
        this.concurrentStatsCollector = concurrentStatsCollector;
        diagnosticsLogger.addDiagnosticObject(this);
    }
    
    @PostConstruct
    void initialize() {
        ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(NUM_WORKERS, new ThreadFactory() {
            private AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "AgentSynchronizer" + i.getAndIncrement());
            }
        });
        for (int i=0; i<NUM_WORKERS; i++) {
            SchedulerThread worker = new SchedulerThread("AgentSynchronizer" + i, i*1000);
            executor.scheduleWithFixedDelay(worker, i+1, NUM_WORKERS, TimeUnit.SECONDS);
        }
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_SYNC_JOB_QUEUE_ADDS);
    }
    
    public void addAgentJob(AgentDataTransferJob agentJob) {
        synchronized (agentJobs) {
            agentJobs.add(agentJob);
        }
        concurrentStatsCollector.addStat(1, ConcurrentStatsCollector.AGENT_SYNC_JOB_QUEUE_ADDS);
    }
    
    private class SchedulerThread implements Runnable {
        private final String name;
        private final long initialSleep;
        private SchedulerThread(String name, long initialSleep) {
            this.name = name;
            this.initialSleep = initialSleep;
        }
        public String toString() {
            return name;
        }
        public synchronized void run() {
            try {
                Thread.sleep(initialSleep);
            } catch (InterruptedException e) {
                log.debug(e,e);
            }
            while (!shutdown.get()) {
                try {
                    boolean hasMoreScheduleJobs = true;
                    while (hasMoreScheduleJobs) {
                        hasMoreScheduleJobs = syncData();
                    }
                    Thread.sleep(NUM_WORKERS*1000);
                } catch (Throwable t) {
                    log.error(t, t);
                }
            }
        }
    }

    private boolean syncData() throws Exception {
        AgentDataTransferJob job = null;
        synchronized (agentJobs) {
            job = agentJobs.poll();
        }
        if (job == null) {
            return false;
        }
        Integer agentId = null;
        try {
            agentId = job.getAgentId();
            final boolean debug = log.isDebugEnabled();
            boolean added = activeAgents.add(agentId);
            if (!added) {
                synchronized (agentJobs) {
                    agentJobs.add(job);
                }
                agentId = null;
                // return false so that this mechanism doesn't spin out of control
                // allow the other thread some time to get its job done
                return false;
            }
            if (debug) log.debug("executing agent data transfer agentId=" + agentId +
                                 " jobdesc=" + job.getJobDescription());
            job.execute();
            setDiags(job);
            synchronized (agentJobs) {
                return !agentJobs.isEmpty();
            }
        } finally {
            if (agentId != null) {
                activeAgents.remove(agentId);
            }
        }
    }

    private void setDiags(AgentDataTransferJob job) {
        synchronized(fullDiagInfo) {
            final String desc = job.getJobDescription() + ", agentId=" + job.getAgentId();
            final Integer runs = fullDiagInfo.get(desc);
            if (runs == null) {
                shortDiagInfo.put(desc, 1);
            } else {
                shortDiagInfo.put(desc, runs+1);
            }
        }
        synchronized(shortDiagInfo) {
            final String desc = job.getJobDescription();
            final Integer runs = shortDiagInfo.get(desc);
            if (runs == null) {
                shortDiagInfo.put(desc, 1);
            } else {
                shortDiagInfo.put(desc, runs+1);
            }
        }
    }

    public String getStatus() {
        Map<String, Integer> diags = null;
        synchronized(fullDiagInfo) {
            diags = new HashMap<String, Integer>(fullDiagInfo);
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("\nAgent Synchronizer Diagnostics (job desc - number of executes):\n");
        for (final Entry<String, Integer> entry : diags.entrySet()) {
            buf.append("    ").append(entry.getKey()).append(" - ")
                              .append(entry.getValue()).append("\n");
        }
        return buf.toString();
    }

    public String getShortStatus() {
        Map<String, Integer> diags = null;
        synchronized(shortDiagInfo) {
            diags = new HashMap<String, Integer>(shortDiagInfo);
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("\nAgent Synchronizer Diagnostics (job desc - number of executes):\n");
        for (final Entry<String, Integer> entry : diags.entrySet()) {
            buf.append("    ").append(entry.getKey()).append(" - ")
                              .append(entry.getValue()).append("\n");
        }
        return buf.toString();
    }

    public String getShortName() {
        return "agentSynchronizer";
    }

    public String getName() {
        return "Agent Synchronizer";
    }

}
