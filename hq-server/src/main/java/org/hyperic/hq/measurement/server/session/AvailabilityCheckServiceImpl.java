/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This job is responsible for filling in missing availabilty metric values.
 */
@Service("availabilityCheckService")
public class AvailabilityCheckServiceImpl implements AvailabilityCheckService {
    private final Log log = LogFactory.getLog(AvailabilityCheckServiceImpl.class);
    private static final double AVAIL_DOWN   = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_PAUSED = MeasurementConstants.AVAIL_PAUSED;
    private static final double AVAIL_NULL   = MeasurementConstants.AVAIL_NULL;
    private static final String AVAIL_BACKFILLER_TIME =
        ConcurrentStatsCollector.AVAIL_BACKFILLER_TIME;
    private static final String AVAIL_BACKFILLER_NUMPLATFORMS =
        ConcurrentStatsCollector.AVAIL_BACKFILLER_NUMPLATFORMS;

    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    private final Object IS_RUNNING_LOCK = new Object();
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private boolean isRunning = false;
    private AvailabilityManager availabilityManager;
    private PermissionManager permissionManager;
    private AvailabilityCache availabilityCache;
    private ConcurrentStatsCollector concurrentStatsCollector;
  

    @Autowired
    public AvailabilityCheckServiceImpl(AvailabilityManager availabilityManager,
                                        PermissionManager permissionManager,
                                        ConcurrentStatsCollector concurrentStatsCollector,
                                        AvailabilityCache availabilityCache) {
        this.availabilityManager = availabilityManager;
        this.permissionManager = permissionManager;
        this.availabilityCache = availabilityCache;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void initStats() {
        concurrentStatsCollector.register(AVAIL_BACKFILLER_TIME);
        concurrentStatsCollector.register(AVAIL_BACKFILLER_NUMPLATFORMS);
    }

    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
        return TimingVoodoo.roundDownTime((current - meas.getInterval()), meas.getInterval());
    }

    private long getBeginWindow(long end, Measurement meas) {
        final long interval = 0;
        final long wait = 5 * MeasurementConstants.MINUTE;
        long measInterval = meas.getInterval();

        // We have to get at least the measurement interval
        long maxInterval = Math.max(Math.max(interval, wait), measInterval);

        // Begin is maximum of interval or measurement create time
        long begin = Math.max(end - maxInterval, meas.getMtime() + measInterval);
        return TimingVoodoo.roundDownTime(begin, measInterval);
    }

    /**
     * Since this method is called from the synchronized block in hitInSession()
     * please see the associated NOTE.
     * @return {@link Map} of {@link Integer} to {@link ResourceDataPoint},
     *         Integer -> resource id
     */
    private Map<Integer, ResourceDataPoint> getDownPlatforms(long timeInMillis) {
        final boolean debug = log.isDebugEnabled();
        final List<Measurement> platformResources = availabilityManager.getPlatformResources();
        final long now = TimingVoodoo.roundDownTime(timeInMillis, MeasurementConstants.MINUTE);
        final String nowTimestamp = TimeUtil.toString(now);
        final Map<Integer, ResourceDataPoint> rtn =
            new HashMap<Integer, ResourceDataPoint>(platformResources.size());
        Resource resource = null;
        synchronized (availabilityCache) {
            for (final Measurement meas : platformResources) {
                final long interval = meas.getInterval();
                final long end = getEndWindow(now, meas);
                final long begin = getBeginWindow(end, meas);
                final DataPoint defaultPt = new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                final DataPoint last = availabilityCache.get(meas.getId(), defaultPt);
                final long lastTimestamp = last.getTimestamp();
                if (debug) {
                    String msg = "Checking availability for " + last + ", CacheValue=(" +
                                 TimeUtil.toString(lastTimestamp) + ") vs. Now=(" + nowTimestamp + ")";
                    log.debug(msg);
                }
                if (begin > end) {
                    // this represents the scenario where the measurement mtime
                    // was modified recently and therefore we need to wait
                    // another interval
                    continue;
                }
                if (!meas.isEnabled()) {
                    final long t = TimingVoodoo.roundDownTime(now - interval, interval);
                    final DataPoint point =
                        new DataPoint(meas.getId(), new MetricValue(AVAIL_PAUSED, t));
                    resource = meas.getResource();
                    rtn.put(resource.getId(), new ResourceDataPoint(resource, point));
                } else if (last.getValue() == AVAIL_DOWN || (now - lastTimestamp) > interval * 2) {
                    // HQ-1664: This is a hack: Give a 5 minute grace period for
                    // the agent and HQ
                    // to sync up if a resource was recently part of a downtime
                    // window
                    if (last.getValue() == AVAIL_PAUSED && (now - lastTimestamp) <= 5 * 60 * 1000) {
                        continue;
                    }
                    long t = (last.getValue() != AVAIL_DOWN) ?
                        lastTimestamp+interval : TimingVoodoo.roundDownTime(now-interval, interval);
                    t = (last.getValue() == AVAIL_PAUSED) ?
                        TimingVoodoo.roundDownTime(now, interval) : t;
                    DataPoint point = new DataPoint(meas.getId(), new MetricValue(AVAIL_DOWN, t));
                    resource = meas.getResource();
                    rtn.put(resource.getId(), new ResourceDataPoint(resource, point));
                }
            }
        }
        if (!rtn.isEmpty()) {
            permissionManager.getHierarchicalAlertingManager().performSecondaryAvailabilityCheck(rtn);
        }
        return rtn;
    }

    @Transactional(readOnly=true)
    public void backfill() {
        backfill(System.currentTimeMillis(), false);
    }

    @Transactional(readOnly=true)
    public void backfill(long timeInMillis) {
        // since method is used for unittests no need to check if alert triggers have initialized
        backfill(timeInMillis, true);
    }

    private void backfill(long current, boolean forceStart) {
        long start = now();
        long backfilledPts = -1;
        try {
            final boolean debug = log.isDebugEnabled();
            if (debug) {
                Date lDate = new Date(current);
                log.debug("Availability Check Service started executing: " + lDate);
            }
            // Don't start backfilling immediately
            if (!forceStart && !canStart(current)) {
                log.info("not starting availability check");
                return;
            }
           
            synchronized (IS_RUNNING_LOCK) {
                if (isRunning) {
                    log.warn("Availability Check Service is already running, bailing out");
                    return;
                } else {
                    isRunning = true;
                }
            }
            try {
                // PLEASE NOTE: This synchronized block directly affects the
                // throughput of the availability metrics, while this lock is
                // active no availability metric will be inserted. This is to
                // ensure the backfilled points will not be inserted after the
                // associated AVAIL_UP value from the agent.
                // The code must be extremely efficient or else it will have
                // a big impact on the performance of availability insertion.
                Map<Integer, DataPoint> backfillPoints = null;
                synchronized (availabilityCache) {
                    Map<Integer, ResourceDataPoint> downPlatforms = getDownPlatforms(current);
                    backfilledPts = downPlatforms.size();
                    backfillPoints = getBackfillPts(downPlatforms, current);
                    backfillAvails(new ArrayList<DataPoint>(backfillPoints.values()));
                }
                // send data to event handlers outside of synchronized block
                availabilityManager.sendDataToEventHandlers(backfillPoints);
            } finally {
                synchronized (IS_RUNNING_LOCK) {
                    isRunning = false;
                }
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            concurrentStatsCollector.addStat(now()-start, AVAIL_BACKFILLER_TIME);
            if (backfilledPts != -1) {
                concurrentStatsCollector.addStat(backfilledPts, AVAIL_BACKFILLER_NUMPLATFORMS);
            }
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * Since this method is called from the synchronized block in hitInSession()
     * please see the associated NOTE.
     */
    private void backfillAvails(List<DataPoint> backfillList) {
        final boolean debug = log.isDebugEnabled();
        final int batchSize = 500;
        for (int i=0; i<backfillList.size(); i+=batchSize) {
            if (debug) {
                log.debug("backfilling " + batchSize +
                          " datapoints, " + (backfillList.size() - i) + " remaining");
            }
            int end = Math.min(i + batchSize, backfillList.size());
            // use this method signature to not send data to event handlers from here.
            // send it outside the synchronized cache block from the calling method
            availabilityManager.addData(backfillList.subList(i, end), false);
        }
    }

    private Map<Integer, DataPoint> getBackfillPts(Map<Integer, ResourceDataPoint> downPlatforms,
                                                   long current) {
        final boolean debug = log.isDebugEnabled();
        final Map<Integer, DataPoint> rtn = new HashMap<Integer, DataPoint>();
        final List<Integer> resourceIds = new ArrayList<Integer>(downPlatforms.keySet());
        final Map<Integer, List<Measurement>> rHierarchy =
            availabilityManager.getAvailMeasurementChildren(
                resourceIds, AuthzConstants.ResourceEdgeContainmentRelation);
        for (ResourceDataPoint rdp : downPlatforms.values()) {
            final Resource platform = rdp.getResource();
            if (debug) {
                log.debug(new StringBuilder(256)
                    .append("platform name=").append(platform.getName())
                    .append(", resourceid=").append(platform.getId())
                    .append(", measurementid=").append(rdp.getMetricId())
                    .append(" is being marked ").append(rdp.getValue())
                    .append(" with timestamp = ").append(TimeUtil.toString(rdp.getTimestamp()))
                    .toString());
            }
            rtn.put(platform.getId(), rdp);
            if (rdp.getValue() != AVAIL_DOWN) {
                // platform may be paused, so skip pausing its children
                continue;
            }
            final List<Measurement> associatedResources = rHierarchy.get(platform.getId());
            if (associatedResources == null) {
                continue;
            }
            if (debug) {
                log.debug("platform [resource id " + platform.getId() +
                          "] has " + associatedResources.size() + " associated resources");
            }
            for (Measurement meas : associatedResources) {
                if (!meas.isEnabled()) {
                    continue;
                }
                final long end = getEndWindow(current, meas);
                final DataPoint defaultPt = new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                final DataPoint lastPt = availabilityCache.get(meas.getId(), defaultPt);
                final long backfillTime = lastPt.getTimestamp() + meas.getInterval();
                if (backfillTime > current) {
                    continue;
                }
                if (debug) {
                    log.debug("measurement id " + meas.getId() +
                              " is being marked down, time=" + backfillTime);
                }
                final MetricValue val = new MetricValue(AVAIL_DOWN, backfillTime);
                final MeasDataPoint point =
                    new MeasDataPoint(meas.getId(), val, meas.getTemplate().isAvailability());
                rtn.put(meas.getResource().getId(), point);
            }
        }
        return rtn;
    }

    private boolean canStart(long now) {
        if (!hasStarted.get()) {
            if (startTime == 0) {
                startTime = now;
            } else if ((startTime + wait) <= now) {
                // start after the initial wait time only if the
                // availability inserter is not "backlogged"
                if (getAvailabilityInserterQueueSize() < 1000) {
                    hasStarted.set(true);
                }
            }
        }
        
        return hasStarted.get();
    }
    
    private long getAvailabilityInserterQueueSize() {
        boolean debug = log.isDebugEnabled();
        String statKey = "AvailabilityInserter_QUEUE_SIZE";
        long currentQueueSize = -1;

        StatCollector stat = (StatCollector) concurrentStatsCollector.getStatKeys().get(statKey);

        if (stat == null) {
            if (debug) {
                log.debug(statKey + " is not registered in the ConcurrentStatsCollector");
            }
        } else {
            try {
                currentQueueSize = stat.getVal();
            } catch (StatUnreachableException e) {
                if (debug) {
                    log.debug("Could not get value for " + statKey + ": " + e.getMessage(), e);
                }
            }
        }
        
        if (debug) {
            log.debug("Current availability inserter queue size = " + currentQueueSize);
        }

        return currentQueueSize;
    }
}
