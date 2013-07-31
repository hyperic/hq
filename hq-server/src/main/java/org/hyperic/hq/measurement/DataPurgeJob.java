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

package org.hyperic.hq.measurement;

import java.beans.Introspector;
import java.util.Date;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.ha.HAUtil;
import org.hyperic.hq.measurement.shared.DataCompress;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dataPurgeJob")
public class DataPurgeJob implements Runnable {

    private ServerConfigManager serverConfigManager;
    private MeasurementManager measurementManager;
    private EventLogManager eventLogManager;
    private DataCompress dataCompress;
    private AlertManager alertManager;
    private DataManager dataManager;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private long _lastAnalyze = 0l;
    private static final long ANALYZE_INTERVAL = Integer.parseInt(System.getProperty(
        "data.purge.analyze.interval", "6")) * MeasurementConstants.HOUR;
    private static final boolean RUN_ANALYZE_METRIC_TABLES = Boolean.parseBoolean((System.getProperty(
        "data.purge.analyze.metricTables", "true")));
    private static final boolean RUN_ANALYZE_NON_METRIC_TABLES = Boolean.parseBoolean((System.getProperty(
        "data.purge.analyze.nonMetricTables", "true")));
    private final Object analyzeRunningLock = new Object();
    private boolean analyzeRunning = false;
    private final Object compressRunningLock = new Object();
    private boolean compressRunning = false;
    private long purgeRaw, purge1h, purge6h, purge1d, purgeAlert;
    private int purgeTopN;
    private final Log log = LogFactory.getLog(DataPurgeJob.class);

    @Autowired
    public DataPurgeJob(ServerConfigManager serverConfigManager,
                        MeasurementManager measurementManager, EventLogManager eventLogManager,
                        DataCompress dataCompress,
                        ConcurrentStatsCollector concurrentStatsCollector, AlertManager alertManager,
                        DataManager dataManager) {
        this.serverConfigManager = serverConfigManager;
        this.measurementManager = measurementManager;
        this.eventLogManager = eventLogManager;
        this.dataCompress = dataCompress;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.alertManager = alertManager;
        this.dataManager = dataManager;
    }

    @PostConstruct
    public void init() {
        initStatsCollector();
        loadPurgeDefaults();
        dataCompress.createMetricDataViews();
    }

    private void initStatsCollector() {
        concurrentStatsCollector.register(ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);
        concurrentStatsCollector.register(ConcurrentStatsCollector.DB_ANALYZE_TIME);
        concurrentStatsCollector.register(ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
        concurrentStatsCollector.register(ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
    }

    /**
     * Get the server purge configuration, loaded on startup.
     */
    private void loadPurgeDefaults() {
        this.log.info("Loading default purge intervals");
        Properties conf;
        try {
            conf = serverConfigManager.getConfig();
        } catch (ConfigPropertyException e) {
            // Not gonna happen
            throw new SystemException(e);
        }

        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        String purge1hString = conf.getProperty(HQConstants.DataPurge1Hour);
        String purge6hString = conf.getProperty(HQConstants.DataPurge6Hour);
        String purge1dString = conf.getProperty(HQConstants.DataPurge1Day);
        String purgeAlertString = conf.getProperty(HQConstants.AlertPurge);
        String purgeTopNString = conf.getProperty(HQConstants.TopNPurge);

        try {
            this.purgeRaw = Long.parseLong(purgeRawString);
            this.purge1h = Long.parseLong(purge1hString);
            this.purge6h = Long.parseLong(purge6hString);
            this.purge1d = Long.parseLong(purge1dString);
            this.purgeAlert = Long.parseLong(purgeAlertString);
            this.purgeTopN = Integer.parseInt(purgeTopNString);
        } catch (NumberFormatException e) {
            // Shouldn't happen unless manual edit of config table
            throw new IllegalArgumentException("Invalid purge interval: " + e, e);
        }
    }

    public synchronized void run() {
        try {
            if (!HAUtil.isMasterNode()) {
                log.warn("Data Purge invoked on the secondary HQ node.  " +
                         "It should not be run if the node is not master.  Not running");
                return;
            }
            compressData();
            Properties conf = null;
            try {
                conf = serverConfigManager.getConfig();
            } catch (Exception e) {
                throw new SystemException(e);
            }
            final long now = System.currentTimeMillis();
            // need to do this because of the Sun JVM bug
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5102804
            Introspector.flushCaches();
            if (conf != null) {
                purge(conf, now);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Entry point into compression routine
     */
    public void compressData() {

        // First check if we are already running
        synchronized (compressRunningLock) {
            if (compressRunning) {
                log.info("Not starting data compression. (Already running)");
                return;
            } else {
                compressRunning = true;
            }
        }

        final long time_start = now();

        try {
            // Announce
            log.info("Data compression starting at " + TimeUtil.toString(time_start));
            HQDialect dialect =
                (HQDialect) ((SessionFactoryImplementor) Bootstrap.getBean(SessionFactory.class)).getDialect();

            if (dialect.analyzeDb()) {
                runDBAnalyze();
                concurrentStatsCollector.addStat((now() - time_start), ConcurrentStatsCollector.DB_ANALYZE_TIME);
            }

            final long start = now();
            // Round down to the nearest hour.
            long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(),
                MeasurementConstants.HOUR);
            long last;
            // Compress hourly data
            last = compressData(MeasurementConstants.HOUR, now);
            // Purge, ensuring we don't purge data not yet compressed.
            truncateMeasurementData(Math.min(now - this.purgeRaw, last));
            // Purge metric problems as well
            purgeMetricProblems(Math.min(now - this.purgeRaw, last));

            // Compress 6 hour data
            last = compressData(MeasurementConstants.SIX_HOUR, now);

            // Purge, ensuring we don't purge data not yet compressed.
            purgeMeasurements(MeasurementConstants.HOUR, Math.min(start - this.purge1h, last));

            // Compress daily data
            last = compressData(MeasurementConstants.DAY, now);
            // Purge, ensuring we don't purge data not yet compressed.
            purgeMeasurements(MeasurementConstants.SIX_HOUR, Math.min(now - this.purge6h, last));

            // Purge, we never store more than 1 year of data.
            purgeMeasurements(MeasurementConstants.DAY, now - this.purge1d);

            purgeAlerts(now);

            purgeTopNData(now);

            concurrentStatsCollector.addStat((now() - start), ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);

        } finally {
            synchronized (compressRunningLock) {
                compressRunning = false;
            }
        }

        long time_end = System.currentTimeMillis();
        log.info("Data compression completed in " + ((time_end - time_start) / 1000) + " seconds.");
        runDBMaintenance();
    }

    private void purgeAlerts(long now) {
        log.info("Purging alerts older than " + TimeUtil.toString(now - this.purgeAlert));
        int alertsDeleted = -1;
        int totalDeleted = 0;
        // HQ-2731 - want to batch this 10,000 rows at a time
        // this avoids the session getting too large and will (hopefully) avoid transaction timeouts
        int maxBatch = 10000;
        do {
            alertsDeleted = alertManager.deleteAlerts(now - this.purgeAlert, maxBatch);
            totalDeleted += alertsDeleted;
        } while (alertsDeleted >= maxBatch);
        log.info("Done (Deleted " + totalDeleted + " alerts)");
    }

    private void purgeTopNData(long now) {
        Date timeToKeep = DateUtils.addDays(new Date(now), -this.purgeTopN);
        log.info("Purging TopNData older than " + timeToKeep);
        int topNDeleted = dataManager.purgeTopNData(timeToKeep);
        log.info("Done (Deleted " + topNDeleted + " TopNData rows)");
    }

    long compressData(long toInterval, long now) {
        long begin = dataCompress.getCompressionStartTime(toInterval, now);
        if (begin == 0) {
            //No data to compress
            return 0;
        }
        // Compress all the way up to now.
        StopWatch watch = new StopWatch();
        //begin=1:00:00 AM
        //toInterval = 1 hour
        //now=2:00:00 AM
        //We add toInterval due to bug HQ-4497, 6 hours and 1 day aggregations were wrong
        //begin + toInterval <= now (2:00:00<=2:00:00) otherwise it will enter the while one hour later
        while (begin + toInterval <= now) {
            long end = begin + toInterval;
            log.info("Compression interval: " + TimeUtil.toString(begin) + " to " +
                     TimeUtil.toString(end));
            try {
                dataCompress.compressData(toInterval, now, begin, end);
            } catch (Exception e) {
                // Just log the error and continue
                log.debug("Exception when inserting data " + " at " + TimeUtil.toString(begin), e);
            }
            // Increment for next iteration.
            begin = end;
        }
        log.info("Done (" + (watch.getElapsed() / 1000) + " seconds)");
        // Return the last interval that was compressed.
        return begin;
    }
    
    void truncateMeasurementData(long truncateBefore) {
        dataCompress.truncateMeasurementData(truncateBefore);
    }

    private final long now() {
        return System.currentTimeMillis();
    }

    private void runDBAnalyze() {
        if (log.isDebugEnabled()) {
            log.debug("Run analyze metric tables: " + RUN_ANALYZE_METRIC_TABLES);
            log.debug("Run analyze non metric tables: " + RUN_ANALYZE_NON_METRIC_TABLES);
            log.debug("Analyze interval: " + ANALYZE_INTERVAL + " ms");
        }
        // First check if we are already running
        long analyzeStart = System.currentTimeMillis();
        synchronized (analyzeRunningLock) {
            if (analyzeRunning) {
                log.info("Not starting db analyze. (Already running)");
                return;
            } else if (RUN_ANALYZE_METRIC_TABLES &&
                       (_lastAnalyze + ANALYZE_INTERVAL) > analyzeStart) {
                serverConfigManager.analyzeHqMetricTables(false);
                log.info("Only running analyze on current metric data table " +
                         "since last full run was at " + TimeUtil.toString(_lastAnalyze));
                return;
            } else {
                analyzeRunning = true;
            }
        }
        try {
            if (RUN_ANALYZE_METRIC_TABLES) {
                log.info("Performing database analyze on metric tables.");
                // Analyze the current and previous hq_metric_data table
                serverConfigManager.analyzeHqMetricTables(true);
            }
            if (RUN_ANALYZE_NON_METRIC_TABLES) {
                log.info("Performing database analyze on non metric tables.");
                // Analyze all non-metric tables
                serverConfigManager.analyzeNonMetricTables();
            }
            long secs = (System.currentTimeMillis() - analyzeStart) / 1000;
            log.info("Completed database analyze " + secs + " secs");
        } finally {
            synchronized (analyzeRunningLock) {
                analyzeRunning = false;
                _lastAnalyze = analyzeStart;
            }
        }
    }

    private void runDBMaintenance() {
        // Once compression finishes, we check to see if database maintenance
        // should be performed. This is defaulted to 1 hour, so it should
        // always run unless changed by the user.

        long time_start = System.currentTimeMillis();
        Properties conf;

        try {
            conf = serverConfigManager.getConfig();
        } catch (Exception e) {
            throw new SystemException(e);
        }

        String dataMaintenance = conf.getProperty(HQConstants.DataMaintenance);
        if (dataMaintenance == null) {
            // Should never happen
            log.error("No data maintenance interval found");
            return;
        }

        long maintInterval = Long.parseLong(dataMaintenance);
        if (maintInterval <= 0) {
            log.error("Maintenance interval was specified as [" + dataMaintenance +
                      "] -- which is invalid");
            return;
        }

    }

    protected void purge(Properties conf, long now) {
        long start = now();
        purgeEventLogs(conf, now);
        concurrentStatsCollector.addStat((now() - start),
            ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
        start = now();
        purgeMeasurements();
        concurrentStatsCollector.addStat((now() - start),
            ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
    }

    /**
     * Remove measurements no longer associated with a resource.
     */
    private void purgeMeasurements() {
        final long start = System.currentTimeMillis();
        int total = 0;
        try {
            final int batchSize = 10000;
            int dcount = Integer.MAX_VALUE;
            while (dcount > 0) {
                dcount = measurementManager.removeOrphanedMeasurements(batchSize);
                total += dcount;
            }
        } catch (Exception e) {
            // Do not allow errors to cause other maintenance functions to not run.
            log.error("Error removing measurements: " + e, e);
        }
        final long finish = System.currentTimeMillis();
        log.info("Removed " + total + " measurements in " + ((finish - start) / 1000) + " seconds.");
    }

    void purgeMeasurements(long dataInterval, long purgeAfter) {
        long min = dataCompress.getMinTimestamp(dataInterval);
        // No data
        if (min != 0) {
            long interval = MeasurementConstants.HOUR;
            long endWindow = purgeAfter;
            long startWindow = endWindow - interval;
            // while oldest timestamp in DB is older than purgeAfter, delete
            // batches in one hour increments
            while (endWindow > min) {
                dataCompress.purgeMeasurements(dataInterval, startWindow, endWindow);
                endWindow -= interval;
                startWindow -= interval;
            }
        }
    }

    void purgeMetricProblems(long purgeAfter) {
        long min = dataCompress.getMetricProblemMinTimestamp();
        // No data
        if (min != 0) {
            long interval = MeasurementConstants.HOUR;
            long endWindow = purgeAfter;
            long startWindow = endWindow - interval;
            // while oldest timestamp in DB is older than purgeAfter, delete
            // batches in one hour increments
            while (endWindow > min) {
                dataCompress.purgeMetricProblems(startWindow, endWindow);
                endWindow -= interval;
                startWindow -= interval;
            }
        }
    }

    /**
     * Purge Event Log data
     */
    private void purgeEventLogs(Properties conf, long now) {
        String purgeEventString = conf.getProperty(HQConstants.EventLogPurge);
        long purgeEventLog = Long.parseLong(purgeEventString);

        // Purge event logs

        log.info("Purging event logs older than " + TimeUtil.toString(now - purgeEventLog));
        try {
            int rowsDeleted = eventLogManager.deleteLogs(-1, now - purgeEventLog);
            log.info("Done (Deleted " + rowsDeleted + " event logs)");
        } catch (Exception e) {
            log.error("Unable to delete event logs: " + e.getMessage(), e);
        }
    }
}
