/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMware, Inc.
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

package org.hyperic.hq.measurement.agent.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.diagnostics.AgentDiagnosticObject;
import org.hyperic.hq.agent.diagnostics.AgentDiagnostics;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.agent.stats.AgentStatsCollector;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.MeasurementValueGetter;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.util.properties.PropertiesUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.ScheduledItem;
import org.hyperic.util.schedule.UnscheduledItemException;

/**
 * The schedule thread which maintains the schedule, and dispatches on them.
 * After data is retrieved, it is sent to the SenderThread which handles
 * depositing the results on disk, and sending them to the bizapp.
 */

public class ScheduleThread  extends AgentMonitorSimple implements Runnable, AgentDiagnosticObject {
    private static final String SCHEDULE_THREAD_METRICS_COLLECTED_TIME = AgentStatsCollector.SCHEDULE_THREAD_METRICS_COLLECTED_TIME;
    private static final String SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED = AgentStatsCollector.SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED;
    private static final String SCHEDULE_THREAD_METRIC_COLLECT_FAILED  = AgentStatsCollector.SCHEDULE_THREAD_METRIC_COLLECT_FAILED;

    // Agent properties configuration
    static final String PROP_POOLSIZE = "scheduleThread.poolsize."; // e.g. scheduleThread.poolsize.system=10
    static final String PROP_FETCH_LOG_TIMEOUT = "scheduleThread.fetchLogTimeout";
    static final String PROP_CANCEL_TIMEOUT = "scheduleThread.cancelTimeout";
    static final String PROP_QUEUE_SIZE = "scheduleThread.queuesize.";

    private boolean deductServerTimeDiff = true;

    // How often we check schedules when we think they are empty.
    private static final int ONE_SECOND = 1000;
    private static final int POLL_PERIOD = ONE_SECOND;
    private static final int UNREACHABLE_EXPIRE = (60 * 1000) * 5;

    private static final long FETCH_TIME  = 2000; // 2 seconds.
    private static final long CANCEL_TIME = 5000; // 5 seconds.
    private static final int  EXECUTOR_QUEUE_SIZE = 10000;

    private long logFetchTimeout = FETCH_TIME;
    private long cancelTimeout = CANCEL_TIME;

    private static final Log log = LogFactory.getLog(ScheduleThread.class.getName());

    // AppdefID -> Schedule
    private final Map<String,ResourceSchedule> schedules = new HashMap<String,ResourceSchedule>();
    // Should I shut down?
    private final AtomicBoolean shouldDie = new AtomicBoolean(false);
    // Interrupt object
    private final Object interrupter = new Object();
    // Hash of DSNs to their errors
    private final HashMap<String,String> errors = new HashMap<String,String>();
    private final Properties agentConfig; // agent.properties

    // Map of Executors, one per plugin
    private final HashMap<String,ThreadPoolExecutor> executors = new HashMap<String,ThreadPoolExecutor>();
    // Map of asynchronous MetricTasks pending confirmation
    private final HashMap<Future<?>,MetricTask> metricCollections = new HashMap<Future<?>,MetricTask>();
    // The executor confirming metric collections, cancelling tasks that exceed
    // our timeouts.
    private final ScheduledExecutorService metricVerificationService;
    private final ScheduledFuture<?> metricVerificationTask;
    private final ScheduledFuture<?> metricLoggingTask;
    
    private final MeasurementValueGetter manager;
    private final Sender sender;  // Guy handling the results
    private final Set<Integer> scheduled = new HashSet<Integer>();

    // Statistics
    private final Object statsLock = new Object();
    private long statNumMetricsFetched = 0;
    private long statNumMetricsFailed = 0;
    private long statTotFetchTime = 0;
    private long statNumMetricsScheduled = 0;
    private long statMaxFetchTime = Long.MIN_VALUE;
    private long statMinFetchTime = Long.MAX_VALUE;
    private final AgentStatsCollector statsCollector;
    private final Random rand = new Random();
    private final int offset;
    private final Map<AppdefEntityID, DiagInfo> diagInfo = new HashMap<AppdefEntityID, DiagInfo>();

    ScheduleThread(Sender sender, MeasurementValueGetter manager, Properties config) throws AgentStartException {
        this.statsCollector = AgentStatsCollector.getInstance();
        this.statsCollector.register(SCHEDULE_THREAD_METRIC_COLLECT_FAILED);
        this.statsCollector.register(SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED);
        this.statsCollector.register(SCHEDULE_THREAD_METRICS_COLLECTED_TIME);
        this.agentConfig = config;
        this.manager = manager;
        this.sender = sender;
        int tmp = getFudgeFactor();
        if (tmp <= 0) {
            offset = ONE_SECOND;
        } else {
            offset = tmp;
            log.info("fudgeFactor is set to " + offset + " ms");
        }

        String sLogFetchTimeout = agentConfig.getProperty(PROP_FETCH_LOG_TIMEOUT);
        if(sLogFetchTimeout != null){
            try {
                logFetchTimeout = Integer.parseInt(sLogFetchTimeout);
                log.info("Log fetch timeout set to " + logFetchTimeout);
            } catch(NumberFormatException exc){
                log.error("Invalid setting for " + PROP_FETCH_LOG_TIMEOUT + " value=" +
                           sLogFetchTimeout + ", using defaults.");
            }
        }

        String sCancelTimeout = agentConfig.getProperty(PROP_CANCEL_TIMEOUT);
        if(sCancelTimeout != null){
            try {
                cancelTimeout = Integer.parseInt(sCancelTimeout);
                log.info("Cancel timeout set to " + cancelTimeout);
            } catch(NumberFormatException exc){
                log.error("Invalid setting for " + PROP_CANCEL_TIMEOUT + " value=" +
                           sCancelTimeout + ", using defaults.");
            }
        }

        metricVerificationService = Executors.newSingleThreadScheduledExecutor();
        metricVerificationTask = metricVerificationService.scheduleAtFixedRate(new MetricVerificationTask(),
                                                                               POLL_PERIOD, POLL_PERIOD,
                                                                               TimeUnit.MILLISECONDS);
        metricLoggingTask = metricVerificationService.scheduleAtFixedRate(new MetricLoggingTask(),
                                                                          1, 600, TimeUnit.SECONDS);
        AgentDiagnostics.getInstance().addDiagnostic(this);

        // by default we the deduction feature is on
        Boolean deductServerOffset = PropertiesUtil.getBooleanValue(
                agentConfig.getProperty(ServerTimeDiff.PROP_DEDUCT_SERVER_TIME_DIFF), true);

        deductServerTimeDiff = deductServerOffset;
    }

    /**
     * Task for printing Executor statistics
     */
    private class MetricLoggingTask implements Runnable {
        public void run() {
            for (String plugin : executors.keySet()) {
                ThreadPoolExecutor executor = executors.get(plugin);
                if (log.isDebugEnabled()) {
                    log.debug("Plugin=" + plugin + ", " +
                              "CompletedTaskCount=" + executor.getCompletedTaskCount() + ", " +
                              "ActiveCount=" + executor.getActiveCount() + ", " +
                              "TaskCount=" + executor.getTaskCount() + ", " +
                              "PoolSize=" + executor.getPoolSize());
                }
            }
        }
    }

    /**
     * The MetricVerificationTask iterates over the list of FutureTasks that
     * have been submitted for execution.  Each task is checked for completion
     * and then removed.  For tasks that do not complete within the timeout
     * are cancelled, which will attempt to free up the executor running the
     * task.
     * NOTE: This will only work if the hung task is in an interrupt-able state
     *       i.e. sleep() or wait()
     */
    private class MetricVerificationTask implements Runnable  {
        public void run() {
            boolean isDebugEnabled = log.isDebugEnabled();
            synchronized (metricCollections) {
                if (isDebugEnabled && (metricCollections.size() > 0)) {
                    log.debug(metricCollections.size() + " metrics to validate.");
                }
                for (Iterator<Future<?>> i = metricCollections.keySet().iterator(); i.hasNext();) {
                    Future<?> t = i.next();
                    MetricTask mt = metricCollections.get(t);
                    if (t.isDone()) {
                        if (isDebugEnabled) {
                            log.debug("Metric task '" + mt +
                                   "' complete, duration: " +
                                   mt.getExecutionDuration());
                        }
                        i.remove();
                    } else {
                        // Not complete, check for timeout
                        if (mt.getExecutionDuration() > cancelTimeout) {
                            boolean res = t.cancel(true);
                            log.error("Metric '" + mt +
                                       "' took too long to run (" + mt.getExecutionDuration() +
                                       "ms), cancelled (result=" + res + ")");

                            // If the metric is Availability, send a down data point in
                            // case the metric cancellation fails.
                            ParsedTemplate pt = getParsedTemplate(mt.meas);
                            if (pt.metric.isAvail()) {
                                MetricValue data = new MetricValue(MeasurementConstants.AVAIL_DOWN);
                                sender.processData(mt.meas.getDsnID(), data,
                                                   mt.meas.getDerivedID(), true);
                            }
                            // Task will be removed on next iteration
                        }
                    }
                }
            }
        }
    }

    private static class ResourceSchedule {
        private final Schedule       schedule = new Schedule();
        private AppdefEntityID id;
        private long           lastUnreachble = 0;
        private final List<ScheduledMeasurement> retry = new ArrayList<ScheduledMeasurement>();
        private final IntHashMap collected = new IntHashMap();
    }

    private ResourceSchedule getSchedule(ScheduledMeasurement meas) {
        String key = meas.getEntity().getAppdefKey();
        ResourceSchedule schedule;
        synchronized (schedules) {
            schedule = schedules.get(key);
            if (schedule == null) {
                schedule = new ResourceSchedule();
                schedule.id = meas.getEntity();
                schedules.put(key, schedule);
                log.debug("Created ResourceSchedule for: " + key);
            }
        }
        
        return schedule;
    }

    // TODO: I don't think this works properly, hence the slow agent shutdowns..
    private void interruptMe(){
        synchronized (interrupter) {
            interrupter.notify();
        }
    }

    /**
     * Shut down the schedule thread.
     */
    void die(){
        shouldDie.set(true);
        for (String s : executors.keySet()) {
            ThreadPoolExecutor executor = executors.get(s);
            List<Runnable> queuedMetrics = executor.shutdownNow();
            log.info("Shut down executor service for plugin '" + s + "'" +
                      " with " + queuedMetrics.size() + " queued collections");
        }

        metricLoggingTask.cancel(true);
        metricVerificationTask.cancel(true);
        List<Runnable> pending = metricVerificationService.shutdownNow();
        log.info("Shutdown metric verification task with " +
                  pending.size() + " tasks");
        
        interruptMe();
    }

    /**
     * Un-schedule a previously scheduled, repeatable measurement.
     *
     * @param ent Entity to un-schedule metrics for
     *
     * @throws UnscheduledItemException indicating the passed ID was not found
     */
    void unscheduleMeasurements(AppdefEntityID ent)
        throws UnscheduledItemException
    {
        String key = ent.getAppdefKey();
        ScheduledItem[] items;

        ResourceSchedule rs;
        synchronized (schedules) {
            rs = schedules.remove(key);
        }

        if (rs == null) {
            if (log.isDebugEnabled()) {
                log.debug("No measurement schedule for: " + key);
            }
            return;
        }
        setDiagScheduled(rs, false);

        items = rs.schedule.getScheduledItems();
        log.debug("Un-scheduling " + items.length + " metrics for " + ent);

        synchronized (statsLock) {
            statNumMetricsScheduled -= items.length;
        }

        synchronized (scheduled) {
            for (ScheduledItem item : items) {
                ScheduledMeasurement meas = (ScheduledMeasurement) item.getObj();
                scheduled.remove(meas.getDerivedID());
                //For plugin/Collector awareness
                ParsedTemplate tmpl = getParsedTemplate(meas);
                if ((tmpl == null) || (tmpl.metric == null)) {
                    continue;
                }
                tmpl.metric.setInterval(-1);
            }
        }
    }

    /**
     * Schedule a measurement to be taken at a given interval.  
     *
     * @param meas Measurement to schedule
     */
    void scheduleMeasurement(ScheduledMeasurement meas){
        int mid = meas.getDerivedID();
        synchronized (scheduled) {
            if (scheduled.contains(mid)) {
                return;
            }
            scheduled.add(mid);
        }
        ResourceSchedule rs = getSchedule(meas);
        setDiagScheduled(rs, true);
        try {
            rs.schedule.scheduleItem(meas, meas.getInterval(), true, true);
            if (log.isDebugEnabled()) {
                Long timeOfNext;
                try {
                    timeOfNext = rs.schedule.getTimeOfNext();
                } catch (EmptyScheduleException e) {
                    timeOfNext = null;
                }
                log.debug("scheduleMeasurement timeOfNext=" + TimeUtil.toString(timeOfNext) +
                          ", template=" + getParsedTemplate(meas).metric.toDebugString());
            }
            synchronized (statsLock) {
                statNumMetricsScheduled++;
            }
        } catch (ScheduleException e) {
            log.error("Unable to schedule metric '" + getParsedTemplate(meas) + "', skipping. Cause is " + e, e);
            synchronized (scheduled) {
                scheduled.remove(mid);
            }
        }
    }

    private void logCache(String basicMsg, ParsedTemplate tmpl, String msg,
                          Exception exc, boolean printStack){
        String oldMsg;
        boolean isDebug = log.isDebugEnabled();

        synchronized(errors){
            oldMsg = errors.get(tmpl.metric.toString());
        }

        if(!isDebug && (oldMsg != null) && oldMsg.equals(msg)){
            return;
        }

        if(isDebug){
            log.error(basicMsg + " while processing Metric '" + tmpl + "'", exc);
        } else {
            log.error(basicMsg + ": " + msg);
            if (printStack) {
                log.error("Stack trace follows:", exc);
            }
        }

        synchronized(errors){
            errors.put(tmpl.metric.toString(), msg);
        }
    }

    /**
     * A method which does the main logging for the run() method.  It
     * ensures that we don't perform excessive logging when plugins
     * generate a lot of errors.
     *
     * @param basicMsg The basic log message
     * @param tmpl The template causing the errors
     * @param exc The Exception to be logged.
     */
    private void logCache(String basicMsg, ParsedTemplate tmpl, Exception exc){
        logCache(basicMsg, tmpl, exc.getMessage(), exc, false);
    }

    private void clearLogCache(ParsedTemplate tmpl){
        synchronized(errors){
            errors.remove(tmpl.metric.toString());
        }
    }

    static class ParsedTemplate {
        String plugin;
        Metric metric;
        
        @Override
        public String toString() {
            return plugin + ":" + metric.toDebugString();
        }
    }

    static ParsedTemplate getParsedTemplate(ScheduledMeasurement meas) {
        ParsedTemplate tmpl = new ParsedTemplate();
        String template = meas.getDSN();
        //duplicating some code from MeasurementPluginManager
        //so we can do Metric.setId
        int ix = template.indexOf(":");
        if (ix < 0) {
            return tmpl;
        }
        tmpl.plugin = template.substring(0, ix);
        String metric = template.substring(ix+1, template.length());
        tmpl.metric = Metric.parse(metric);
        return tmpl;
    }

    private ParsedTemplate toParsedTemplate(ScheduledMeasurement meas) {
        AppdefEntityID aid = meas.getEntity();
        if (aid == null) {
            return null;
        }
        int id = aid.getID();
        int type = aid.getType();
        ParsedTemplate tmpl = getParsedTemplate(meas);
        if ((tmpl == null) || (tmpl.metric == null)) {
            return null;
        }
        tmpl.metric.setId(type, id);
        tmpl.metric.setCategory(meas.getCategory());
        tmpl.metric.setInterval(meas.getInterval());
        return tmpl;
    }

    private class MetricTask implements Runnable {
        ResourceSchedule rs;
        ScheduledMeasurement meas;
        long executeStartTime = 0;
        long executeEndTime = 0;

        MetricTask(ResourceSchedule rs, ScheduledMeasurement meas) {
            this.rs = rs;
            this.meas = meas;
        }

        /**
         * @return The string representing this metric.
         */
        @Override
        public String toString() {
            return getParsedTemplate(meas).metric.toDebugString();
        }

        /**
         * @return 0 if task is still queued for execution, otherwise the
         * amount of time in milliseconds the task has been running.
         */
        public long getExecutionDuration() {
            if (executeStartTime == 0) {
                // Still queued for execution
                return executeStartTime;
            } else if (executeEndTime == 0) {
                // Currently executing
                return System.currentTimeMillis() - executeStartTime;
            } else {
                // Completed
                return executeEndTime - executeStartTime;
            }
        }

        public void run() {
            boolean isDebug = log.isDebugEnabled();
            AppdefEntityID aid = meas.getEntity();
            String category = meas.getCategory();
            ParsedTemplate dsn = toParsedTemplate(meas);
            MetricValue data = null;
            executeStartTime = System.currentTimeMillis();
            boolean success = false;

            if (rs.lastUnreachble != 0) {
                if (!category.equals(MeasurementConstants.CAT_AVAILABILITY)) {
                    // Prevent stacktrace bombs if a resource is
                    // down, but don't skip processing availability metrics.
                    statsCollector.addStat(1, SCHEDULE_THREAD_METRIC_COLLECT_FAILED);
                    statNumMetricsFailed++;
                    return;
                }
            }

            // XXX -- We should do something with the exceptions here.
            //        Maybe send some kind of error back to the
            //        bizapp?
            try {
                int mid = meas.getDsnID();
                if (rs.collected.get(mid) == Boolean.TRUE) {
                    if (isDebug) {
                        log.debug("Skipping duplicate mid=" + mid + ", aid=" + rs.id);
                    }
                    return;
                }
                long lastCollected = meas.getLastCollected();
                long now = now();
                long nowRounded = TimingVoodoo.roundDownTime(now, 60000);
                // HHQ-5483 - agent is collecting metrics too frequently.  I don't want to mess with the scheduling
                // algorithm at this time, so I am simply putting checks in to prevent this scenario from occurring
                if ((lastCollected + meas.getInterval()) > nowRounded) {
                    if (isDebug) {
                        log.debug("ALREADY COLLECTED meas=" + meas + " @ " + TimeUtil.toString(lastCollected));
                    }
                    return;
                }
                if (isDebug) {
                    log.debug("collecting data for meas=" + meas);
                }
                data = manager.getValue(dsn.plugin, dsn.metric);

                if (deductServerTimeDiff) {
                    if (Math.abs(ServerTimeDiff.getInstance().getServerTimeDiff()) > ServerTimeDiff.MIN_OFFSET_FOR_DEDUCTION) {
                        // deduct the server time offset from the metric
                        // value time stamp
                        data.setTimestamp(data.getTimestamp() + ServerTimeDiff.getInstance().getServerTimeDiff());
                    }
                }
                long time = TimingVoodoo.roundDownTime(now, meas.getInterval());
                meas.setLastCollected(time);
                if (data == null) {
                    // Don't allow plugins to return null from getValue(),
                    // convert these to MetricValue.NONE
                    log.warn("Plugin returned null value for metric: " + dsn);
                    data = MetricValue.NONE;
                }
                rs.collected.put(mid, Boolean.TRUE);
                setDiagInfo(data, dsn, rs, mid);
                success = true;
                clearLogCache(dsn);
            } catch(PluginNotFoundException exc){
                logCache("Plugin not found", dsn, exc);
            } catch(PluginException exc){
                logCache("Measurement plugin error", dsn, exc);
            } catch(MetricInvalidException exc){
                logCache("Invalid Metric requested", dsn, exc);
            } catch(MetricNotFoundException exc){
                logCache("Metric Value not found", dsn, exc);
            } catch(MetricUnreachableException exc){
                logCache("Metric unreachable", dsn, exc);
                rs.lastUnreachble = executeStartTime;
                log.warn("Disabling metrics for: " + rs.id);
            } catch(Exception exc){
                // Unexpected exception
                logCache("Error getting measurement value",
                         dsn, exc.toString(), exc, true);
            }

            // Stats stuff
            Long timeDiff = System.currentTimeMillis() - executeStartTime;
            statsCollector.addStat(timeDiff, SCHEDULE_THREAD_METRICS_COLLECTED_TIME);

            synchronized (statsLock) {
                statTotFetchTime += timeDiff;
                if(timeDiff > statMaxFetchTime) {
                    statMaxFetchTime = timeDiff;
                }

                if(timeDiff < statMinFetchTime) {
                    statMinFetchTime = timeDiff;
                }
            }

            if (timeDiff > logFetchTimeout) {
                log.warn("Collection of metric: '" + dsn + "' took: " + timeDiff + "ms");
            }

            if (success) {
                if (isDebug) {
                    String debugDsn = getParsedTemplate(meas).metric.toDebugString();
                    String msg =
                        "[" + aid + ":" + category +
                        "] Metric='" + debugDsn + "' -> " + data;

                    log.debug(msg + " timestamp=" + data.getTimestamp());
                }
                if (data.isNone()) {
                    //wouldn't be inserted into the database anyhow
                    //but might as well skip sending this to the server.
                    return;
                }
                else if (data.isFuture()) {
                    //for example, first time collecting an exec: metric
                    //adding to this list will cause the metric to be
                    //collected next time the schedule has items to consume
                    //rather than waiting for the metric's own interval
                    //which could take much longer to hit
                    //(e.g. Windows Updates on an 8 hour interval)
                    rs.retry.add(meas);
                    return;
                }
                
                sender.processData(meas.getDsnID(), data, meas.getDerivedID(), MeasurementConstants.CAT_AVAILABILITY.equals(category));
                synchronized (statsLock) {
                    statNumMetricsFetched++;
                }
            } else {
                synchronized (statsLock) {
                    statNumMetricsFailed++;
                }
            }
        }

        private long now() {
            return System.currentTimeMillis();
        }
    }

    private void setDiagInfo(MetricValue data, ParsedTemplate dsn, ResourceSchedule rs, int mid) {
        final AppdefEntityID aeid = rs.id;
        synchronized (diagInfo) {
            DiagInfo tmp = diagInfo.get(aeid);
            if (tmp == null) {
                tmp = new DiagInfo(rs.id);
                diagInfo.put(aeid, tmp);
            }
            tmp.add(new MetricValuePlusId(mid, data));
        }
    }

    private void setDiagScheduled(ResourceSchedule rs, boolean incrementSchedules) {
        synchronized(diagInfo) {
            DiagInfo tmp = diagInfo.get(rs.id);
            if (tmp == null) {
                tmp = new DiagInfo(rs.id);
                diagInfo.put(rs.id, tmp);
            }
            if (incrementSchedules) {
                tmp.incrementSchedules();
            } else {
                tmp.incrementUnSchedules();
            }
        }
    }

    private int getQueueSize(String plugin) {
        String prop = PROP_QUEUE_SIZE + plugin;
        String sQueueSize = agentConfig.getProperty(prop);
        if(sQueueSize != null){
            try {
                return Integer.parseInt(sQueueSize);
            } catch(NumberFormatException exc){
                log.error("Invalid setting for " + prop + " value=" +
                           sQueueSize + " using defaults.");
            }
        }
        return EXECUTOR_QUEUE_SIZE;
    }

    private int getPoolSize(String plugin) {
        String prop = PROP_POOLSIZE + plugin;
        String sPoolSize = agentConfig.getProperty(prop);
        if(sPoolSize != null){
            try {
                return Integer.parseInt(sPoolSize);
            } catch(NumberFormatException exc){
                log.error("Invalid setting for " + prop + " value=" +
                           sPoolSize + " using defaults.");
            }
        }
        return 1;
    }

    private void collect(ResourceSchedule rs, List<ScheduledMeasurement> items) {
        final boolean debug = log.isDebugEnabled();
        for (int i=0; (i<items.size()) && (!shouldDie.get()); i++) {
            ScheduledMeasurement meas = items.get(i);
            ParsedTemplate tmpl = toParsedTemplate(meas);
            if (tmpl == null) {
                log.warn("template for meas id=" + meas.getDerivedID() + " is null");
                continue;
            }
            ThreadPoolExecutor executor;
            String plugin;
            synchronized (executors) {
                try {
                    GenericPlugin p = manager.getPlugin(tmpl.plugin).getProductPlugin();
                    plugin = p.getName();
                } catch (PluginNotFoundException e) {
                    if (debug) {
                        log.debug("Could not find plugin name from template '" + tmpl.plugin +
                                  "'. Associated plugin might not be initialized yet.");
                    }
                    continue;
                }
                executor = executors.get(plugin);
                if (executor == null) {
                    final int poolSize = getPoolSize(plugin);
                    final int queueSize = getQueueSize(plugin);
                    log.info("Creating executor for plugin '" + plugin +
                              "' with a poolsize=" + poolSize + " queuesize=" + queueSize);
                    final ThreadFactory factory = getFactory(plugin);
                    executor = new ThreadPoolExecutor(poolSize, poolSize,
                                                 60, TimeUnit.SECONDS,
                                                 new LinkedBlockingQueue<Runnable>(queueSize), factory,
                                                 new ThreadPoolExecutor.AbortPolicy());
                    executors.put(plugin, executor);
                }
            }
            MetricTask metricTask = new MetricTask(rs, meas);
            statsCollector.addStat(1, SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED);
            try {
                Future<?> task = executor.submit(metricTask);
                synchronized (metricCollections) {
                    metricCollections.put(task,metricTask);
                }
            } catch (RejectedExecutionException e) {
                log.warn("Executor[" + plugin + "] rejected metric task " + metricTask);
                statNumMetricsFailed++;
            }
        }
    }

    private ThreadFactory getFactory(final String plugin) {
        final SecurityManager s = System.getSecurityManager();
        final ThreadGroup group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        return new ThreadFactory() {
            private final AtomicLong num = new AtomicLong();
            public Thread newThread(Runnable r) {
                final Thread rtn = new Thread(group, r);
                rtn.setDaemon(true);
                rtn.setName(plugin + "-" + num.getAndIncrement());
                return rtn;
            }
        };
    }

    private long collect(ResourceSchedule rs) {
        long timeOfNext;
        long now = System.currentTimeMillis();
        Schedule schedule = rs.schedule;
        try {
            timeOfNext = schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            return POLL_PERIOD + now;
        }

        if (rs.lastUnreachble != 0) {
            if ((now - rs.lastUnreachble) > UNREACHABLE_EXPIRE) {
                rs.lastUnreachble = 0;
                log.info("Re-enabling metrics for: " + rs.id);
            }
        }

        rs.collected.clear();

        if (rs.retry.size() != 0) {
            if (log.isDebugEnabled()) {
                log.debug("Retrying " + rs.retry.size() + " items (MetricValue.FUTUREs)");
            }
            collect(rs, rs.retry);
            rs.retry.clear();
        }

        if (now < timeOfNext) {
            return timeOfNext;
        }

        List items;

        try {
            items = schedule.consumeNextItems();
            timeOfNext = schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            return POLL_PERIOD + now;
        }
        collect(rs, items);
        return timeOfNext;
    }
    
    private long collect() {
        long timeOfNext = 0;
        
        Map<String,ResourceSchedule> schedules = null;
        synchronized (this.schedules) {
            if (this.schedules.size() == 0) {
                //nothing scheduled
                timeOfNext = POLL_PERIOD + System.currentTimeMillis();
            } else {
                schedules = new HashMap<String,ResourceSchedule>(this.schedules);
            }
        }

        if (schedules != null) {
            for (Iterator<ResourceSchedule> it = schedules.values().iterator(); it.hasNext() && (!shouldDie.get());) {
                ResourceSchedule rs = it.next();
                try {
                    long next = collect(rs);
                    if (timeOfNext == 0) {
                        timeOfNext = next;
                    } else {
                        timeOfNext = Math.min(next, timeOfNext);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        
        return timeOfNext;
    }

    /**
     * The main loop of the ScheduleThread, which watches the schedule
     * waits the appropriate time, and executes scheduled operations.
     */
    public void run(){
        final boolean isDebug = log.isDebugEnabled();
        final int fudgeFactor = getFudgeFactor();
        while (!shouldDie.get()) {
            long timeOfNext = collect();
            if (fudgeFactor > 0) {
                timeOfNext += rand.nextInt(fudgeFactor);
            }
            long now = System.currentTimeMillis();
            if (timeOfNext > now) {
                long wait = timeOfNext - now;
                if (isDebug) {
                    log.debug("Waiting " + wait + " ms until " +
                               TimeUtil.toString(now+wait));
                }
                try {
                    synchronized (interrupter) {
                        interrupter.wait(wait);
                    }
                } catch (InterruptedException e) {
                    log.debug("Schedule thread kicked");
                }
            }
        }
        log.info("Schedule thread shut down");
    }

    /**
     * HQ-3904 Fudge factor is only for scale environments.  DO NOT USE IN PRODUCTION!
     */
    private int getFudgeFactor() {
        final String val = agentConfig.getProperty("agent.dsl.fudge", "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.debug("val=" + val + " is not a valid number.  Fudge factor will be 0 seconds");
            return 0;
        }
    }

    // MONITOR METHODS

    /**
     * @return Get the number of metrics in the schedule
     */
    public double getNumMetricsScheduled() throws AgentMonitorException {
        synchronized (statsLock) {
            return statNumMetricsScheduled;
        }
    }

    /**
     * @return The number of metrics which were attempted to be fetched (failed or successful)
     */
    public double getNumMetricsFetched() throws AgentMonitorException {
        synchronized (statsLock) {
            return statNumMetricsFetched;
        }
    }

    /**
     * @return Get the number of metrics which resulted in an error when collected
     */
    public double getNumMetricsFailed() throws AgentMonitorException {
        synchronized (statsLock) {
            return statNumMetricsFailed;
        }
    }

    /**
     * @return The total time spent fetching metrics
     */
    public double getTotFetchTime() throws AgentMonitorException {
        synchronized (statsLock) {
            return statTotFetchTime;
        }
    }

    /**
     * @return The maximum time spent fetching a metric
     */
    public double getMaxFetchTime() throws AgentMonitorException {
        synchronized (statsLock) {
            if(statMaxFetchTime == Long.MIN_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return statMaxFetchTime;
        }
    }

    /**
     * @return The minimum time spent fetching a metric
     */
    public double getMinFetchTime() throws AgentMonitorException {
        synchronized (statsLock) {
            if(statMinFetchTime == Long.MAX_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return statMinFetchTime;
        }
    }
    
    public String getDiagStatus() {
        StringBuilder rtn = new StringBuilder();
        synchronized (diagInfo) {
            for (Entry<AppdefEntityID, DiagInfo> entry : diagInfo.entrySet()) {
                AppdefEntityID aeid = entry.getKey();
                DiagInfo d = entry.getValue();
                rtn.append(aeid).append(":").append(d).append("\n");
                d.clear();
            }
        }
        return rtn.toString();
    }
    
    private static final SimpleDateFormat diagInfoTimeFormat = new SimpleDateFormat("HH:mm");
    private class DiagInfo {
        @SuppressWarnings("unused")
        private final AppdefEntityID aeid;
        private final List<MetricValuePlusId> collected = new ArrayList<MetricValuePlusId>();
        private int numSchedules = 0;
        private int numUnSchedules = 0;
        private DiagInfo(AppdefEntityID aeid) {
            this.aeid = aeid;
        }
        private void add(MetricValuePlusId data) {
            collected.add(data);
        }
        private void clear() {
            collected.clear();
            numUnSchedules = 0;
            numSchedules = 0;
        }
        private void incrementUnSchedules() {
            numUnSchedules++;
        }
        private void incrementSchedules() {
            numSchedules++;
        }
        @Override
        public String toString() {
            final StringBuilder rtn = new StringBuilder();
            rtn.append("(").append(numSchedules).append("-").append(numUnSchedules).append(")");
            for (MetricValuePlusId m : collected) {
                rtn.append("(").append(m.mid).append("-").append(diagInfoTimeFormat.format(new Date(m.getTimestamp())))
                   .append("=").append(m.getValue()).append(")");
            }
            return rtn.toString();
        }
    }
    
    private class MetricValuePlusId {
        private final int mid;
        private final MetricValue value;
        private MetricValuePlusId(int mid, MetricValue value) {
            this.mid = mid;
            this.value = value;
        }
        public double getValue() {
            return value.getValue();
        }
        public long getTimestamp() {
            return value.getTimestamp();
        }
    }

    public String getDiagName() {
        return "Schedule Thread Diagnostics";
    }
}
