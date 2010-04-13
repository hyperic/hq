/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.mbean;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SessionMBeanBase;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.AvailabilityCache;
import org.hyperic.hq.measurement.server.session.MeasDataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.ResourceDataPoint;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.notReady.NotReadyManager;

/**
 * This job is responsible for filling in missing availabilty metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AvailabilityCheck"
 */
public class AvailabilityCheckService
    extends SessionMBeanBase
    implements AvailabilityCheckServiceMBean
{
    private final Log _log = LogFactory.getLog(AvailabilityCheckService.class);
    private static final double AVAIL_DOWN   = MeasurementConstants.AVAIL_DOWN,
                                AVAIL_PAUSED = MeasurementConstants.AVAIL_PAUSED,
                                AVAIL_NULL   = MeasurementConstants.AVAIL_NULL;

    private long _interval = 0;
    private long _startTime = 0;
    private long _wait = 5 * MeasurementConstants.MINUTE;
    private final Object IS_RUNNING_LOCK = new Object();
    private boolean _isRunning = false;
    
    /**
     * This method is used mainly for the unittest from
     * AvailabilityManager_testEJBImpl.invokeBackfiller()
     * @jmx:managed-operation
     */
    public void hitWithDate(Date lDate) {
        if (!new NotReadyManager().isReady()) {
            _log.info("availability check service not starting until server is ready.");
            return;
        }
        super.hit(lDate);
    }

    /**
     * This method ignores the date which is passed in, the reason for this is
     * that we have seen instances where the JBoss Timer service produces an
     * invalid date which is in the past.  Since AvailabilityCheckService is
     * very time sensitive this is not acceptable.  Therefore we use
     * System.currentTimeMillis() and ignore the date which is passed in.
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        if (!new NotReadyManager().isReady()) {
            _log.info("availability check service not starting until server is ready.");
            return;
        }
        Date date = new Date(System.currentTimeMillis());
        super.hit(date);
    }
    
    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
	    return TimingVoodoo.roundDownTime(
	        (current-meas.getInterval()), meas.getInterval());
    }

    private long getBeginWindow(long end, Measurement meas) {
        final long interval = 0;
        final long wait = 5 * MeasurementConstants.MINUTE;
        long measInterval = meas.getInterval();

        // We have to get at least the measurement interval
        long maxInterval =
            Math.max(Math.max(interval, wait), measInterval);
    
        // Begin is maximum of mbean interval or measurement create time
        long begin = Math.max(end - maxInterval, meas.getMtime() + measInterval);
        return TimingVoodoo.roundDownTime(begin, measInterval);
    }

    /**
     * Since this method is called from the synchronized block in hitInSession()
     * please see the associated NOTE.
     * @return {@link Map} of {@link Integer} to {@link ResourceDataPoint}, Integer -> resource id
     */
    private Map getDownPlatforms(Date lDate) {
        final boolean debug = _log.isDebugEnabled();
        AvailabilityCache cache = AvailabilityCache.getInstance();
        AvailabilityManagerLocal availMan = AvailabilityManagerEJBImpl.getOne();
        List platformResources = availMan.getPlatformResources();
        final long now = TimingVoodoo.roundDownTime(
            lDate.getTime(), MeasurementConstants.MINUTE);
        final String nowTimestamp = TimeUtil.toString(now);
        Map rtn = new HashMap(platformResources.size());
        Resource resource = null;
        synchronized (cache) {
            for (Iterator i = platformResources.iterator(); i.hasNext();) {
                Measurement meas = (Measurement)i.next();
                long interval = meas.getInterval();
                long end = getEndWindow(now, meas);
                long begin = getBeginWindow(end, meas);
                DataPoint defaultPt =
                    new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                DataPoint last = cache.get(meas.getId(), defaultPt);
                long lastTimestamp = last.getTimestamp();
                if (debug) {
                    String msg = "Checking availability for " + last +
                        ", CacheValue=(" + TimeUtil.toString(lastTimestamp) +
                        ") vs. Now=(" + nowTimestamp + ")";
                    _log.debug(msg);
                }
                if (begin > end) {
                    // this represents the scenario where the measurement mtime
                    // was modified recently and therefore we need to wait
                    // another interval
                    continue;
                }
                if (!meas.isEnabled()) {
                    long t = TimingVoodoo.roundDownTime(now - interval, interval);
                    DataPoint point = new DataPoint(
                        meas.getId(), new MetricValue(AVAIL_PAUSED, t));
                    resource = meas.getResource();
                    rtn.put(resource.getId(),
                            new ResourceDataPoint(resource, point));
                } else if (last.getValue() == AVAIL_DOWN ||
                           (now - lastTimestamp) > interval*2) {
                    // HQ-1664: This is a hack: Give a 5 minute grace period for the agent and HQ
                    // to sync up if a resource was recently part of a downtime window
                    if (last.getValue() == AVAIL_PAUSED
                            && (now - lastTimestamp) <= 5*60*1000 ) {
                        continue;
                    }
                    long t = (last.getValue() != AVAIL_DOWN) ?
                        lastTimestamp + interval :
                        TimingVoodoo.roundDownTime(now - interval, interval);
                    t = (last.getValue() == AVAIL_PAUSED) ?
                        TimingVoodoo.roundDownTime(now, interval) : t;
                    DataPoint point = new DataPoint(
                        meas.getId(), new MetricValue(AVAIL_DOWN, t));
                    resource = meas.getResource();
                    rtn.put(resource.getId(),
                            new ResourceDataPoint(resource, point));                    
                }
            }
        }
        
        if (!rtn.isEmpty()) {
            PermissionManagerFactory.getInstance().getHierarchicalAlertingManager()
                .performSecondaryAvailabilityCheck(rtn);
        }

        return rtn;
    }

    protected void hitInSession(Date lDate) {
        final boolean debug = _log.isDebugEnabled();
        if (debug) {
            _log.debug("Availability Check Service started executing: "+lDate);            
        }
        long current = lDate.getTime();
        // Don't start backfilling immediately
        if (!canStart(current)) {
            _log.debug("not starting availability check");
            return;
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        synchronized (IS_RUNNING_LOCK) {
            if (_isRunning) {
                _log.warn("Availability Check Service is already running, " +
                    "bailing out");
                return;
            } else {
                _isRunning = true;
            }
        }
        try {
            // PLEASE NOTE: This synchronized block directly affects the
            // throughput of the availability metrics, while this lock is
            // active no availability metric will be inserted.  This is to
            // ensure the backfilled points will not be inserted after the
            // associated AVAIL_UP value from the agent.
            // The code must be extremely efficient or else it will have
            // a big impact on the performance of availability insertion.
            Map backfillPoints = null;
            synchronized (cache) {
                Map downPlatforms = getDownPlatforms(lDate);
                backfillPoints = getBackfillPts(downPlatforms, current);
                backfillAvails(new ArrayList(backfillPoints.values()));
            }
            // send data to event handlers outside of synchronized block
            AvailabilityManagerEJBImpl.getOne()
                .sendDataToEventHandlers(backfillPoints);
        } finally {
            synchronized (IS_RUNNING_LOCK) {
                _isRunning = false;
            }
        }
    }
    
    /**
     * Since this method is called from the synchronized block in hitInSession()
     * please see the associated NOTE.
     */
    private void backfillAvails(List backfillList) {
        final boolean debug = _log.isDebugEnabled();
        final AvailabilityManagerLocal availMan =
            AvailabilityManagerEJBImpl.getOne();
        final int batchSize = 500;
        for (int i=0; i<backfillList.size(); i+=batchSize) {
            if (debug) {
                _log.debug("backfilling " + batchSize + " datapoints, " +
                    (backfillList.size() - i) + " remaining");
            }
            int end = Math.min(i + batchSize, backfillList.size());
            // use this method signature to not send data to event handlers from here.
            // send it outside the synchronized cache block from the calling method
            availMan.addData(backfillList.subList(i, end), false);
        }
    }

    private Map getBackfillPts(Map downPlatforms, long current) {
        final boolean debug = _log.isDebugEnabled();
        final AvailabilityManagerLocal availMan =
            AvailabilityManagerEJBImpl.getOne();
        final AvailabilityCache cache = AvailabilityCache.getInstance();
        final Map rtn = new HashMap();
        final List resourceIds = new ArrayList(downPlatforms.keySet());
        final Map rHierarchy = availMan.getAvailMeasurementChildren(
            resourceIds, AuthzConstants.ResourceEdgeContainmentRelation);
        for (final Iterator i=downPlatforms.values().iterator(); i.hasNext(); ) {
            final ResourceDataPoint rdp = (ResourceDataPoint)i.next();
            final Resource platform = rdp.getResource();
            if (debug) {
                _log.debug("platform measurement id " + rdp.getMetricId() +
                           " is being marked " + rdp.getValue() +
                           " with timestamp = " +
                           TimeUtil.toString(rdp.getTimestamp()));
            }
            rtn.put(platform.getId(), rdp);
            if (rdp.getValue() != AVAIL_DOWN) {
                // platform may be paused, so skip pausing its children
                continue;
            }
            final List associatedResources = (List)rHierarchy.get(platform.getId());
            if (associatedResources == null) {
                continue;
            }
            if (debug) {
                _log.debug("platform [resource id " + platform.getId() + "] has " +
                    associatedResources.size() + " associated resources");
            }
            for (final Iterator j=associatedResources.iterator(); j.hasNext(); ) {
                final Measurement meas = (Measurement)j.next();
                if (!meas.isEnabled()) {
                    continue;
                }
                final long end = getEndWindow(current, meas);
                final DataPoint defaultPt =
                    new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                final DataPoint lastPt = cache.get(meas.getId(), defaultPt);
                final long backfillTime =
                    lastPt.getTimestamp() + meas.getInterval();
                if (backfillTime > current) {
                    continue;
                }
                if (debug) {
                    _log.debug("measurement id " + meas.getId() + " is " +
                               "being marked down, time=" + backfillTime);
                }
                final MetricValue val =
                    new MetricValue(AVAIL_DOWN, backfillTime);
                final MeasDataPoint point = new MeasDataPoint(
                    meas.getId(), val, meas.getTemplate().isAvailability());
                rtn.put(meas.getResource().getId(), point);
            }
        }
        return rtn;
    }

    private boolean canStart(long now) {
        if (_startTime == 0) {
            _startTime = now;
            return false;
        } else if ((_startTime + _wait) > now) {
            return false;
        }
        return true;
    }

    /**
     * Get the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public long getInterval() {
        return _interval;
    }

    /**
     * Set the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public void setInterval(long interval) {
        _interval = interval;
    }
    
    /**
     * Get the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public long getWait() {
        return _wait;
    }

    /**
     * Set the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public void setWait(long wait) {
        _wait = wait;
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void start() throws Exception {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        _log.info("Stopping " + this.getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}
}

