/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.product.MeasurementValueGetter;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.ScheduledItem;
import org.hyperic.util.schedule.UnscheduledItemException;

/**
 * The realtime availability thread maintains collection of
 * selected resources whose availabilities will be collected
 * outside of normal collection cycle.
 *
 * This class only tracks availability metrics. However metrics
 * are requested through same path as other metrics.
 * 
 * Metric is passed to sender only if status is changed from
 * previously collected status.
 *
 * Configuration through agent.properties:
 * RealtimeAvailabilityThread.enable=[true|false]
 * RealtimeAvailabilityThread.eid.1=1:10123,5
 * RealtimeAvailabilityThread.eid.2=2:10123,20
 */
public class RealtimeAvailabilityThread
    implements Runnable {

    private static final Log log =
        LogFactory.getLog(RealtimeAvailabilityThread.class.getName());
    
    // true or false - enable this thread through agent.properties
    public final static String PROP_ENABLE = "RealtimeAvailabilityThread.enable";
    
    // key for the entity configuration
    public final static String PROP_EIDS = "RealtimeAvailabilityThread.eid";
    
    // default interval in seconds
    private final static int DEF_INTERVAL = 20; 
    
    // How often we check schedules when we think they are empty.
    private static final int POLL_PERIOD = 5000;
    
    // This guy can send my metrics to server.
    private SenderThread sender;
    
    // Sleeper cell / mutex
    private Object interrupter;
        
    // stored realtime schedules 
    private Map<String, RealtimeSchedule> schedules;

    // handle to eids configuration from agent properties
    private Hashtable<String, Integer> config;
    
    // plugin manager
    private MeasurementValueGetter manager;
    
    // flag telling if thread loop should break
    private volatile boolean shouldDie;

    /**
     * Constructs realtime thread.
     * 
     * @param sender
     * @param manager
     * @throws AgentStartException
     */
    public RealtimeAvailabilityThread(SenderThread sender, MeasurementValueGetter manager)
        throws AgentStartException {
        this.sender = sender;
        this.manager = manager;
        this.interrupter  = new Object();
        this.schedules = new HashMap<String, RealtimeSchedule>();
        this.config = new Hashtable<String, Integer>();
        initConfig();
    }

    /**
     * Schedule class storing needed information for
     * realtime scheduled resource.
     *
     */
    private static class RealtimeSchedule {
        private Schedule schedule = new Schedule();
        AppdefEntityID eid;
        // null, initially we know nothing hasn't been collected
        MetricValue lastVal = null;
        // < 0 = not set or unknown
        private long realtimeInterval = -1;
    }

    /**
     * Gets scheduled measurement from local schedule storage.
     * 
     * 
     * @param meas
     * @return Existing RealtimeSchedule from storage, creates if doesn't exist.
     */
    private RealtimeSchedule getSchedule(ScheduledMeasurement meas) {
        String key = meas.getEntity().getAppdefKey();
        RealtimeSchedule schedule = null;
        synchronized (schedules) {
            schedule = schedules.get(key);
            
            if(log.isDebugEnabled()){
                if(schedule != null) log.debug("Found existing schedule for " + key);
            }
            
            if(schedule == null){
                log.debug("Creating new schedule for " + key);
                schedule = new RealtimeSchedule();
                schedule.eid = meas.getEntity();
                schedules.put(key, schedule);
            }
        }
        return schedule;
    }

    /**
     * Main method ran by thread. Just loops and waits
     * until it's asked to die.
     */
    public void run() {
        while (shouldDie == false) {
            long timeOfNext = collect();
            long now = System.currentTimeMillis();
            if (timeOfNext > now) {
                long wait = timeOfNext - now;
                if(log.isDebugEnabled())
                	log.debug("Sleeping.. " + wait + " ms");
                try {
                    synchronized (interrupter) {
                        interrupter.wait(wait);
                    }
                } catch (InterruptedException e) {
                }
            }
        }
        log.debug("Realtime thread is done after this message");
    }

    /**
     * Utility method to ask thread to die.
     */
    public void die() {
        log.debug("Request to kill realtime thread");
        shouldDie = true;
        interruptMe();
    }

    /**
     * Calling mutex lock to wake up the thread.
     */
    private void interruptMe(){
        synchronized (interrupter) {
            interrupter.notify();
        }
    }
    
    /**
     * Frontend method to start collection cycle.
     */
    private long collect() {
        
        log.debug("Running collect()...");
        
        long timeOfNext = 0;
        
        // use copy to prevent blocking
        Map<String, RealtimeSchedule> schedulesCopy = null;
        
        synchronized (schedules) {
            if(schedules.size() == 0){
                timeOfNext = POLL_PERIOD + System.currentTimeMillis();
            } else {
                schedulesCopy = new HashMap<String, RealtimeSchedule>(schedules);
            }
        }
        
        if (schedulesCopy != null) {
            
            Iterator<RealtimeSchedule> it = schedules.values().iterator();

            while(it.hasNext() && !shouldDie) {
                RealtimeSchedule rs = it.next();
                
                try {
                    long next = collect(rs);
                    if (timeOfNext == 0) {
                        timeOfNext = next;
                    }
                    else {
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
     * 
     * 
     * @param rs
     * @return Timestamp when this method should be ran again.
     */
    private long collect(RealtimeSchedule rs) {
        
        long timeOfNext;
        long now = System.currentTimeMillis();
        Schedule schedule = rs.schedule;
        
        try {
            timeOfNext = schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            return POLL_PERIOD + now;
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
    
    /**
     * 
     * @param rs
     * @param items
     * @param isUnreachable
     */
    private void collect(RealtimeSchedule rs, List<ScheduledMeasurement> items) {
        
        for (int i=0; i<items.size() && (shouldDie == false); i++) {
            ScheduledMeasurement meas = items.get(i);

            MetricValue data = null;

            ParsedTemplate dsn = toParsedTemplate(meas, rs);


            try {
                log.debug("Getting data...");
                data = getValue(dsn);
                
                boolean needToSend = false;
                
                if(rs.lastVal != null)
                    needToSend = !(rs.lastVal.compareTo(data) == 0);
                
                rs.lastVal = data;
                
                if(needToSend) {
                    log.debug("Asking to send data for " + meas.getEntity().getAppdefKey() + ": " + data.toString());
                    sender.processData(meas.getDsnID(), data, 
                            meas.getDerivedID());
                    sender.sendNow();                    
                } else {
                    log.debug("Not sending.. but got new data for " + meas.getEntity().getAppdefKey() + ": " + data.toString());                    
                }
                
            } catch (PluginNotFoundException e) {
                log.error(e.toString(),e);
            } catch (MetricNotFoundException e) {
                log.error(e.toString(),e);
            } catch (MetricUnreachableException e) {
                log.error(e.toString(),e);
            } catch (PluginException e) {
                log.error(e.toString(),e);
            }
        }

    }

    /**
     * Schedule a measurement
     * 
     * @param meas Scheduled measurement
     */
    public void scheduleMeasurement(ScheduledMeasurement meas) {
        
        log.debug("Checking " + meas.getEntity().getAppdefKey() + " for scheduling.");
        
        // we just check if it's an availability metric and
        // enabled in configuration.
        String cat = meas.getCategory().toLowerCase();
        log.debug("meas category is " + cat);
        if(cat.contains("availability")) {
            
            
            if(!filterEIDS(meas.getEntity()))
                return;

            RealtimeSchedule rs = getSchedule(meas);

            // we're not interested interval found from measurement, 
            // instead use the one from configuration
            int interval = config.get(meas.getEntity().getAppdefKey());
            rs.realtimeInterval = interval*1000;
            try {
                log.debug("Scheduling resource " +
                          meas.getEntity().getAppdefKey() +
                          " with " + interval + " second interval");
                rs.schedule.scheduleItem(meas, interval*1000, true, true);
            } catch (ScheduleException e) {
                log.error("Unable to schedule realtime metric. ", e);
            }
        }
    }
    
    /**
     * Unschedule a measurement
     * 
     * @param ent Resource entity id
     */
    public void unscheduleMeasurements(AppdefEntityID ent) throws UnscheduledItemException {
        String key = ent.getAppdefKey();
        ScheduledItem[] items = null;

        RealtimeSchedule rs = null;
        synchronized (schedules) {
            rs = schedules.remove(key);
        }
        
        if (rs == null) {
            throw new UnscheduledItemException("No measurement schedule for: " + key);
        }
        
        items = rs.schedule.getScheduledItems();

        for (int i=0; i<items.length; i++) {
            ScheduledMeasurement meas = (ScheduledMeasurement)items[i].getObj();
            ParsedTemplate tmpl = getParsedTemplate(meas);
            tmpl.metric.setInterval(-1);
        }

    }

    /**
     * Checking if resource entity id is added to
     * agent.properties.
     * 
     * @param ent Resource entity id
     * @return True if found, false otherwise.
     */
    private boolean filterEIDS(AppdefEntityID ent){
        log.debug("Checking aid " + ent.getAppdefKey());
        boolean found = config.containsKey(ent.getAppdefKey());
        log.debug("Found aid from config? " + found);
        return found;
    }

    /**
     * Initialises needed configuration from agent.properties.
     * Getting configured eids and intervals.
     */
    private void initConfig() {
        AgentConfig conf = AgentDaemon.getMainInstance().getBootConfig();
        Properties p = conf.getBootProperties();
        
        // now iterate through props and populate config
        Enumeration e = p.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if(key.startsWith(PROP_EIDS)) {
                try {
                    String v = p.getProperty(key);
                    String[] fields = v.split(",");
                    if(fields.length == 1)
                        config.put(fields[0], DEF_INTERVAL);
                    else
                        config.put(fields[0], Integer.parseInt(fields[1]));
                } catch (Exception ee) {
                    // right so assume correct format and just
                    // forget if we get error
                }
            }
        }
        
    }

    static class ParsedTemplate {
        String plugin;
        Metric metric;
        
        public String toString() {
            return plugin + ":" + metric.toDebugString();
        }
    }

    /**
     * Creates parsed templates from scheduled measurement.
     * 
     * @param meas Scheduled measurement
     * @return Parsed template
     */
    private ParsedTemplate toParsedTemplate(ScheduledMeasurement meas, RealtimeSchedule rs) {
        AppdefEntityID aid = meas.getEntity();
        int id = aid.getID();
        int type = aid.getType();
        ParsedTemplate tmpl = getParsedTemplate(meas);
        tmpl.metric.setId(type, id);
        tmpl.metric.setCategory(meas.getCategory());
        tmpl.metric.setInterval(meas.getInterval());
        tmpl.metric.setRealtimeInterval(rs.realtimeInterval);
        return tmpl;
    }

    /**
     * Utility function to create a ParsedTemplate object.
     * 
     * @param meas Scheduled measurement
     * @return Parsed template
     */
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

    /**
     * Requests metric value from measurement manager.
     * 
     * @param tmpl Parsed template
     * @return Metric value
     * @throws PluginException
     * @throws PluginNotFoundException
     * @throws MetricNotFoundException
     * @throws MetricUnreachableException
     */
    private MetricValue getValue(ParsedTemplate tmpl)
    throws PluginException, PluginNotFoundException,
           MetricNotFoundException, MetricUnreachableException {
        return manager.getValue(tmpl.plugin, tmpl.metric);
    }
    

}
