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
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
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
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AgentSynchronizer implements DiagnosticObject, ApplicationContextAware {
    
    private final int NUM_WORKERS;
    private static final long WAIT_TIME = 5 * MeasurementConstants.MINUTE;
    private static final int DEFAULT_NUM_WORKERS = 20;
    private final Log log = LogFactory.getLog(AgentSynchronizer.class.getName());
    private final Set<Integer> activeAgents = Collections.synchronizedSet(new HashSet<Integer>());
    private final LinkedList<StatefulAgentDataTransferJob> agentJobs =
        new LinkedList<StatefulAgentDataTransferJob>();
    /** used mainly for diagnostics.  map of job description and number of times it has run */
    private final Map<String, Integer> fullDiagInfo = new HashMap<String, Integer>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong executorNum = new AtomicLong(0);
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final AuthzSubject overlord;
    private final AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private ApplicationContext ctx;
    private ScheduledThreadPoolExecutor executor ; 

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
        if (DEFAULT_NUM_WORKERS > 0) {
            return DEFAULT_NUM_WORKERS;
        }
        else {
            int cpus = Runtime.getRuntime().availableProcessors();
            if (cpus > 4) {
                return 4;
            } else if (cpus <= 1) {
                return 1;
            } else {
                return cpus;
            }
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
        this.executor =
            new ScheduledThreadPoolExecutor(NUM_WORKERS, new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);
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
        concurrentStatsCollector.register(new StatCollector() {
            public long getVal() throws StatUnreachableException {
                synchronized (agentJobs) {
                    return agentJobs.size();
                }
            }
            public String getId() {
                return ConcurrentStatsCollector.AGENT_SYNCHRONIZER_QUEUE_SIZE;
            }
        });
    }
    
    public void addAgentJob(AgentDataTransferJob agentJob) {
        addAgentJob(agentJob, false);
    }

    /**
     * @param agentJob job to execute in the background
     * @param isPriority - will add job to the top of the list
     */
    public void addAgentJob(AgentDataTransferJob agentJob, boolean isPriority) {
        if (log.isDebugEnabled()) log.debug("adding job=" + agentJob);
        synchronized (agentJobs) {
            if (isPriority) {
                agentJobs.add(new StatefulAgentDataTransferJob(agentJob));
            } else {
                agentJobs.addFirst(new StatefulAgentDataTransferJob(agentJob));
            }
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
        @Override
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
        StatefulAgentDataTransferJob job = null;
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
            boolean added;
            if (!job.canRun()) {
                added = false;
            } else {
                added = activeAgents.add(agentId);
            }
            if (!added) {
                reAddJob(job);
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

    private void executeJob(final StatefulAgentDataTransferJob job) throws InterruptedException {
        final String name = Thread.currentThread().getName() + "-" + executorNum.getAndIncrement();
        final Thread thread = new Thread(name) {
            @Override
            public void run() {
                job.setLastRuntime();
                if (agentIsPingable(job)) {
                    try {
                        job.execute();
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException) {
                            log.warn("jobdesc=" + job.getJobDescription() + " was interrupted: " + e);
                            log.debug(e,e);
                        } else {
                            log.error(e,e);
                        }
                    }
                    return;
                } else {
                    log.warn("Could not ping agent in order to run job " + getJobInfo(job));
                }
            }
        };
        thread.start();
        thread.join(WAIT_TIME);
        // if the thread is alive just try to interrupt it and keep going
        final boolean threadIsAlive = thread.isAlive();
        final boolean jobWasSuccessful = job.wasSuccessful();
        final AvailabilityManager availabilityManager = ctx.getBean(AvailabilityManager.class);
        final boolean platformIsAvailable =
            availabilityManager.platformIsAvailableOrUnknown(job.getAgentId()) || isInRestartState(job.getAgentId());
        if (jobWasSuccessful) {
            // do nothing, this is good!
            return;
        } else if (platformIsAvailable) {
            if (threadIsAlive) {
                thread.interrupt();
            }
            job.incrementFailures();
            if (job.discardJob()) {
                job.onFailure("Too many failures on agent " + job.getAgentId());
            } else {
                reAddJob(job);
                if (threadIsAlive) {
                    log.warn("AgentDataTransferJob=" + getJobInfo(job) +
                             " has take more than " + WAIT_TIME/1000/60 +
                             " minutes to run.  The agent appears alive so therefore the job was" +
                             " interrupted and requeued.  Job threadName={" + thread.getName() + "}");
                } else {
                    log.warn("AgentDataTransferJob=" + getJobInfo(job) +
                             " died and was not successful.  The agent appears alive and" +
                             " therefore the job was requeued. " +
                             " Job threadName={" + thread.getName() + "}");
                }
            }
        } else {
            if (threadIsAlive) {
                thread.interrupt();
                log.warn("AgentDataTransferJob=" + getJobInfo(job) +
                         " has take more than " + WAIT_TIME/1000/60 +
                         " minutes to run.  Discarding job threadName={" + thread.getName() + "}");
            }
            // Can't ping agent and platform availability is down, therefore agent must be down
            job.onFailure("Platform associated with agent " + job.getAgentId() + " is not available");
        }
    }

    private boolean isInRestartState(int agentId) {
        return agentPluginSyncRestartThrottle.getAgentIdsInRestartState().containsKey(agentId);
    }

    private boolean reAddJob(StatefulAgentDataTransferJob job) {
        if (job.discardJob()) {
            return false;
        }
        synchronized (agentJobs) {
            agentJobs.add(job);
        }
        return true;
    }

    private boolean agentIsPingable(AgentDataTransferJob job) {
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
        final String address = getAgentAddress(job.getAgentId());
        return new StringBuilder(desc.length() + 32)
            .append("{agentId=").append(job.getAgentId())
            .append(", agentAddress=").append(address)
            .append(", desc=").append(desc).append("}")
            .toString();
    }
    
    private String getAgentAddress(int agentId) {
        final AgentManager agentManager = ctx.getBean(AgentManager.class);
        if (agentManager == null) {
            return "";
        }
        Agent agent = agentManager.getAgent(agentId);
        if (agent == null) {
            return "";
        }
        return agent.getAddress();
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
    }

    public String getStatus() {
        return getStatus(fullDiagInfo);
    }

    public String getShortStatus() {
        return getStatus(fullDiagInfo);
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

    private class StatefulAgentDataTransferJob implements AgentDataTransferJob {
        private static final int MAX_FAILURES = 60;
        private static final long TIME_BTWN_RUNS = MeasurementConstants.MINUTE;
        private final AgentDataTransferJob job;
        private int numFailures = 0;
        private long lastRuntime = Long.MIN_VALUE;
        private StatefulAgentDataTransferJob(AgentDataTransferJob job) {
            this.job = job;
        }
        public void setLastRuntime() {
            lastRuntime = now();
        }
        public int getAgentId() {
            return job.getAgentId();
        }
        public String getJobDescription() {
            return job.getJobDescription();
        }
        public void execute() {
            job.execute();
        }
        public boolean wasSuccessful() {
            return job.wasSuccessful();
        }
        private void incrementFailures() {
            numFailures++;
        }
        public void onFailure(String reason) {
            job.onFailure(reason);
        }
        private boolean discardJob() {
            return numFailures >= MAX_FAILURES;
        }
        private boolean canRun() {
            if (numFailures >= MAX_FAILURES) {
                return false;
            }
            if (lastRuntime != Long.MIN_VALUE && (lastRuntime+TIME_BTWN_RUNS) > now()) {
                return false;
            }
            return true;
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
    
    @PreDestroy
    public void shutdown() {
        shutdown.set(true);
        this.executor.shutdown() ;
    }//EOM 

}
