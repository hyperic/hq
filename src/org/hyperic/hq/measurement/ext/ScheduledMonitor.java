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

package org.hyperic.hq.measurement.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.MeasurementReportConstructor;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.UnscheduledItemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The abstract data class to be extended to be able to
 * communicate with specific agent types to schedule
 * measurement sampling.
 */
public abstract class ScheduledMonitor
    implements MonitorInterface, java.io.Serializable {
    public static String QNAME = "queue/monitorAlarmQueue";

    private Log log = LogFactory.getLog(ScheduledMonitor.class);
    
    // Static Messenger for convenience
    private static Messenger sender = new Messenger();

    /** Get the subclass' static Schedule object
     * @return a static Schedule
     */
    protected abstract Schedule getSchedule();

    /** Get the subclass' static Hashtable object that keeps
     * track of the mapping between marker and ScheduledItem
     * ID
     * @return a static Hashtable
     */
    protected abstract Hashtable getScheduleMap();

    protected abstract MetricValue[] getValues(RawMeasurement[] measurements);

    public boolean ping(AgentValue agent) {
        return true;
    }

    /**
     * Check the schedule to sleep and then wake up to do some measurements
     */
    public void scheduleMeasurements() {
        try {
            long sleepTime = getSchedule().getTimeOfNext() -
                             System.currentTimeMillis();
        
            try {
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // Do nothing, let's just get the value now
            }

            // Don't want duplicates
            HashMap measurements  = new HashMap();
            HashMap markers       = new HashMap();
            List scheduledItems = getSchedule().consumeNextItems();
            for (Iterator i = scheduledItems.iterator(); i.hasNext();) {
                ScheduledItem si = (ScheduledItem) i.next();
                RawMeasurement[] rms = si.getRmvs();
                for (int ind = 0; ind < rms.length; ind++) {
                    Integer rmId = rms[ind].getId();
                    if (measurements.containsKey(rmId))
                        continue;

                    // Collection measurements
                    measurements.put(rmId, rms[ind]);

                    // Lookup the marker
                    markers.put(rmId, si.getDmv().getId());
                }
            }

            // Now get the unique values
            Collection msCol = measurements.values();
            RawMeasurement[] rmvs =
                (RawMeasurement[]) msCol.toArray(new RawMeasurement[msCol.size()]);

            MetricValue[] values = getValues(rmvs);

            try {   // Now send the report
                MeasurementReportConstructor constr;

                constr = new MeasurementReportConstructor();
                for (int i = 0; i < values.length; i++) {
                    Integer rmId = (Integer) markers.get(rmvs[i].getId());
                    constr.addDataPoint(rmId.intValue(),
                                        rmvs[i].getId().intValue(),
                                        values[i]);
                }

                // Create a measurement report
                MeasurementReport report = new MeasurementReport();

                report.setClientIdList(constr.constructDSNList());
                sender.sendMessage(MeasurementConstants.REPORT_QUEUE, report);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Let's schedule the next one
            long nextTime = getSchedule().getTimeOfNext();

            // Would have thrown EmptyScheduleException if there's nothing
            sender.sendMessage(QNAME, this);
        } catch (EmptyScheduleException e) {
            // Nothing to do
            return;
        }
    }

    /** Schedule the measurement to be retrieved at
     * specified intervals
     * @param agent Agent to contact -- fulfills MonitorInterface contract
     * @param schedule Schedule of metrics to collect
     */
    public void schedule(AgentValue agent, SRN srn,
                         ScheduleMetricInfo[] schedule)
        throws MonitorAgentException
    {
        // Keep track of the next time before we scheduled
        long before = 0;
        
        try {
            before = getSchedule().getTimeOfNext();
        } catch (EmptyScheduleException e) {
            // No problem, go with 0
        }

        for(int i=0; i<schedule.length; i++){
            DerivedMeasurement dMetric = schedule[i].getDerivedMetric();
            RawMeasurement[] rMetrics = schedule[i].getRawMetrics();

            try {
                // Create new UnscheduleMetricInfo
                UnscheduleMetricInfo umi =
                    new UnscheduleMetricInfo(srn.getEntity());
                
                // Unschedule first
                this.unschedule(agent, new UnscheduleMetricInfo[] { umi });
            } catch (MonitorAgentException e) {
                // That's fine, no worries
            }
            
            long sid;
            try {
                sid =
                    getSchedule().scheduleItem(new ScheduledItem(dMetric,
                                                                 rMetrics),
                                               dMetric.getInterval(), true);
            } catch (ScheduleException e) {
                //XXX: We continue through the schedule rather than bail with
                //     an Exception.
                log.error("Unable to schedule metric '" +
                          dMetric.getTemplate() + "', skipping. Cause is " +
                          e.getMessage(), e);
                return;
            }

            // See if we changed the schedule
            long after = 0;
            try {
                after = getSchedule().getTimeOfNext();
            } catch (EmptyScheduleException e) {
                // Not likely, but go with 0
            }

            // Save it for later
            Hashtable schedMap = this.getScheduleMap();

            synchronized(schedMap){
                HashSet hs = (HashSet)schedMap.get(srn.getEntity());

                if(hs == null){
                    hs = new HashSet();
                    schedMap.put(srn.getEntity(), hs);
                }

                hs.add(new Long(sid));
            }

            // We may have changed it, set a timer
            if (before != after) {
                Messenger sender = new Messenger();
                sender.sendMessage(QNAME, this);
            }
        }
    }

    /**
     * Unschedule measurements
     */
    public void unschedule(AgentValue agent, UnscheduleMetricInfo[] schedule)
        throws MonitorAgentException 
    {
        Hashtable schedMap = this.getScheduleMap();

        synchronized(schedMap){
            for (int i = 0; i < schedule.length; i++) {
                HashSet hs;

                hs = (HashSet)schedMap.get(schedule[i].getEntity());
                if(hs == null){
                    continue;
                }

                for(Iterator j=hs.iterator(); j.hasNext(); ){
                    Long id = (Long)j.next();
                    
                    try {
                        getSchedule().unscheduleItem(id.longValue());
                    } catch(UnscheduledItemException e){
                        e.printStackTrace();
                    }
                }

                schedMap.remove(schedule[i].getEntity());
            }
        }
    }

    private class ScheduledItem {

        private DerivedMeasurement _dm;
        private RawMeasurement[] _rms;
        
        public ScheduledItem(DerivedMeasurement dm,
                             RawMeasurement[] rms) {
            _dm = dm;
            _rms = rms;
        }

        public DerivedMeasurement getDmv() {
            return _dm;
        }

        public void setDmv(DerivedMeasurement dm) {
            _dm = dm;
        }

        public RawMeasurement[] getRmvs() {
            return _rms;
        }

        public void setRmvs(RawMeasurement[] rms) {
            _rms = rms;
        }
    }
}
