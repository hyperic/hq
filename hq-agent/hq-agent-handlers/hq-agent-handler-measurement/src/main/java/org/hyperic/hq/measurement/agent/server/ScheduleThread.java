/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementConstants;
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

public class ScheduleThread 
    extends AgentMonitorSimple
    implements Runnable 
{
    // Agent properties configuration
    static final String PROP_POOLSIZE =
            "scheduleThread.poolsize."; // e.g. scheduleThread.poolsize.sigar=10
    static final String PROP_FETCH_LOG_TIMEOUT =
            "scheduleThread.fetchLogTimeout";
    static final String PROP_CANCEL_TIMEOUT =
            "scheduleThread.cancelTimeout";

    // How often we check schedules when we think they are empty.
    private static final int POLL_PERIOD = 1000;
    private static final int UNREACHABLE_EXPIRE = (60 * 1000) * 5;

    private static final long FETCH_TIME  = 2000; // 2 seconds.
    private static final long CANCEL_TIME = 5000; // 5 seconds.

    private static long _logFetchTimeout = FETCH_TIME;
    private static long _cancelTimeout   = CANCEL_TIME;

    private static final Log _log = LogFactory.getLog(ScheduleThread.class.getName());

    private final Map<String,ResourceSchedule>    _schedules;   // AppdefID -> Schedule
    private volatile boolean                      _shouldDie;   // Should I shut down?
    private final Object                          _interrupter; // Interrupt object
    private final HashMap<String,String>          _errors;      // Hash of DSNs to their errors

    private final Properties _agentConfig; // agent.properties

    // Map of Executors, one per metric domain
    private final HashMap<String,ExecutorService> _executors;
    // Map of asynchronous MetricTasks pending confirmation
    private final HashMap<FutureTask,MetricTask>   _metricCollections;
    // The Thread confirming metric collections, cancelling tasks that exceed
    // our timeouts.
    private MetricCancelThread _metricCancelThread;

    private MeasurementValueGetter   _manager;
    private Sender                   _sender;  // Guy handling the results

    // Statistics
    private final Object _statsLock = new Object();
    private long _stat_numMetricsFetched = 0;
    private long _stat_numMetricsFailed  = 0;
    private long _stat_totFetchTime      = 0;
    private long _stat_numMetricsScheduled = 0;
    private long _stat_maxFetchTime      = Long.MIN_VALUE;
    private long _stat_minFetchTime      = Long.MAX_VALUE;

    ScheduleThread(Sender sender, MeasurementValueGetter manager,
                   Properties config)
        throws AgentStartException
    {
        _agentConfig  = config;
        _schedules    = new HashMap<String,ResourceSchedule>();
        _shouldDie    = false;
        _interrupter  = new Object();
        _manager      = manager;
        _sender       = sender;
        _errors       = new HashMap<String,String>();
        _executors    = new HashMap<String,ExecutorService>();
        _metricCollections = new HashMap<FutureTask,MetricTask>();

        String sLogFetchTimeout = _agentConfig.getProperty(PROP_FETCH_LOG_TIMEOUT);
        if(sLogFetchTimeout != null){
            try {
                _logFetchTimeout = Integer.parseInt(sLogFetchTimeout);
            } catch(NumberFormatException exc){
                _log.error("Invalid setting for " + PROP_FETCH_LOG_TIMEOUT + " value=" +
                           sLogFetchTimeout + ", using defaults.");
            }
        }

        String sCancelTimeout = _agentConfig.getProperty(PROP_CANCEL_TIMEOUT);
        if(sCancelTimeout != null){
            try {
                _cancelTimeout = Integer.parseInt(sCancelTimeout);
            } catch(NumberFormatException exc){
                _log.error("Invalid setting for " + PROP_CANCEL_TIMEOUT + " value=" +
                           sCancelTimeout + ", using defaults.");
            }
        }

        _metricCancelThread = new MetricCancelThread();
        _metricCancelThread.start();
    }

    /**
     * The MetricCancelThread iterates over the list of FutureTasks that
     * have been submitted for execution.  Each task is checked for completion
     * and then removed.  For tasks that do not complete within the timeout
     * are cancelled, which will attempt to free up the executor running the
     * task.
     * NOTE: This will only work if the hung task is in an interrupt-able state
     *       i.e. sleep() or wait()
     */
    private class MetricCancelThread extends Thread  {
        public void run() {
            while (!_shouldDie) {
                synchronized (_metricCollections) {
                    if (_metricCollections.size() > 0) {
                        _log.debug(_metricCollections.size() + " metrics to validate.");
                    }
                    for (Iterator<FutureTask> i = _metricCollections.keySet().iterator();
                         i.hasNext(); )
                    {
                        FutureTask t = i.next();
                        MetricTask mt = _metricCollections.get(t);
                        if (t.isDone()) {
                            _log.debug("Metric task '" + mt.getMetric() +
                                       "' complete, duration: " +
                                       mt.getExecutionDuration());
                            i.remove();
                        } else {
                            // Not complete, check for timeout
                            if (mt.getExecutionDuration() > _cancelTimeout) {
                                _log.error("Metric '" + mt.getMetric() +
                                           "' took too long to run, attempting to cancel");
                                boolean res = t.cancel(true);
                                _log.error("Cancel result=" + res);
                                // Task will be removed on next iteration
                            }
                        }
                    }
                }

                try {
                    Thread.sleep(POLL_PERIOD);
                } catch (InterruptedException e) {
                    _log.info("MetricCancelThread interrupted!");
                }
            }
            _log.info("MetricCancelThread shutting down with " + _metricCollections.size() +
                      " unprocessed MetricTasks");
        }
    }

    private static class ResourceSchedule {
        private Schedule       schedule = new Schedule();
        private AppdefEntityID id;
        private long           lastUnreachble = 0;
        private List<ScheduledMeasurement> retry = new ArrayList<ScheduledMeasurement>();
        private IntHashMap collected = new IntHashMap();
    }

    private ResourceSchedule getSchedule(ScheduledMeasurement meas) {
        String key = meas.getEntity().getAppdefKey();
        ResourceSchedule schedule;
        synchronized (_schedules) {
            schedule = _schedules.get(key);
            if (schedule == null) {
                schedule = new ResourceSchedule();
                schedule.id = meas.getEntity();
                _schedules.put(key, schedule);
                _log.debug("Created ResourceSchedule for: " + key);
            }
        }
        
        return schedule;
    }

    // TODO: I don't think this works properly, hence the slow agent shutdowns..
    private void interruptMe(){
        synchronized (_interrupter) {
            _interrupter.notify();
        }
    }

    /**
     * Shut down the schedule thread.
     */
    void die(){
        _shouldDie = true;
        for (String s : _executors.keySet()) {
            ExecutorService svc = _executors.get(s);
            List<Runnable> queuedMetrics = svc.shutdownNow();
            _log.info("Shut down executor service for plugin '" + s + "'" +
                      " with " + queuedMetrics.size() + " queued collections");
        }

        try {
            _metricCancelThread.interrupt();
            _metricCancelThread.join();
        } catch (InterruptedException e) {
            // Ignore
        }

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
        synchronized (_schedules) {
            rs = _schedules.remove(key);
        }

        if (rs == null) {
            throw new UnscheduledItemException("No measurement schedule for: " + key);
        }

        items = rs.schedule.getScheduledItems();
        _log.debug("Un-scheduling " + items.length + " metrics for " + ent);

        synchronized (_statsLock) {
            _stat_numMetricsScheduled -= items.length;            
        }

        for (ScheduledItem item : items) {
            ScheduledMeasurement meas = (ScheduledMeasurement) item.getObj();
            //For plugin/Collector awareness
            ParsedTemplate tmpl = getParsedTemplate(meas);
            tmpl.metric.setInterval(-1);
        }
    }

    /**
     * Schedule a measurement to be taken at a given interval.  
     *
     * @param meas Measurement to schedule
     */

    void scheduleMeasurement(ScheduledMeasurement meas){
        ResourceSchedule rs = getSchedule(meas);
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("scheduleMeasurement " + getParsedTemplate(meas).metric.toDebugString());
            }

            rs.schedule.scheduleItem(meas, meas.getInterval(), true, true);

            synchronized (_statsLock) {
                _stat_numMetricsScheduled++;
            }
        } catch (ScheduleException e) {
            _log.error("Unable to schedule metric '" +
                      getParsedTemplate(meas) + "', skipping. Cause is " +
                      e.getMessage(), e);
        }
    }

    private void logCache(String basicMsg, ParsedTemplate tmpl, String msg,
                          Exception exc, boolean printStack){
        String oldMsg;
        boolean isDebug = _log.isDebugEnabled();

        synchronized(_errors){
            oldMsg = _errors.get(tmpl.metric.toString());
        }

        if(!isDebug && oldMsg != null && oldMsg.equals(msg)){
            return;
        }

        if(isDebug){
            _log.error(basicMsg + " while processing Metric '" + tmpl + "'", exc);
        } else {
            _log.error(basicMsg + ": " + msg);
            if (printStack) {
                _log.error("Stack trace follows:", exc);
            }
        }

        synchronized(_errors){
            _errors.put(tmpl.metric.toString(), msg);
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
        synchronized(_errors){
            _errors.remove(tmpl.metric.toString());
        }
    }

    static class ParsedTemplate {
        String plugin;
        Metric metric;
        
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
        tmpl.plugin = template.substring(0, ix);
        String metric = template.substring(ix+1, template.length());
        tmpl.metric = Metric.parse(metric);
        return tmpl;
    }

    private ParsedTemplate toParsedTemplate(ScheduledMeasurement meas) {
        AppdefEntityID aid = meas.getEntity();
        int id = aid.getID();
        int type = aid.getType();
        ParsedTemplate tmpl = getParsedTemplate(meas);
        tmpl.metric.setId(type, id);
        tmpl.metric.setCategory(meas.getCategory());
        tmpl.metric.setInterval(meas.getInterval());
        return tmpl;
    }

    private MetricValue getValue(ParsedTemplate tmpl)
        throws PluginException, MetricNotFoundException,
               MetricUnreachableException
    {
        return _manager.getValue(tmpl.plugin, tmpl.metric);
    }

    private class MetricTask implements Runnable {
        ResourceSchedule _rs;
        ScheduledMeasurement _meas;
        long _executeStartTime = 0;
        long _executeEndTime = 0;

        MetricTask(ResourceSchedule rs, ScheduledMeasurement meas) {
            _rs = rs;
            _meas = meas;
        }

        /**
         * @return The metric this task is attempting to collect.
         */
        public String getMetric() {
            return _meas.getDSN();
        }

        /**
         * @return 0 if task is still queued for execution, otherwise the
         * amount of time in milliseconds the task has been running.
         */
        public long getExecutionDuration() {
            if (_executeStartTime == 0) {
                // Still queued for execution
                return _executeStartTime;
            } else if (_executeEndTime == 0) {
                // Currently executing
                return System.currentTimeMillis() - _executeStartTime;
            } else {
                // Completed
                return _executeEndTime - _executeStartTime;
            }
        }

        public void run() {
            boolean isDebug = _log.isDebugEnabled();
            AppdefEntityID aid = _meas.getEntity();
            String category = _meas.getCategory();
            ParsedTemplate dsn = toParsedTemplate(_meas);
            MetricValue data = null;
            _executeStartTime = System.currentTimeMillis();
            boolean success = false;

            if (_rs.lastUnreachble != 0) {
                if (!category.equals(MeasurementConstants.CAT_AVAILABILITY)) {
                    // Prevent stacktrace bombs if a resource is
                    // down, but don't skip processing availability metrics.
                    _stat_numMetricsFailed++;
                    return;
                }
            }

            // XXX -- We should do something with the exceptions here.
            //        Maybe send some kind of error back to the
            //        bizapp?
            try {
                int mid = _meas.getDsnID();
                if (_rs.collected.get(mid) == Boolean.TRUE) {
                    if (isDebug) {
                        _log.debug("Skipping duplicate mid=" + mid + ", aid=" + _rs.id);
                    }
                }
                data = getValue(dsn);
                if (data == null) {
                    // Don't allow plugins to return null from getValue(),
                    // convert these to MetricValue.NONE
                    _log.warn("Plugin returned null value for metric: " + dsn);
                    data = MetricValue.NONE;
                }
                _rs.collected.put(mid, Boolean.TRUE);
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
                _rs.lastUnreachble = _executeStartTime;
                _log.warn("Disabling metrics for: " + _rs.id);
            } catch(Exception exc){
                // Unexpected exception
                logCache("Error getting measurement value",
                         dsn, exc.toString(), exc, true);
            }

            // Stats stuff
            Long timeDiff = System.currentTimeMillis() - _executeStartTime;

            synchronized (_statsLock) {
                _stat_totFetchTime += timeDiff;
                if(timeDiff > _stat_maxFetchTime) {
                    _stat_maxFetchTime = timeDiff;
                }

                if(timeDiff < _stat_minFetchTime) {
                    _stat_minFetchTime = timeDiff;
                }
            }

            if (timeDiff > _logFetchTimeout) {
                _log.warn("Collection of metric: '" + dsn + "' took: " + timeDiff + "ms");
            }

            if (success) {
                if (isDebug) {
                    String debugDsn = getParsedTemplate(_meas).metric.toDebugString();
                    String msg =
                        "[" + aid + ":" + category +
                        "] Metric='" + debugDsn + "' -> " + data;

                    _log.debug(msg + " timestamp=" + data.getTimestamp());
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
                    _rs.retry.add(_meas);
                    return;
                }
                _sender.processData(_meas.getDsnID(), data,
                                    _meas.getDerivedID());
                synchronized (_statsLock) {
                    _stat_numMetricsFetched++;
                }
            } else {
                synchronized (_statsLock) {
                    _stat_numMetricsFailed++;
                }
            }
        }
    }

    private int getPoolSize(String domain) {
        String prop = PROP_POOLSIZE + domain;
        String sPoolSize = _agentConfig.getProperty(prop);
        if(sPoolSize != null){
            try {
                return Integer.parseInt(sPoolSize);
            } catch(NumberFormatException exc){
                _log.error("Invalid setting for " + prop + " value=" +
                           sPoolSize + " using defaults.");
            }
        }
        return 1;
    }

    private void collect(ResourceSchedule rs, List items)
    {
        for (int i=0; i<items.size() && (!_shouldDie); i++) {
            ScheduledMeasurement meas =
                (ScheduledMeasurement)items.get(i);
            ParsedTemplate tmpl = toParsedTemplate(meas);

            ExecutorService svc;
            synchronized (_executors) {
                String plugin;
                try {
                    GenericPlugin p = _manager.getPlugin(tmpl.plugin).getProductPlugin();
                    plugin = p.getName();
                } catch (PluginNotFoundException e) {
                    // Proxied plugin?
                    plugin = tmpl.plugin;
                }

                svc = _executors.get(plugin);
                if (svc == null) {
                    int poolSize = getPoolSize(plugin);
                    _log.info("Creating executor for plugin '" + plugin +
                              "' with a pool size of " + poolSize);
                    svc = Executors.newFixedThreadPool(poolSize);
                    _executors.put(plugin, svc);
                }
            }

            MetricTask metricTask = new MetricTask(rs, meas);
            FutureTask<?> task = new FutureTask<Object>(metricTask, null);
            svc.submit(task);
            synchronized (_metricCollections) {
                _metricCollections.put(task,metricTask);
            }
        }
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
                _log.info("Re-enabling metrics for: " + rs.id);
            }
        }

        rs.collected.clear();

        if (rs.retry.size() != 0) {
            if (_log.isDebugEnabled()) {
                _log.debug("Retrying " + rs.retry.size() + " items (MetricValue.FUTUREs)");
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
        synchronized (_schedules) {
            if (_schedules.size() == 0) {
                //nothing scheduled
                timeOfNext = POLL_PERIOD + System.currentTimeMillis();
            } else {
                schedules = new HashMap<String,ResourceSchedule>(_schedules);
            }
        }

        if (schedules != null) {
            for (Iterator<ResourceSchedule> it = schedules.values().iterator();
            it.hasNext() && (!_shouldDie);) {

                ResourceSchedule rs = it.next();
                try {
                    long next = collect(rs);
                    if (timeOfNext == 0) {
                        timeOfNext = next;
                    } else {
                        timeOfNext = Math.min(next, timeOfNext);
                    }
                } catch (Throwable e) {
                    _log.error(e.getMessage(), e);
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
        boolean isDebug = _log.isDebugEnabled();
        while (!_shouldDie) {
            long timeOfNext = collect();
            long now = System.currentTimeMillis();
            if (timeOfNext > now) {
                long wait = timeOfNext - now;
                if (isDebug) {
                    _log.debug("Waiting " + wait + " ms until " +
                               TimeUtil.toString(now+wait));
                }
                try {
                    synchronized (_interrupter) {
                        _interrupter.wait(wait);
                    }
                } catch (InterruptedException e) {
                    _log.debug("Schedule thread kicked");
                }
            }
        }
        _log.info("Schedule thread shut down");
    }

    // MONITOR METHODS

    /**
     * @return Get the number of metrics in the schedule
     */
    public double getNumMetricsScheduled() throws AgentMonitorException {
        synchronized (_statsLock) {
            return _stat_numMetricsScheduled;            
        }
    }

    /**
     * @return The number of metrics which were attempted to be fetched (failed or successful)
     */
    public double getNumMetricsFetched() throws AgentMonitorException {
        synchronized (_statsLock) {
            return _stat_numMetricsFetched;
        }
    }

    /**
     * @return Get the number of metrics which resulted in an error when collected
     */
    public double getNumMetricsFailed() throws AgentMonitorException {
        synchronized (_statsLock) {
            return _stat_numMetricsFailed;
        }
    }

    /**
     * @return The total time spent fetching metrics
     */
    public double getTotFetchTime() throws AgentMonitorException {
        synchronized (_statsLock) {
            return _stat_totFetchTime;
        }
    }

    /**
     * @return The maximum time spent fetching a metric
     */
    public double getMaxFetchTime() throws AgentMonitorException {
        synchronized (_statsLock) {
            if(_stat_maxFetchTime == Long.MIN_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return _stat_maxFetchTime;
        }
    }

    /**
     * @return The minimum time spent fetching a metric
     */
    public double getMinFetchTime() throws AgentMonitorException {
        synchronized (_statsLock) {
            if(_stat_minFetchTime == Long.MAX_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return _stat_minFetchTime;
        }
    }
}
