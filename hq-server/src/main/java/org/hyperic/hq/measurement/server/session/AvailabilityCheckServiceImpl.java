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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This job is responsible for filling in missing availabilty metric values.
 */
@Service("availabilityCheckService")
public class AvailabilityCheckServiceImpl implements AvailabilityCheckService {
    private final Log log = LogFactory.getLog(AvailabilityCheckServiceImpl.class);
    private static final String AVAIL_BACKFILLER_TIME = ConcurrentStatsCollector.AVAIL_BACKFILLER_TIME;
    private static final String AVAIL_BACKFILLER_NUMPLATFORMS = ConcurrentStatsCollector.AVAIL_BACKFILLER_NUMPLATFORMS;

    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    private final Object IS_RUNNING_LOCK = new Object();
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private boolean isRunning = false;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private AvailabilityManager availabilityManager;
    private AvailabilityCache availabilityCache;
    private BackfillPointsService backfillPointsService;

    @Autowired
    public AvailabilityCheckServiceImpl(ConcurrentStatsCollector concurrentStatsCollector,
                                        AvailabilityManager availabilityManager,
                                        AvailabilityCache availabilityCache,
                                        BackfillPointsService backfillPointsService) {
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.availabilityCache = availabilityCache;
        this.availabilityManager = availabilityManager;
        this.backfillPointsService = backfillPointsService;
    }

    @PostConstruct
    public void initStats() {
        concurrentStatsCollector.register(AVAIL_BACKFILLER_TIME);
        concurrentStatsCollector.register(AVAIL_BACKFILLER_NUMPLATFORMS);
    }

    public void backfill() {
        backfill(System.currentTimeMillis(), false);
    }

    public void backfill(long timeInMillis) {
        // since method is used for unittests no need to check if alert triggers
        // have initialized
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
                    backfillPoints = backfillPointsService.getBackfillPoints(current);
                    backfilledPts = backfillPoints.keySet().size();
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
            concurrentStatsCollector.addStat(now() - start, AVAIL_BACKFILLER_TIME);
            if (backfilledPts != -1) {
                concurrentStatsCollector.addStat(backfilledPts, AVAIL_BACKFILLER_NUMPLATFORMS);
            }
        }
    }

    /**
     * Since this method is called from the synchronized block in backfill()
     * please see the associated NOTE.
     */
    private void backfillAvails(List<DataPoint> backfillList) {
        final boolean debug = log.isDebugEnabled();
        final int batchSize = 500;
        for (int i = 0; i < backfillList.size(); i += batchSize) {
            if (debug) {
                log.debug("backfilling " + batchSize + " datapoints, " + (backfillList.size() - i) +
                          " remaining");
            }
            int end = Math.min(i + batchSize, backfillList.size());
            // use this method signature to not send data to event handlers from
            // here.
            // send it outside the synchronized cache block from the calling
            // method
            availabilityManager.addData(backfillList.subList(i, end), false);
        }
    }

    private long now() {
        return System.currentTimeMillis();
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
