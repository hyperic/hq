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
    private static final Log _log = 
        LogFactory.getLog(MeasurementGtrigger.class);
    
    private static final int ONE_MINUTE = 60*1000;
    
    /**
     * The minimum assumed measurement collection interval. This 
     * value doesn't have to be exact since it's only used to 
     * estimate the time window if we have the case where none 
     * of the resources in the group are collecting on the metric.
     */
    private static final int MIN_COLLECTION_INTERVAL=ONE_MINUTE;
    
    private final SizeComparator     _sizeCompare;
    private final int                _numResources; // Num needed to match
    private final boolean            _isPercent;
    private final Integer            _templateId;
    private final ComparisonOperator _comparator;
    private final Float              _metricVal;
    private final Set                _interestedEvents;
    private final Map                _trackedResources;
    private final boolean            _isNotReportingEventsOffending;
    private final TreeSet            _nextStartOfTimeWindow;
    private       String             _triggerName;
    private       String             _partitionDescription;
    private       long               _maxCollectionInterval;
    private       boolean            _isWithinFirstTimeWindow;
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
        setStartOfFirstTimeWindow(System.currentTimeMillis());
        _nextStartOfTimeWindow = new TreeSet();
        _maxCollectionInterval = MIN_COLLECTION_INTERVAL;
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
        
        // Evaluate the sliding time window boundary.
        boolean inFirstTimeWindow = isInFirstTimeWindow();
        evaluateNextStartOfTimeWindow(inFirstTimeWindow);
        long endOfTimeWindow = _startOfTimeWindow+2*_maxCollectionInterval;
        
        // Track heart beat events
        if (isHeartBeatEvent(event)) {
            HeartBeatZevent hb = (HeartBeatZevent)event;
            
            long hbTimestamp = adjustHeartBeatTimestamp(hb);
            
            if (!isOlderThanTimeWindowStartTime(hb, _startOfTimeWindow, hbTimestamp)) {
                track(hb, hbTimestamp);                
            }
        }
        
        // Track the measurement events.
        if (isMeasurementEvent(event)) {         
            MeasurementZevent me = (MeasurementZevent)event;
            
            if (!isOlderThanTimeWindowStartTime(me, _startOfTimeWindow)) {
                track(me);       
            }
        }
        
        if (!shouldStartEvaluatingMetrics()) {
            return;
        }
        
        // Try to fire.
        Map srcId2ViolatingMetricValue = 
            evaluateResourceMetricsAndReturnViolators(_trackedResources, 
                                                      _startOfTimeWindow, 
                                                      endOfTimeWindow);
        
        if (srcId2ViolatingMetricValue.size() > 0) {
            // The alert fired time should be the end of the time window.
            tryToFire(srcId2ViolatingMetricValue, endOfTimeWindow);            
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

        _srcId2CollectionInterval.clear();
        
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
    }

    /**
     * @return <code>true</code> if we are still in the first time window.
     */
    private boolean isInFirstTimeWindow() {
        if (_isWithinFirstTimeWindow) {
            if (System.currentTimeMillis() <
                _startOfTimeWindowExact+2*_maxCollectionInterval) {
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
     * timestamp of the oldest tracked metric value (or the current time if no 
     * metrics are currently being tracked).
     * 
     * @param inFirstTimeWindow <code>true</code> if we are in the first time window.
     */
    private void evaluateNextStartOfTimeWindow(boolean inFirstTimeWindow) {
        boolean debug = _log.isDebugEnabled();
        
        if (inFirstTimeWindow) {
            // don't change start of time window yet - use the old value
        } else {
            if (_nextStartOfTimeWindow.isEmpty() || 
                _nextStartOfTimeWindow.first() == null) {

                // The next start of time window is unknown! It's as though we 
                // are in the very first time window.
                if (debug) {
                    _log.debug("The next start of time window is unknown! " +
                               "Resetting time window for trigger ["+
                               getTriggerNameWithPartitionDesc()+"]");                        
                }
                
                setStartOfFirstTimeWindow(System.currentTimeMillis());                
            } else {
                Long timestamp = (Long)_nextStartOfTimeWindow.first();
                _startOfTimeWindow = timestamp.longValue();
                _nextStartOfTimeWindow.remove(timestamp);
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
     * exact time. When calculating the time window bounds and whether or not 
     * an event is older than the bounds, use the voodooed time, since the 
     * measurement and heart beat event timestamps are voodooed.
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
            TimingVoodoo.roundDownTime(timestamp, ONE_MINUTE/2));
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
     * @param heartBeatTimestamp The heart beat timestamp after adjustment 
     *                           for the agent send interval and time voodooing.
     * @return <code>true</code> if this is an old heart beat event.
     */
    private boolean isOlderThanTimeWindowStartTime(HeartBeatZevent event, 
                                                   long startTime, 
                                                   long heartBeatTimestamp) {
        if (heartBeatTimestamp < startTime) {
            if (_log.isDebugEnabled()) {
                _log.debug("Trigger ["+getTriggerNameWithPartitionDesc()+
                           "] is rejecting heart beat event older than time window: "+
                           event+"; "+heartBeatTimestamp+" < "+startTime);                    
            }

            return true;
        } else {
            return false;
        }        
    }
    
    /**
     * Adjust the heart beat event timestamp so that it is "equivalent" to 
     * a measurement event timestamp. This includes subtracting 1 minute 
     * from the timestamp to adjust for the agent metric send interval of 
     * 1 minute and time voodooing that result down.
     * 
     * @param event The heart beat event.
     * @return The adjusted heart beat timestamp.
     */
    private long adjustHeartBeatTimestamp(HeartBeatZevent event) {
        long agentEquivalentTime = event.getTimestamp()-ONE_MINUTE;
        
        // the heart beat interval is every 1/2 minute.
        return TimingVoodoo.roundDownTime(agentEquivalentTime, ONE_MINUTE/2);
    }
    
    /**
     * Track this heart beat event. Heart beats are used to move the time 
     * window forward even when there are no processed measurement events.
     * 
     * @param event The heart beat event.
     * @param heartBeatTimestamp The heart beat timestamp after adjustment 
     *                           for the agent send interval and time voodooing.
     */
    private void track(HeartBeatZevent event, long heartBeatTimestamp) {        
        _nextStartOfTimeWindow.add(new Long(heartBeatTimestamp));  
        
        if (_log.isDebugEnabled()) {
            _log.debug("Tracking heart beat for trigger ["+
                       getTriggerNameWithPartitionDesc()+
                       "]: "+event+", timestamp="+heartBeatTimestamp);                 
        }
    }
    
    /**
     * Track this measurement event.
     * 
     * @param event The measurement event.
     */
    private void track(MeasurementZevent event) {        
        ResourceMetricTracker tracker = 
            (ResourceMetricTracker)_trackedResources.get(event.getSourceId());
                
        if (tracker == null) {
            tracker = 
                new ResourceMetricTracker(_comparator,
                                          _metricVal, 
                                          _isNotReportingEventsOffending);
            _trackedResources.put(event.getSourceId(), tracker);
        }
        
        MeasurementZeventPayload payload = 
            (MeasurementZeventPayload)event.getPayload();
        
        MetricValue val = payload.getValue();
        
        tracker.trackMetricValue(val);
        
        _nextStartOfTimeWindow.add(new Long(val.getTimestamp()));
        
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
     * @param endTime The end timestamp for the time window (exclusive).
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
    
    private void tryToFire(Map srcId2MetricValue, long firedTime) {                
        int numMatched = 0;
        
        for (Iterator i=srcId2MetricValue.values().iterator(); i.hasNext(); ) {
            MetricValue val = (MetricValue)i.next();
            
            // Offending resources always add towards the number matched
            if (val.equals(MetricValue.NONE) || 
                _comparator.isTrue(new Float(val.getValue()), _metricVal)) { 
                numMatched++;
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

        setFired(new FireReason(sr.toString(), lr.toString(), 
                    formulateAuxLogs(srcId2MetricValue, firedTime)));
    }

    private List formulateAuxLogs(Map srcId2MetricValue, long firedTime) {
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
            String descr, entName, metricName;
            
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
            try {
                entName = entVal.getName();
            } catch(Exception e) {
                entName = entId.toString();
            }
            metricName = metric.getTemplate().getName();
            descr = entName + " reported " + metric.getTemplate().getName() + 
                    " = " + val.getValue();
                      
            baseLog = new SimpleAlertAuxLog(descr, val.getTimestamp());
            
            if (val.equals(MetricValue.NONE)) {
                // Offending resources have no metric aux log.
                baseLog.addChild(new ResourceAuxLog(entName, firedTime, entId));
            } else {
                baseLog.addChild(new MetricAuxLog(metricName + " chart", 
                        val.getTimestamp(), metric));                
                baseLog.addChild(new ResourceAuxLog(entName, val.getTimestamp(), 
                        entId));
            }
            
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
            
            // reset the start of the first time window to now since we 
            // can't even start processing events until the group is set
            setStartOfFirstTimeWindow(System.currentTimeMillis());
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
