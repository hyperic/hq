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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
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
    // How often we check schedules when we think they are empty.
    private static final int POLL_PERIOD = 1000;
    private static final int UNREACHABLE_EXPIRE = (60 * 1000) * 5;
    private static final long WARN_FETCH_TIME = 5 * 1000; // 5 seconds.

    private static final Log _log = LogFactory.getLog(ScheduleThread.class.getName());

    private final Map          _schedules;   // AppdefID -> Schedule
    private volatile boolean   _shouldDie;   // Should I shut down?
    private final Object       _interrupter; // Interrupt object
    private final HashMap      _errors;      // Hash of DSNs to their errors

    private final HashMap _executors;     // Domain -> ExecutorService

    private MeasurementValueGetter   _manager;
    private Sender                   _sender;  // Guy handling the results

    private final Object _lock = new Object();
    private long _stat_numMetricsFetched = 0;
    private long _stat_numMetricsFailed  = 0;
    private long _stat_totFetchTime      = 0;
    private long _stat_numMetricsScheduled = 0;
    private long _stat_maxFetchTime      = Long.MIN_VALUE;
    private long _stat_minFetchTime      = Long.MAX_VALUE;

    private static class ResourceSchedule {
        private Schedule _schedule = new Schedule();
        private AppdefEntityID _id;
        private long _lastUnreachble = 0;
        private List _retry = new ArrayList();
        private IntHashMap _collected = new IntHashMap();
    }

    ScheduleThread(Sender sender, MeasurementValueGetter manager)
        throws AgentStartException 
    {
        _schedules    = new HashMap();
        _shouldDie    = false;
        _interrupter  = new Object();
        _manager      = manager;
        _sender       = sender;               
        _errors       = new HashMap();
        _executors    = new HashMap();
    }

    private ResourceSchedule getSchedule(ScheduledMeasurement meas) {
        String key = meas.getEntity().getAppdefKey();
        ResourceSchedule schedule;
        synchronized (_schedules) {
            schedule = (ResourceSchedule)_schedules.get(key);
            if (schedule == null) {
                schedule = new ResourceSchedule();
                schedule._id = meas.getEntity();
                _schedules.put(key, schedule);
                _log.debug("Created ResourceSchedule for: " + key);
            }
        }
        
        return schedule;
    }

    private void interruptMe(){
        synchronized (_interrupter) {
            _interrupter.notify();
        }
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
            rs = (ResourceSchedule)_schedules.remove(key);
        }

        if (rs == null) {
            throw new UnscheduledItemException("No measurement schedule for: " + key);
        }

        items = rs._schedule.getScheduledItems();
        _log.debug("Un-scheduling " + items.length + " metrics for " + ent);

        synchronized (_lock) {
            _stat_numMetricsScheduled -= items.length;            
        }

        for (int i=0; i<items.length; i++) {
            ScheduledMeasurement meas = (ScheduledMeasurement)items[i].getObj();
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

            rs._schedule.scheduleItem(meas, meas.getInterval(), true, true);

            synchronized (_lock) {
                _stat_numMetricsScheduled++;
            }
        } catch (ScheduleException e) {
            _log.error("Unable to schedule metric '" +
                      getParsedTemplate(meas) + "', skipping. Cause is " +
                      e.getMessage(), e);
        }
    }

    /**
     * Shut down the schedule thread.  
     */
    void die(){
        _shouldDie = true;
        // TODO: Shutdown executors
        interruptMe();
    }

    private void logCache(String basicMsg, ParsedTemplate tmpl, String msg,
                          Exception exc, boolean printStack){
        String oldMsg;
        boolean isDebug = _log.isDebugEnabled();

        synchronized(_errors){
            oldMsg = (String)_errors.get(tmpl.metric.toString());
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

    private class MetricRunner implements Runnable {
        ResourceSchedule _rs;
        ScheduledMeasurement _meas;

        MetricRunner(ResourceSchedule rs, ScheduledMeasurement meas) {
            _rs = rs;
            _meas = meas;
        }

        public void run() {
            boolean isDebug = _log.isDebugEnabled();
            AppdefEntityID aid = _meas.getEntity();
            String category = _meas.getCategory();
            ParsedTemplate dsn = toParsedTemplate(_meas);
            MetricValue data = null;
            long currTime, timeDiff;
            boolean success = false;

            if (_rs._lastUnreachble != 0) {
                if (!category.equals(MeasurementConstants.CAT_AVAILABILITY)) {
                    // Prevent stacktrace bombs if a resource is
                    // down, but don't skip processing availability metrics.
                    _stat_numMetricsFailed++;
                }
            }

            currTime = System.currentTimeMillis();
            // XXX -- We should do something with the exceptions here.
            //        Maybe send some kind of error back to the
            //        bizapp?
            try {
                int mid = _meas.getDsnID();
                if (_rs._collected.get(mid) == Boolean.TRUE) {
                    if (isDebug) {
                        _log.debug("Skipping duplicate mid=" + mid +
                                   ", aid=" + _rs._id);
                    }
                }
                data = getValue(dsn);
                if (data == null) {
                    // Don't allow plugins to return null from getValue(),
                    // convert these to MetricValue.NONE
                    _log.warn("Plugin returned null value for metric: " + dsn);
                    data = MetricValue.NONE;
                }
                _rs._collected.put(mid, Boolean.TRUE);
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
                _rs._lastUnreachble = currTime;
                _log.warn("Disabling metrics for: " + _rs._id);
            } catch(Exception exc){
                // Unexpected exception
                logCache("Error getting measurement value",
                         dsn, exc.toString(), exc, true);
            }

            // Stats stuff
            timeDiff = System.currentTimeMillis() - currTime;

            synchronized (_lock) {
                _stat_totFetchTime += timeDiff;
                if(timeDiff > _stat_maxFetchTime) {
                    _stat_maxFetchTime = timeDiff;
                }

                if(timeDiff < _stat_minFetchTime) {
                    _stat_minFetchTime = timeDiff;
                }
            }

            if (timeDiff > WARN_FETCH_TIME) {
                _log.warn("Collection of metric: '" + dsn +
                          "' took: " + timeDiff + "ms");
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
                    _rs._retry.add(_meas);
                    return;
                }
                _sender.processData(_meas.getDsnID(), data,
                                    _meas.getDerivedID());
                synchronized (_lock) {
                    _stat_numMetricsFetched++;
                }
            } else {
                synchronized (_lock) {
                    _stat_numMetricsFailed++;
                }
            }
        }
    }

    private void collect(ResourceSchedule rs, List items)
    {
        for (int i=0; i<items.size() && (!_shouldDie); i++) {
            ScheduledMeasurement meas =
                (ScheduledMeasurement)items.get(i);
            ParsedTemplate tmpl = toParsedTemplate(meas);

            ExecutorService svc;
            synchronized (_executors) {
                svc = (ExecutorService)_executors.get(tmpl.plugin);
                if (svc == null) {
                    _log.info("Creating executor for domain '" + tmpl.plugin + "'");
                    // TODO: Optional thread pool
                    svc = Executors.newSingleThreadExecutor();
                    _executors.put(tmpl.plugin, svc);
                }
            }

            svc.submit(new MetricRunner(rs, meas));
        }
    }

    private long collect(ResourceSchedule rs) {
        long timeOfNext;
        long now = System.currentTimeMillis();
        Schedule schedule = rs._schedule;
        try {
            timeOfNext = schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            return POLL_PERIOD + now;
        }

        if (rs._lastUnreachble != 0) {
            if ((now - rs._lastUnreachble) > UNREACHABLE_EXPIRE) {
                rs._lastUnreachble = 0;
                _log.info("Re-enabling metrics for: " + rs._id);
            }
        }

        rs._collected.clear();

        if (rs._retry.size() != 0) {
            if (_log.isDebugEnabled()) {
                _log.debug("Retrying " + rs._retry.size() + " items (MetricValue.FUTUREs)");
            }
            collect(rs, rs._retry);
            rs._retry.clear();
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
        
        Map schedules = null;
        synchronized (_schedules) {
            if (_schedules.size() == 0) {
                //nothing scheduled
                timeOfNext = POLL_PERIOD + System.currentTimeMillis();
            } else {
                schedules = new HashMap(_schedules);
            }
        }

        if (_log.isDebugEnabled()) {
            _log.debug("Platform schedule is null");
        }

        if (schedules != null) {
            for (Iterator it = schedules.values().iterator();
            it.hasNext() && (!_shouldDie);) {

                ResourceSchedule rs = (ResourceSchedule) it.next();
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
    public double getNumMetricsScheduled() {
        synchronized (_lock) {
            return _stat_numMetricsScheduled;            
        }
    }

    /**
     * @return The number of metrics which were attempted to be fetched (failed or successful)
     */
    public double getNumMetricsFetched() {
        synchronized (_lock) {
            return _stat_numMetricsFetched;
        }
    }

    /**
     * @return Get the number of metrics which resulted in an error when collected
     */
    public double getNumMetricsFailed() {
        synchronized (_lock) {
            return _stat_numMetricsFailed;
        }
    }

    /**
     * @return The total time spent fetching metrics
     */
    public double getTotFetchTime() {
        synchronized (_lock) {
            return _stat_totFetchTime;
        }
    }

    /**
     * @return The maximum time spent fetching a metric
     */
    public double getMaxFetchTime() {
        synchronized (_lock) {
            if(_stat_maxFetchTime == Long.MIN_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return _stat_maxFetchTime;
        }
    }

    /**
     * @return The minimum time spent fetching a metric
     */
    public double getMinFetchTime() {
        synchronized (_lock) {
            if(_stat_minFetchTime == Long.MAX_VALUE) {
                return MetricValue.VALUE_NONE;
            }
            return _stat_minFetchTime;
        }
    }
}
