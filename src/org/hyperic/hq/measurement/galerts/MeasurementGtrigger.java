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

package org.hyperic.hq.measurement.galerts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementScheduleZevent;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MeasurementScheduleZevent.MeasurementScheduleZeventSource;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.HeartBeatZevent;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.HeartBeatZevent.HeartBeatZeventSource;


/**
 * This is a simple trigger which will fire when the criteria is met, and
 * not-fire when the criteria is .. not met. 
 */
public class MeasurementGtrigger 
    extends Gtrigger
{
    private static final Log _log = LogFactory.getLog(MeasurementGtrigger.class);
    
    /**
     * We need to allow up to a 1 minute time skew with the agent behind the 
     * server.
     */
    private static final int AGENT_SERVER_TIME_SKEW_TOLERANCE=60*1000;
    
    /**
     * The minimum assumed measurement collection interval. This 
     * value doesn't have to be exact since it's only used to 
     * estimate the time window if we have the case where none 
     * of the resources in the group are collecting on the metric.
     */
    private static final int MIN_COLLECTION_INTERVAL=60*1000;
    
    private final SizeComparator     _sizeCompare;
    private final int                _numResources; // Num needed to match
    private final boolean            _isPercent;
    private final Integer            _templateId;
    private final ComparisonOperator _comparator;
    private final Float              _metricVal;
    private final Set                _interestedEvents;
    private final Map                _trackedResources;
    private final boolean            _isNotReportingEventsOffending;
    private final TreeSet            _trackedHeartBeatTimestamps;
    private       String             _triggerName;
    private       String             _partitionDescription;
    private       long               _maxCollectionInterval;
    private       boolean            _isWithinFirstTimeWindow;
    private       boolean            _isTimeWindowingInitialized;
    private       long               _startOfTimeWindowExact; // the start of the time window
    private       long               _startOfTimeWindow;  // the start of the time window, voodooed down
    private       String             _metricName;
    private       int                _groupSize;  // The total size of our group    
    private       ResourceGroup      _resourceGroup;
    
    // These are the resources that have the metric collection interval set
    private final Map                _srcId2CollectionInterval;
    
    MeasurementGtrigger(SizeComparator sizeCompare, 
                        int numResources,
                        boolean isPercent, 
                        int templateId,
                        ComparisonOperator comparator, 
                        float metricVal, 
                        boolean isNotReportingOffending) 
    {
        _sizeCompare      = sizeCompare;
        _numResources     = numResources;
        _isPercent        = isPercent;
        _templateId       = new Integer(templateId);
        _comparator       = comparator;
        _metricVal        = new Float(metricVal);
        _interestedEvents = new HashSet();
        _trackedResources  = new HashMap();
        _metricName       = "Unknown";
        _groupSize        = 0;
        _isNotReportingEventsOffending = isNotReportingOffending;
        _srcId2CollectionInterval = new HashMap();
        _trackedHeartBeatTimestamps = new TreeSet();
        _maxCollectionInterval = MIN_COLLECTION_INTERVAL;
        _isTimeWindowingInitialized = false;
        setTriggerName();
    }
    
    public Set getInterestedEvents() {
        return Collections.unmodifiableSet(_interestedEvents);
    }
    
    public void processEvent(Zevent event) {
        // Process measurement schedule changes.
        if (isMeasurementScheduleEvent(event)) {
            metricCollectionIntervalChanged();
            return;
        }
        
        initializeTimeWindowing(event);
        
        // Evaluate the sliding time window boundary.
        boolean inFirstTimeWindow = isInFirstTimeWindow();
        
        // We only move the time window forward when a heart beat is processed.
        if (isHeartBeatEvent(event)) {
            evaluateNextStartOfTimeWindow(inFirstTimeWindow);            
        }
        
        long endOfTimeWindow = _startOfTimeWindow+2*_maxCollectionInterval;
        
        // Track heart beat events
        if (isHeartBeatEvent(event)) {
            HeartBeatZevent hb = (HeartBeatZevent)event;
                        
            if (!isOlderThanTimeWindowStartTime(hb, _startOfTimeWindow)) {
                track(hb);                
            }
        }
        
        // Track the measurement events.
        if (isMeasurementEvent(event)) {         
            MeasurementZevent me = (MeasurementZevent)event;
            
            if (!isOlderThanTimeWindowStartTime(me, _startOfTimeWindow)) {
                track(me);       
            }
            
            return;
        }
        
        if (!shouldStartEvaluatingMetrics()) {
            return;
        }
        
        // Try to fire.
        Map srcId2ViolatingMetricValue = 
            evaluateResourceMetricsAndReturnViolators(_trackedResources, 
                                                      _startOfTimeWindow, 
                                                      endOfTimeWindow);
        
        tryToFire(srcId2ViolatingMetricValue, _startOfTimeWindow, endOfTimeWindow);
    }
    
    /**
     * Initialize the time windowing. This is only done once when the trigger 
     * processes the first heart beat or measurement event.
     * 
     * @param event The event.
     */
    private void initializeTimeWindowing(Zevent event) {
        if (!_isTimeWindowingInitialized && 
            (isHeartBeatEvent(event) || isMeasurementEvent(event))) {
            setStartOfFirstTimeWindow(System.currentTimeMillis());
            _isTimeWindowingInitialized = true;
        }        
    }
    
    /**
     * Is this a heart beat event?
     * 
     * @param event The event.
     * @return <code>true</code> if this is a {@link HeartBeatZevent}.
     */
    private boolean isHeartBeatEvent(Zevent event) {
        return event instanceof HeartBeatZevent;
    }
    
    /**
     * Is this a measurement schedule event?
     * 
     * @param event The event.
     * @return <code>true</code> if this is a {@link MeasurementScheduleZevent}.
     */
    private boolean isMeasurementScheduleEvent(Zevent event) {
        return event instanceof MeasurementScheduleZevent;
    }
    
    /**
     * Is this a measurement event?
     * 
     * @param event The event.
     * @return <code>true</code> if this is a {@link MeasurementZevent}.
     */
    private boolean isMeasurementEvent(Zevent event) {
        return event instanceof MeasurementZevent;
    }
        
    /**
     * Process the collection interval change.
     */
    private void metricCollectionIntervalChanged() {        
        // It's safer to pull directly from the database than to 
        // use the measurement schedule event, since we may have 
        // missed a prior measurement schedule event.
        List derivedMeas = getMeasurementsCollecting();
        
        // Find resources that are just starting to collect and create 
        // a resource metric tracker for those resources.
        for (Iterator iter = derivedMeas.iterator(); iter.hasNext();) {
            DerivedMeasurement meas = (DerivedMeasurement) iter.next();
            int mid = meas.getId().intValue();
            MeasurementZeventSource srcId = new MeasurementZeventSource(mid);
            
            if (!_srcId2CollectionInterval.containsKey(srcId)) {
                _log.debug("Start tracking newly scheduled measurement " +
                           "for trigger ["+getTriggerNameWithPartitionDesc()+
                           "]: "+srcId);
                
                getResourceTrackerAddIfNecessary(srcId);    
            }
        }
        
        // Now we can rebuild the collection interval map and reset the 
        // max collection interval.
        _srcId2CollectionInterval.clear();
        
        long oldInterval = _maxCollectionInterval;
        
        _maxCollectionInterval = MIN_COLLECTION_INTERVAL;
        
        for (Iterator iter = derivedMeas.iterator(); iter.hasNext();) {
            DerivedMeasurement meas = (DerivedMeasurement) iter.next();
            int mid = meas.getId().intValue();
            Long interval = new Long(meas.getInterval());
            _srcId2CollectionInterval.put(
                    new MeasurementZeventSource(mid), interval);
            _maxCollectionInterval = 
                Math.max(_maxCollectionInterval, meas.getInterval());
        }
        
        _log.debug("Trigger ["+getTriggerNameWithPartitionDesc()+
                   "] processed measurement schedule zevent: old collection interval="+
                   oldInterval+"; new collection interval="+_maxCollectionInterval);
    }

    /**
     * @return <code>true</code> if we are still in the first time window.
     */
    private boolean isInFirstTimeWindow() {
        if (_isWithinFirstTimeWindow) {
            if (System.currentTimeMillis() <
                _startOfTimeWindowExact+(2*_maxCollectionInterval)+
                AGENT_SERVER_TIME_SKEW_TOLERANCE) {
                // still in first time window
                _isWithinFirstTimeWindow = true;
            } else {
                _isWithinFirstTimeWindow = false;
            }   
        }
        
        return _isWithinFirstTimeWindow;
    }
    
    /**
     * If we are in the first time window, then the start time doesn't change 
     * from the previous value. Otherwise, the start of the time window is the 
     * timestamp of the oldest tracked heart beat (or the current time if no 
     * heart beats are currently being tracked).
     * 
     * @param inFirstTimeWindow <code>true</code> if we are in the first time window.
     */
    private void evaluateNextStartOfTimeWindow(boolean inFirstTimeWindow) {
        boolean debug = _log.isDebugEnabled();
        
        if (inFirstTimeWindow) {
            // don't change start of time window yet - use the old value
        } else {
            if (_trackedHeartBeatTimestamps.isEmpty() || 
                _trackedHeartBeatTimestamps.first() == null) {

                // The next start of time window is unknown! It's as though we 
                // are in the very first time window.
                if (debug) {
                    _log.debug("The next start of time window is unknown! " +
                               "Resetting time window for trigger ["+
                               getTriggerNameWithPartitionDesc()+"]");                        
                }
                
                setStartOfFirstTimeWindow(System.currentTimeMillis());                
            } else {
                Long timestamp = (Long)_trackedHeartBeatTimestamps.first();
                _trackedHeartBeatTimestamps.remove(timestamp);
                
                // Make sure we don't move back in time!
                _startOfTimeWindow = 
                    Math.max(_startOfTimeWindow, timestamp.longValue());
            }
        }
        
        if (debug) {
            _log.debug("Start of time window for trigger ["+
                       getTriggerNameWithPartitionDesc()+"] ="+
                       _startOfTimeWindow+", in first time window="+
                       _isWithinFirstTimeWindow);               
        }
    }
    
    /**
     * When determining if we've moved beyond the first time window, use the 
     * exact time. When calculating the time window lower bound use the voodooed 
     * time, since the measurement and heart beat event timestamps are voodooed.
     * 
     * @param timestamp The timestamp.
     */
    private void setStartOfFirstTimeWindow(long timestamp) {
        _isWithinFirstTimeWindow = true;
        _startOfTimeWindowExact = timestamp;
        
        // Even if we are timing voodooing down, we don't want to move back 
        // in time. Use the max of the last start of time window and the 
        // new value. Timing voodoo down to the nearest 30 seconds since 
        // this is the finest resolution we have (for heart beat events).
        _startOfTimeWindow = Math.max(_startOfTimeWindow,
            TimingVoodoo.roundDownTime(timestamp, 
                    HeartBeatZevent.HEART_BEAT_INTERVAL_MILLIS));
    }
    
    /**
     * @return <code>true</code> if we should start evaluating the tracked metrics.
     */
    private boolean shouldStartEvaluatingMetrics() {
        // If we are in the first time window, then give the trigger through 
        // the end of that time window to track metric history without 
        // evaluating metrics. That way, we'll know if a resource that should 
        // be reporting isn't actually reporting.
        return _isWithinFirstTimeWindow == false;
    }
    
    /**
     * Is this measurement event older than the start of the time window?
     * 
     * @param event The measurement event.
     * @param startTime The start timestamp for the time window.
     * @return <code>true</code> if this is an old measurement event.
     */
    private boolean isOlderThanTimeWindowStartTime(MeasurementZevent event, 
                                                   long startTime) {
        MeasurementZeventPayload payload = 
            (MeasurementZeventPayload)event.getPayload();
        
        MetricValue val = payload.getValue();

        if (val.getTimestamp() < startTime) {
            if (_log.isDebugEnabled()) {
                _log.debug("Trigger ["+getTriggerNameWithPartitionDesc()+
                           "] is rejecting measurement event older than time window: "+
                           event+"; "+val.getTimestamp()+" < "+startTime);                    
            }

            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Is this heart beat event older than the start of the time window?
     * 
     * @param event The heart beat event.
     * @param startTime The start timestamp for the time window.
     * @return <code>true</code> if this is an old heart beat event.
     */
    private boolean isOlderThanTimeWindowStartTime(HeartBeatZevent event, 
                                                   long startTime) {
        if (event.getVoodooedTimestamp() < startTime) {
            if (_log.isDebugEnabled()) {
                _log.debug("Trigger ["+getTriggerNameWithPartitionDesc()+
                           "] is rejecting heart beat event older than time window: "+
                           event+"; "+event.getVoodooedTimestamp()+" < "+startTime);                    
            }

            return true;
        } else {
            return false;
        }        
    }
        
    /**
     * Track this heart beat event. Heart beats are used to move the time 
     * window forward since they are timestamped "reliably" every 30 minutes, 
     * less than the minimum measurement collection interval. Also, since 
     * heart beats share the same zevent queue as the measurement events, if 
     * they are processed slower by the trigger, the time window will move 
     * forward slower. In that respect, they serve to throttle the time 
     * window velocity in case the system is bogged down, minimizing the 
     * loss of measurement events that may spend a long time in the zevent 
     * queue.
     * 
     * @param event The heart beat event.
     */
    private void track(HeartBeatZevent event) {        
        _trackedHeartBeatTimestamps.add(new Long(event.getVoodooedTimestamp()));  
        
        if (_log.isDebugEnabled()) {
            _log.debug("Tracking heart beat for trigger ["+
                       getTriggerNameWithPartitionDesc()+"]: "+event);                 
        }
    }
    
    /**
     * Retrieve the resource metric tracker for a given resource, adding it 
     * to the map of tracked resources if it does not already exist there.
     * 
     * @param sourceId The measurement source id.
     * @return The resource tracker.
     */
    private ResourceMetricTracker getResourceTrackerAddIfNecessary(
                                        MeasurementZeventSource sourceId) {
        
        ResourceMetricTracker tracker = 
            (ResourceMetricTracker)_trackedResources.get(sourceId);
        
        if (tracker==null) {
            tracker = new ResourceMetricTracker(_comparator, 
                                                _metricVal, 
                                                _isNotReportingEventsOffending);
      
            _trackedResources.put(sourceId, tracker);                    
        }
        
        return tracker;
    }
        
    /**
     * Track this measurement event.
     * 
     * @param event The measurement event.
     */
    private void track(MeasurementZevent event) {
        MeasurementZeventSource sourceId = 
            (MeasurementZeventSource)event.getSourceId();
        
        ResourceMetricTracker tracker = getResourceTrackerAddIfNecessary(sourceId);
        
        MeasurementZeventPayload payload = 
            (MeasurementZeventPayload)event.getPayload();
        
        MetricValue val = payload.getValue();
        
        tracker.trackMetricValue(val);
        
        if (_log.isDebugEnabled()) {
            _log.debug("Tracking measurement for trigger ["+
                       getTriggerNameWithPartitionDesc()+
                       "]: "+event+", timestamp="+val.getTimestamp());                 
        }
    }
    
    /**
     * Evaluate the metrics within the time window and return the first found 
     * violating metric value for each of the tracked resources.
     * 
     * @param trackedResources The tracked resources.
     * @param startTime The start timestamp for the time window (inclusive).
     * @param endTime The end timestamp for the time window (inclusive).
     * @return The map of {@link MeasurementZeventSource}Ids to {@link MetricValue}s.
     */
    private Map evaluateResourceMetricsAndReturnViolators(Map trackedResources,
                                                          long startTime,
                                                          long endTime) {
        
        boolean debug = _log.isDebugEnabled();
        
        Map srcId2MetricValue = new HashMap();
        
        if (debug) {
            _log.debug("Checking for violating measurements for trigger ["+
                       getTriggerNameWithPartitionDesc()+"] with time window; start="+
                       startTime+", end="+endTime);                 
        }
        
        for (Iterator iter = trackedResources.entrySet().iterator(); iter.hasNext();) {      
            Map.Entry entry = (Map.Entry) iter.next();
            MeasurementZeventSource srcId = (MeasurementZeventSource)entry.getKey();
            ResourceMetricTracker tracker = (ResourceMetricTracker)entry.getValue();
            
            // Remove resources that are not scheduled to collect and don't 
            // have any remaining tracked metrics so we don't accidentally 
            // consider them violating the trigger conditions (if non reporting 
            // resources are considered violating).
            if (tracker.getNumberOfTrackedMetrics()==0 && 
                !_srcId2CollectionInterval.containsKey(srcId)) {
                if (debug) {
                    _log.debug("Stopped tracking unscheduled measurement for trigger ["+
                               getTriggerNameWithPartitionDesc()+"]: "+srcId);                        
                }
                
                iter.remove();
                continue;
            }

            MetricValue val = 
                tracker.searchForViolatingMetricInWindow(startTime, endTime);
            
            if (val != null) {
                srcId2MetricValue.put(srcId, val);
                
                if (debug) {
                    _log.debug("Found violating measurement for trigger ["+
                               getTriggerNameWithPartitionDesc()+"]: "+srcId+
                               ", "+val+", timestamp="+val.getTimestamp());                 
                }
            }
        } 
        
        return srcId2MetricValue;
    }
    
    /**
     * The alert fired time is the average timestamp for the current time 
     * window.
     * 
     * @param startTime The start timestamp for the time window (inclusive).
     * @param endTime The end timestamp for the time window (exclusive).
     * @return The alert fired time.
     */
    private long getAlertFiredTime(long startTime, long endTime) {
        return (endTime+startTime)/2L;
    }    
    
    private void tryToFire(Map srcId2MetricValue, long startTime, long endTime) { 
        if (_groupSize == 0) {
            if (_log.isDebugEnabled()) {
                _log.debug("Trigger ["+getTriggerNameWithPartitionDesc()+
                           "] has no resources in its group. Aborting " +
                           "trigger evaluation.");                
            }
            
            return;
        }
        
        int numMatched = 0;
        
        if (srcId2MetricValue.size()>0) {
            for (Iterator i=srcId2MetricValue.values().iterator(); i.hasNext(); ) {               
                MetricValue val = (MetricValue)i.next();
                
                // Offending resources always add towards the number matched
                if (val.equals(MetricValue.NONE) || 
                    _comparator.isTrue(new Float(val.getValue()), _metricVal)) { 
                    numMatched++;
                }
            }    
        }
                
        String leftHandStr, numMatchStr;
        int leftHand;
        
        if (_isPercent) { 
            // Shouldn't be larger than 100%
            leftHand = Math.min((numMatched * 100 / _groupSize), 100);
            numMatchStr = leftHand + "%";
            leftHandStr = _numResources + "%";
        } else {
            leftHand    = numMatched;
            numMatchStr = leftHand + "";
            leftHandStr = "" + _numResources;  
        }
        
        if (_log.isDebugEnabled()) {
            _log.debug("For trigger ["+getTriggerNameWithPartitionDesc()+
                       "] checking if "+_sizeCompare+" "+_numResources+
                       (_isPercent ? "%" : "")+" of the resources reported "+
                       _metricName+" "+_comparator+" "+_metricVal);
            _log.debug("Number of resources matching condition="+
                       numMatched+", group size="+_groupSize);            
        }
        
        if (!_sizeCompare.isTrue(leftHand, _numResources)) {
            setNotFired();
            return;
        }
        
        StringBuffer sr = new StringBuffer();
        StringBuffer lr = new StringBuffer();
            
        sr.append(_sizeCompare)
            .append(" ")
            .append(leftHandStr)
            .append(" of the resources reported ")
            .append(_metricName)
            .append(" ")
            .append(_comparator)
            .append(" ")
            .append(_metricVal);
            
        lr.append(_sizeCompare)
            .append(" ")
            .append(leftHandStr)
            .append(" of the resources (")
            .append(numMatchStr)
            .append(") reported ")
            .append(_metricName)
            .append(" ")
            .append(_comparator)
            .append(" ")
            .append(_metricVal);
        
        long nonReportingResourceFiredTime = getAlertFiredTime(startTime, endTime);

        setFired(new FireReason(sr.toString(), lr.toString(), 
            formulateAuxLogs(srcId2MetricValue, nonReportingResourceFiredTime)));
    }

    private List formulateAuxLogs(Map srcId2MetricValue, 
                                 long nonReportingResourceFiredTime) {
        // Assemble the aux info
        List auxLogs = new ArrayList();
        DerivedMeasurementManagerLocal dmMan = getDMMan();
        AuthzSubjectValue overlord = 
            AuthzSubjectManagerEJBImpl.getOne().findOverlord();
        for (Iterator i=srcId2MetricValue.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            MeasurementZeventSource src = (MeasurementZeventSource)ent.getKey();
            MetricValue val = (MetricValue)ent.getValue();
            SimpleAlertAuxLog baseLog; 
            AppdefEntityValue entVal;
            AppdefEntityID entId;
            
            // We know that offending resources violate the conditions.
            // No need to do the comparison in this case.
            if (!val.equals(MetricValue.NONE)) {
                if (!_comparator.isTrue(new Float(val.getValue()), _metricVal)) 
                    continue;                
            }
            
            DerivedMeasurement metric = 
                dmMan.getMeasurement(new Integer(src.getId()));
            
            if (metric == null) {
                // HQ-1117: The resource has already been deleted
                // Don't consider this resource
                continue;
            }
            
            entId  = new AppdefEntityID(metric.getAppdefType(), 
                                        metric.getInstanceId());
            entVal = new AppdefEntityValue(entId, overlord);
            
            String entName;
           
            try {
                entName = entVal.getName();
            } catch(Exception e) {
                entName = entId.toString();
            }
            
            String metricName = metric.getTemplate().getName();
            String descrNoVal = entName+" reported "+metricName+" = ";

            String descr;
            long timestamp;
            
            // Set the metric description and timestamp in the aux log.
            if (val.equals(MetricValue.NONE)) {
                descr = descrNoVal+"Unknown";
                timestamp = nonReportingResourceFiredTime;
            } else {
                descr = descrNoVal+val.getValue();
                timestamp = val.getTimestamp();
            }
            
            baseLog = new SimpleAlertAuxLog(descr, timestamp);                
            baseLog.addChild(new MetricAuxLog(metricName+" chart", timestamp, metric));                
            baseLog.addChild(new ResourceAuxLog(entName, timestamp, entId));
            
            auxLogs.add(baseLog);
        }
        
        return auxLogs;
    }
    
    private DerivedMeasurementManagerLocal getDMMan() {
        return DerivedMeasurementManagerEJBImpl.getOne();
    }
    
    private ResourceGroupManagerLocal getRGMan() {
        return ResourceGroupManagerEJBImpl.getOne();
    }
    
    public void setGroup(ResourceGroup rg) {
        _resourceGroup = rg;
        
        _interestedEvents.clear();
        try {
            // Not sure this is the best way to do this.  It essentially ties us
            // to only having appdef groups come in here.
            AppdefGroupManagerLocal gMan = 
                AppdefGroupManagerUtil.getLocalHome().create();
            AuthzSubjectManagerLocal sMan = 
                AuthzSubjectManagerUtil.getLocalHome().create();
            TemplateManagerLocal tMan = 
                TemplateManagerUtil.getLocalHome().create();
            
            AppdefGroupValue g = gMan.findGroup(sMan.getOverlord(), rg.getId());
            
            _groupSize = g.getTotalSize();
            
            _log.debug("Resource group set: id="+rg.getId()+", size="+_groupSize);
            
            List derivedMeas = getMeasurementsCollecting();
            
            _maxCollectionInterval = MIN_COLLECTION_INTERVAL;
            
            for (Iterator iter = derivedMeas.iterator(); iter.hasNext();) {
                DerivedMeasurement meas = (DerivedMeasurement) iter.next();
                int mid = meas.getId().intValue();
                Long interval = new Long(meas.getInterval());
                
                _maxCollectionInterval = 
                    Math.max(_maxCollectionInterval, meas.getInterval());
                
                MeasurementZeventSource srcId = new MeasurementZeventSource(mid);
                _interestedEvents.add(srcId);
                _srcId2CollectionInterval.put(srcId, interval);
                
                // HQ-1165: Create a resource metric tracker for this resource 
                // right now in case we have decided that not reporting resources 
                // are offending and a metric is never reported for that resource. 
                // We don't want to depend on receiving at least one metric for 
                // that resource before we can determine that it isn't reporting.
                getResourceTrackerAddIfNecessary(srcId);
                
                MeasurementScheduleZeventSource scheduleSrcId = 
                    new MeasurementScheduleZeventSource(mid);
                _interestedEvents.add(scheduleSrcId);
            }
            
            _interestedEvents.add(HeartBeatZeventSource.getInstance());
            
            if (derivedMeas.size() != _groupSize) {
                _log.warn("Listening to different # measurement events ("+
                           derivedMeas.size()+") than resources ("+
                          _groupSize+"). This probably means that not " +
                          "everyone in the group is monitoring something");
            }
            
            _metricName = tMan.getTemplate(_templateId).getName();
            
            setTriggerName();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Get the measurements collecting for each resource in the group.
     * 
     * @return The list of {@link DerivedMeasurement} objects.
     */
    private List getMeasurementsCollecting() {
        return getRGMan().getMetricsCollecting(_resourceGroup, _templateId);
    }
    
    private String getTriggerNameWithPartitionDesc() {
        if (_partitionDescription == null) {
            ExecutionStrategy strategy = getStrategy();
            
            if (strategy != null) {
                if (strategy.getPartition() != null) {
                    _partitionDescription = 
                        strategy.getPartition().getDescription();
                }
            }            
        }
        
        if (_partitionDescription != null) {
            return _triggerName+" "+_partitionDescription;
        } else {
            return _triggerName;            
        }
    }
    
    private void setTriggerName() {
        _triggerName = getAlertDefName()+" ["+_metricName+" "+_comparator+" "+_metricVal+"]";
    }
    
}
