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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import org.hyperic.hq.appdef.server.session.AgentPluginSyncRestartThrottle;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AgentSynchronizer implements DiagnosticObject, ApplicationContextAware {
    
    private final int NUM_WORKERS;
    private static final long WAIT_TIME = 5 * MeasurementConstants.MINUTE;
    private final Log log = LogFactory.getLog(AgentSynchronizer.class.getName());
    private final Set<Integer> activeAgents = Collections.synchronizedSet(new HashSet<Integer>());
    private final LinkedList<AgentDataTransferJob> agentJobs =
        new LinkedList<AgentDataTransferJob>();
    /** used mainly for diagnostics.  map of job description and number of times it has run */
    private final Map<String, Integer> shortDiagInfo = new HashMap<String, Integer>();
    private final Map<String, Integer> fullDiagInfo = new HashMap<String, Integer>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong executorNum = new AtomicLong(0);
    private ConcurrentStatsCollector concurrentStatsCollector;
    private AuthzSubject overlord;
    private AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private ApplicationContext ctx;

    @Autowired
    public AgentSynchronizer(ConcurrentStatsCollector concurrentStatsCollector,
                             DiagnosticsLogger diagnosticsLogger,
                             AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                             AuthzSubjectManager authzSubjectManager) {
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.overlord = authzSubjectManager.getOverlordPojo();
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        diagnosticsLogger.addDiagnosticObject(this);
        NUM_WORKERS = getNumWorkers();
    }
    
    private int getNumWorkers() {
        int cpus = Runtime.getRuntime().availableProcessors();
        if (cpus > 4) {
            return 4;
        } else if (cpus <= 1) {
            return 1;
        } else {
            return cpus;
        }
    }

    public Set<Integer> getJobListByDescription(Collection<String> descriptions) {
        List<AgentDataTransferJob> jobs;
        final Set<String> descs = new HashSet<String>(descriptions);
        synchronized (agentJobs) {
            jobs = new ArrayList<AgentDataTransferJob>(agentJobs);
        }
        final Set<Integer> rtn = new HashSet<Integer>();
        for (final AgentDataTransferJob job : jobs) {
            if (descs.contains(job.getJobDescription())) {
                rtn.add(job.getAgentId());
            }
        }
        return rtn;
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
        log.info("starting AgentSynchronizer with " + NUM_WORKERS + " threads");
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

    private boolean syncData() {
        AgentDataTransferJob j = null;
        synchronized (agentJobs) {
            j = agentJobs.poll();
        }
        if (j == null) {
            return false;
        }
        final AgentDataTransferJob job = j;
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
            executeJob(job);
            setDiags(job);
            synchronized (agentJobs) {
                return !agentJobs.isEmpty();
            }
        } catch (Exception e) {
            throw new SystemException("Error executing " + getJobInfo(job), e);
        } finally {
            if (agentId != null) {
                activeAgents.remove(agentId);
            }
        }
    }

    private void executeJob(final AgentDataTransferJob job) throws InterruptedException {
        final AvailabilityManager availabilityManager = ctx.getBean(AvailabilityManager.class);
        final String name = Thread.currentThread().getName() + "-" + executorNum.getAndIncrement();
        final Thread thread = new Thread(name) {
            public void run() {
                if (agentIsAlive(job)) {
                    try {
                        job.execute();
                    } catch (Throwable e) {
                        job.onFailure();
                        if (isInRestartState(job.getAgentId())) {
                            log.warn("received error while trying to communicate with agentId=" +
                                job.getAgentId() + " while it is restarting.  job=" +
                                getJobInfo(job) + ": " + e);
                            log.debug(e,e);
                        } else {
                            log.error(e,e);
                        }
                    }
                    return;
                }
                if (availabilityManager.platformIsAvailable(job.getAgentId())) {
                    // agent is busy but up and running, add job back to the queue
                    if (log.isDebugEnabled()) {
                        log.debug("cannot ping agentId=" + job.getAgentId() +
                                  " but availabilityManager shows that it is available, " +
                                  "rescheduling job=" + getJobInfo(job));
                    }
                    reAddJob(job, true);
                } else {
                    log.warn("Could not ping agent in order to run job " + getJobInfo(job));
                    job.onFailure();
                }
            }
            private boolean isInRestartState(int agentId) {
                return agentPluginSyncRestartThrottle.getAgentIdsInRestartState().containsKey(agentId);
            }
        };
        thread.start();
        thread.join(WAIT_TIME);
        // if the thread is alive just try to interrupt it and keep going
        final boolean threadIsAlive = thread.isAlive();
        final boolean jobWasSuccessful = job.wasSuccessful();
        final boolean platformIsAvailable = availabilityManager.platformIsAvailable(job.getAgentId());
        if (jobWasSuccessful) {
            // do nothing, this is good!
        } else if (!jobWasSuccessful && platformIsAvailable) {
            if (threadIsAlive) {
                thread.interrupt();
            }
            log.warn("AgentDataTransferJob=" + getJobInfo(job) +
                     " has take more than " + WAIT_TIME/1000/60 +
                     " minutes to run.  The agent appears alive, and therefore will interrupt the " +
                     "current job and requeue it.  Job threadName={" + thread.getName() + "}");
            reAddJob(job, false);
        } else if (threadIsAlive) {
            thread.interrupt();
            log.warn("AgentDataTransferJob=" + getJobInfo(job) +
                     " has take more than " + WAIT_TIME/1000/60 +
                     " minutes to run.  Disregarding job threadName={" + thread.getName() + "}");
        }
    }
    
    private void reAddJob(AgentDataTransferJob job, boolean wait) {
        synchronized (agentJobs) {
            // if the job queue isn't very full then we don't want to keep spinning
            // as a result of re-adding this job.  Instead lets just sleep for a few
            // secs and then re-issue the job.
            if (wait) {
                if (agentJobs.size() < NUM_WORKERS) {
                    try {
                        agentJobs.wait(5000);
                    } catch (InterruptedException e) {
                        log.debug(e,e);
                    }
                }
            }
            agentJobs.add(job);
        }
    }

    private boolean agentIsAlive(AgentDataTransferJob job) {
        try {
            // XXX need to set this in the constructor
            final AgentManager agentManager = ctx.getBean(AgentManager.class);
            agentManager.pingAgent(overlord, job.getAgentId());
        } catch (Exception e) {
            log.debug(e,e);
            return false;
        }
        return true;
    }

    private String getJobInfo(AgentDataTransferJob job) {
        final String desc = job.getJobDescription();
        return new StringBuilder(desc.length() + 32)
            .append("{agentId=").append(job.getAgentId())
            .append(", desc=").append(desc).append("}")
            .toString();
    }

    private void setDiags(AgentDataTransferJob job) {
        synchronized(fullDiagInfo) {
            final String desc = job.getJobDescription() + ", agentId=" + job.getAgentId();
            final Integer runs = fullDiagInfo.get(desc);
            if (runs == null) {
                fullDiagInfo.put(desc, 1);
            } else {
                fullDiagInfo.put(desc, (runs+1));
            }
        }
        synchronized(shortDiagInfo) {
            final String desc = job.getJobDescription();
            final Integer runs = shortDiagInfo.get(desc);
            if (runs == null) {
                shortDiagInfo.put(desc, 1);
            } else {
                shortDiagInfo.put(desc, (runs+1));
            }
        }
    }

    public String getStatus() {
        return getStatus(fullDiagInfo);
    }

    public String getShortStatus() {
        return getStatus(shortDiagInfo);
    }
    
    private String getStatus(Map<String, Integer> diag) {
        Map<String, Integer> diags = null;
        synchronized(diag) {
            diags = new HashMap<String, Integer>(diag);
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("\nTop 10 - Agent Synchronizer Diagnostics (job desc - number of executes):\n");
        final List<Entry<String, Integer>> diagList =
            new ArrayList<Entry<String, Integer>>(diags.entrySet());
        Collections.sort(diagList, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
                if (e1 == e2) {
                    return 0;
                }
                return e2.getValue().compareTo(e1.getValue());
            }
        });
        int i=0;
        for (final Entry<String, Integer> entry : diagList) {
            if (i++ >= 10) {
                break;
            }
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

}
