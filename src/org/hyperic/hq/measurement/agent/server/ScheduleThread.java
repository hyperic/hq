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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorIncalculableException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduledItem;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.UnscheduledItemException;
import org.hyperic.util.TimeUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final  int  POLL_PERIOD = 1000;
    private static final int UNREACHABLE_EXPIRE = (60 * 1000) * 5;
    private static final long WARN_FETCH_TIME = 5 * 1000; // 5 seconds.

    private          IntHashMap[] unreachable; // IDs -> last time unreachable
    private          Schedule  schedule;    // Internal schedule of DSNs, etc
    private          Hashtable entSchedule; // Hash of ent IDs->schedule IDs
    private volatile boolean   shouldDie;   // Should I shut down?
    private volatile Thread    myThread;    // Thread the 'run' is running in
    private          Object    interrupter; // Interrupt object
    private          HashMap   dsnErrors;   // Hash of DSNs to their errors

    private MeasurementPluginManager manager;
    private Log                      log;
    private SenderThread             sender;  // Guy handling the results

    private long stat_numMetricsFetched = 0;
    private long stat_numMetricsFailed  = 0;
    private long stat_totFetchTime      = 0;
    private long stat_maxFetchTime      = Long.MIN_VALUE;
    private long stat_minFetchTime      = Long.MAX_VALUE;

    ScheduleThread(SenderThread sender, MeasurementPluginManager manager)
        throws AgentStartException 
    {
        this.unreachable  = new IntHashMap[4];
        this.unreachable[AppdefEntityConstants.APPDEF_TYPE_PLATFORM] =
            new IntHashMap();
        this.unreachable[AppdefEntityConstants.APPDEF_TYPE_SERVER] =
            new IntHashMap();
        this.unreachable[AppdefEntityConstants.APPDEF_TYPE_SERVICE] =
            new IntHashMap();
        
        this.schedule     = new Schedule();
        this.entSchedule  = new Hashtable();
        this.shouldDie    = false;
        this.myThread     = null;
        this.interrupter  = new Object();
        this.manager      = manager;
        this.log          = LogFactory.getLog(ScheduleThread.class);
        this.sender       = sender;               
        this.dsnErrors    = new HashMap();
    }

    private void interruptMe(){
        synchronized(this.interrupter){
            this.interrupter.notify();
        }
    }

    /**
     * Unschedule a previously scheduled, repeatable measurement.  
     *
     * @param ent Entity to unschedule metrics for
     *
     * @throws UnscheduledItemException indicating the passed ID was not found
     */
    void unscheduleMeasurements(AppdefEntityID ent)
        throws UnscheduledItemException
    {
        // Synchronize on the hash to make sure someone isn't trying to 
        // add to an entity when we are trying to delete it
        synchronized(this.entSchedule){
            Vector mIDList = (Vector)this.entSchedule.get(ent);

            if(mIDList == null){
                this.log.debug("Unscheduling of metrics for " + ent + 
                               " failed:  No metrics scheduled for that " + 
                               "entity");
                return;
            }
            
            if(this.log.isDebugEnabled()){
                this.log.debug("Unscheduling metrics for " + ent + ": " +
                               mIDList);
            }

            for(int i=0; i<mIDList.size(); i++){
                Long mID = (Long)mIDList.get(i);

                try {
                    this.schedule.unscheduleItem(mID.longValue());
                } catch(UnscheduledItemException exc){
                    throw new AgentAssertionException("Tried to unschedule " +
                                                      "something which wasnt" +
                                                      " scheduled", exc);
                }
            }
            
            this.entSchedule.remove(ent);
        }
    }

    /**
     * Schedule a measurement to be taken at a given interval.  
     *
     * @param meas Measurement to schedule
     */

    void scheduleMeasurement(ScheduledMeasurement meas){
        long oldNextTime, newNextTime = 0;

        synchronized(this.entSchedule){
            Vector  mIDList;
            long    mID;

            try {
                oldNextTime = this.schedule.getTimeOfNext();
            } catch(EmptyScheduleException exc){
                oldNextTime = 0;
            }

            try {
                mID = this.schedule.scheduleItem(meas, meas.getInterval(), 
                                                 true, true);
            } catch (ScheduleException e) {
                //XXX: We continue through the schedule rather than bail with
                //     an Exception.
                log.error("Unable to schedule metric '" +
                          logMetric(meas.getDSN()) + "', skipping. Cause is " +
                          e.getMessage(), e);
                return;
            }

            mIDList = (Vector)this.entSchedule.get(meas.getEntity());
            if(mIDList == null){
                mIDList = new Vector();
                this.entSchedule.put(meas.getEntity(), mIDList);
            } 
            mIDList.add(new Long(mID));
            
            try {
                newNextTime = this.schedule.getTimeOfNext();
            } catch(EmptyScheduleException exc){
                throw new AgentAssertionException("Schedule should have at " +
                                                  "least one entry", exc);
            }
        }

        // Check to see if we scheduled something sooner than the
        // running thread is expecting
        if(newNextTime < oldNextTime){
            this.interruptMe();
        }
    }

    /**
     * Shut down the schedule thread.  
     */

    void die(){
        this.shouldDie = true;
        this.interruptMe();
    }

    private void logCache(String basicMsg, String dsn, String msg,
                          Exception exc, boolean printStack){
        String oldMsg;
        boolean isDebug = this.log.isDebugEnabled();

        synchronized(this.dsnErrors){
            oldMsg = (String)this.dsnErrors.get(dsn);
        }

        if(!isDebug && oldMsg != null && oldMsg.equals(msg)){
            return;
        }

        if(isDebug){
            this.log.error(basicMsg + " while processing Metric '" + dsn + "'",
                           exc);
        } else {
            this.log.error(basicMsg + ": " + msg);
            if (printStack) {
                this.log.error("Stack trace follows:", exc);
            }
        }

        synchronized(this.dsnErrors){
            this.dsnErrors.put(dsn, msg);
        }
    }

    /**
     * A method which does the main logging for the run() method.  It
     * ensures that we don't perform excessive logging when plugins
     * generate a lot of errors. 
     */
    private void logCache(String basicMsg, String dsn, Exception exc){
        logCache(basicMsg, dsn, exc.getMessage(), exc, false);
    }

    private void clearLogCache(String dsn){
        synchronized(this.dsnErrors){
            this.dsnErrors.remove(dsn);
        }
    }

    //remove connections properties, if any, to avoid logging passwords
    private String logMetric(String metric) {
        if (log.isDebugEnabled()) {
            return metric;
        }
        StringTokenizer tok = new StringTokenizer(metric, ":");
        if (tok.countTokens() == 4) {
            int ix = metric.lastIndexOf(':');
            return metric.substring(0, ix);
        }
        else {
            return metric;
        }
    }

    private MetricValue getValue(ScheduledMeasurement meas)
        throws PluginException, PluginNotFoundException,
               MetricNotFoundException, MetricUnreachableException
    {
        String template = meas.getDSN();
        AppdefEntityID aid = meas.getEntity();
        int id = aid.getID();
        int type = aid.getType();

        //duplicating some code from MeasurementPluginManager
        //so we can do Metric.setId
        int ix = template.indexOf(":");
        String plugin = template.substring(0, ix);
        String metric = template.substring(ix+1, template.length());
        Metric parsedMetric = Metric.parse(metric);
        parsedMetric.setId(type, id);
        parsedMetric.setCategory(meas.getCategory());
        parsedMetric.setInterval(meas.getInterval());
        return this.manager.getValue(plugin, parsedMetric);    
    }

    /**
     * The main loop of the ScheduleThread, which watches the schedule
     * waits the appropriate time, and executes scheduled operations.
     */
    public void run(){
        this.myThread = Thread.currentThread();
        ArrayList retry = new ArrayList();
        boolean debug = log.isDebugEnabled();

        while(this.shouldDie == false)
        {
            MetricValue data = null;
            List items;
            Map itemsMap;
            long timeOfNext;
            long currTime = System.currentTimeMillis();
            
            try {
                synchronized(this.entSchedule) {
                    timeOfNext = this.schedule.getTimeOfNext();
                }
            } catch(EmptyScheduleException exc) {
                timeOfNext = ScheduleThread.POLL_PERIOD+currTime;
            }

            if (timeOfNext > currTime)
            {
                /*
                String buf = TimeUtil.toString(currTime);
                this.log.debug(buf + "("+currTime+"): Sleeping " +
                               (timeOfNext-System.currentTimeMillis()) +
                               " to next batch");
                */
                synchronized(this.interrupter)
                {
                    try
                    {
                        while (timeOfNext > System.currentTimeMillis()) {
                            this.interrupter.wait(
                                timeOfNext - System.currentTimeMillis());
                        }
                    } catch(InterruptedException exc) {
                        this.log.debug("Schedule thread kicked");
                    }
                }
            }

            synchronized(this.entSchedule)
            {
                if(this.schedule.getNumItems() == 0)
                    continue;
                
                try
                {
                    if (System.currentTimeMillis() <
                        this.schedule.getTimeOfNext()) {
                        continue; // Not yet
                    }
                    itemsMap = getUniqItemsMap(schedule.consumeNextItems());
                    items = new ArrayList(itemsMap.values());

                    if (retry.size() != 0)
                    {
                        log.debug("Retrying " + retry.size() +
                                  " items (MetricValue.FUTUREs)");
                        for (Iterator i=retry.iterator(); i.hasNext(); )
                        {
                            ScheduledMeasurement meas =
                                (ScheduledMeasurement)i.next();
                            Integer dsnId = new Integer(meas.getDsnID());
                            if (itemsMap.containsKey(dsnId))
                                continue;
                            items.add(meas);
                            if (debug);
                                log.debug("retrying -> "+meas);
                        }
                        retry.clear();
                    }
                } catch(EmptyScheduleException exc) {
                    continue;
                }
            }

            /*
            if (debug) {
                String now = TimeUtil.toString(System.currentTimeMillis());
                log.debug(now+": Start processing batch");
            }
            */

            for(int i=0; i<items.size() && this.shouldDie == false; i++)
            {
                ScheduledMeasurement meas = (ScheduledMeasurement)items.get(i);
                /*
                if (debug) {
                    String now = TimeUtil.toString(System.currentTimeMillis());
                    log.debug(now+": Start processing meas=["+meas+"]");
                }
                */
                AppdefEntityID aid = meas.getEntity();
                String category = meas.getCategory();
                int id = aid.getID();
                int type = aid.getType();
                IntHashMap unreachable = this.unreachable[type];
                boolean success = false;
                String dsn = meas.getDSN();
                long getTime, timeDiff;

                this.stat_numMetricsFetched++;
                getTime = System.currentTimeMillis();

                Long lastFailed = (Long)unreachable.get(id);
                if (lastFailed != null)
                {
                    long elapsed = (getTime - lastFailed.longValue()); 
                    if (elapsed > UNREACHABLE_EXPIRE) {
                        unreachable.remove(id);
                        this.log.info("Re-enabling metrics for type=" + type + 
                                      " id=" + id);
                    } else {
                        if (!category.equals(MeasurementConstants.
                                             CAT_AVAILABILITY)) {
                            // Prevent stacktrace bombs if a resource is down,
                            // but don't skip processing availability metrics.
                            this.stat_numMetricsFailed++;
                            continue;
                        }
                    }
                }

                // XXX -- We should do something with the exceptions here.
                //        Maybe send some kind of error back to the
                //        bizapp?
                try {
                    data    = getValue(meas);
                    success = true;
                    this.clearLogCache(dsn);
                } catch(PluginNotFoundException exc){
                    this.logCache("Plugin not found", dsn, exc);
                } catch(PluginException exc){
                    this.logCache("Measurement plugin error", dsn, exc);
                } catch(MetricInvalidException exc){
                    this.logCache("Invalid Metric requested", dsn, exc);
                } catch(MetricNotFoundException exc){
                    this.logCache("Metric Value not found", dsn, exc);
                } catch(MetricUnreachableException exc){
                    this.logCache("Metric unreachable", dsn, exc);
                    lastFailed = new Long(getTime);
                    unreachable.put(id, lastFailed);
                    this.log.warn("Disabling metrics for type=" + type + 
                                  " id=" + id);
                } catch(Exception exc){
                    // Unexpected exception
                    this.logCache("Error getting measurement value",
                                  dsn, exc.toString(), exc, true);
                }
                /*
                if (debug) {
                    String now = TimeUtil.toString(System.currentTimeMillis());
                    log.debug(now+": Done processing meas=["+meas+"]");
                }
                */
                
                // Stats stuff
                timeDiff = System.currentTimeMillis() - getTime;
                this.stat_totFetchTime += timeDiff;
                if(timeDiff > this.stat_maxFetchTime)
                    this.stat_maxFetchTime = timeDiff;

                if(timeDiff < this.stat_minFetchTime)
                    this.stat_minFetchTime = timeDiff;

                if (timeDiff > WARN_FETCH_TIME) {
                    this.log.warn("Collection of metric: '" + logMetric(dsn) + 
                                  "' took: " + timeDiff + "ms");
                }

                if(success)
                {
                    if (debug) {
                        this.log.debug("[" + type + ":" + id + ":" + category +
                                       "] Metric='" + dsn + "' -> " + data +
                                       " timestamp=" + data.getTimestamp());
                    }
                    if (data.isNone()) {
                        //wouldn't be inserted into the database anyhow
                        //but might as well skip sending this to the server.
                        continue;
                    }
                    else if (data.isFuture()) {
                        //for example, first time collecting an exec: metric
                        //adding to this list will cause the metric to be
                        //collected next time the schedule has items to consume
                        //rather than waiting for the metric's own interval
                        //which could take much longer to hit
                        //(e.g. Windows Updates on an 8 hour interval)
                        retry.add(meas);
                        continue;
                    }
                    this.sender.processData(meas.getDsnID(), data, 
                                            meas.getDerivedID());
                } else {
                    this.stat_numMetricsFailed++;
                }
            }
            /*
            if (debug) {
                String now = TimeUtil.toString(System.currentTimeMillis());
                log.debug(now+": Done processing batch");
            }
            */
        }
    }

    private Map getUniqItemsMap(List items)
    {
        Map rtn = new HashMap();
        for (Iterator i=items.iterator(); i.hasNext(); )
        {
            ScheduledMeasurement meas = (ScheduledMeasurement)i.next();
            Integer dsnId = new Integer(meas.getDsnID());
            rtn.put(dsnId, meas);
        }
        return rtn;
    }

    /**
     * MONITOR METHOD:  Get the number of metrics in the schedule
     */
    public double getNumMetricsScheduled() 
        throws AgentMonitorException 
    {
        return this.schedule.getNumItems();
    }

    /**
     * MONITOR METHOD:  Get the number of metrics which were attempted
     *                  to be fetched (failed or successful)
     */
    public double getNumMetricsFetched() 
        throws AgentMonitorException 
    {
        return this.stat_numMetricsFetched;
    }

    /**
     * MONITOR METHOD:  Get the number of metrics which resulted in an
     *                  error when collected
     */
    public double getNumMetricsFailed() 
        throws AgentMonitorException 
    {
        return this.stat_numMetricsFailed;
    }

    /**
     * MONITOR METHOD:  Get the total time spent fetching metrics
     */
    public double getTotFetchTime() 
        throws AgentMonitorException 
    {
        return this.stat_totFetchTime;
    }

    /**
     * MONITOR METHOD:  Get the maximum time spent fetching a metric
     */
    public double getMaxFetchTime() 
        throws AgentMonitorException 
    {
        if(this.stat_maxFetchTime == Long.MIN_VALUE)
            throw new AgentMonitorIncalculableException("No fetches yet");

        return this.stat_maxFetchTime;
    }

    /**
     * MONITOR METHOD:  Get the minimum time spent fetching a metric
     */
    public double getMinFetchTime() 
        throws AgentMonitorException 
    {
        if(this.stat_minFetchTime == Long.MAX_VALUE)
            throw new AgentMonitorIncalculableException("No fetches yet");

        return this.stat_minFetchTime;
    }
}
