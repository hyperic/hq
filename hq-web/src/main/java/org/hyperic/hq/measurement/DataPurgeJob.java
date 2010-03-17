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
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.measurement.shared.DataCompress;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dataPurgeJob")
public class DataPurgeJob implements Runnable {

    private final Log _log = LogFactory.getLog(DataPurgeJob.class);

    static final long HOUR = MeasurementConstants.HOUR;
    static final long MINUTE = MeasurementConstants.MINUTE;
    private ServerConfigManager serverConfigManager;
    private MeasurementManager measurementManager;
    private EventLogManager eventLogManager;
    private DataCompress dataCompress;

    private long _lastAnalyze = 0l;
    private static final long ANALYZE_INTERVAL = 6 * HOUR;
    private final Object analyzeRunningLock = new Object();
    private boolean analyzeRunning = false;
    private final Object compressRunningLock = new Object();
    private boolean compressRunning = false;

    @Autowired
    public DataPurgeJob(ServerConfigManager serverConfigManager, MeasurementManager measurementManager,
                        EventLogManager eventLogManager, DataCompress dataCompress) {
        this.serverConfigManager = serverConfigManager;
        this.measurementManager = measurementManager;
        this.eventLogManager = eventLogManager;
        this.dataCompress = dataCompress;
    }
    
    @PostConstruct
    public void initStatsCollector() {
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.DB_ANALYZE_TIME);
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
    }

    public synchronized void run() {
        try {
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
            _log.error(e.getMessage(), e);
        }
    }

    /**
     * Entry point into compression routine
     */
    public void compressData() throws NamingException {

        // First check if we are already running
        synchronized (compressRunningLock) {
            if (compressRunning) {
                _log.info("Not starting data compression. (Already running)");
                return;
            } else {
                compressRunning = true;
            }
        }

        final long time_start = now();

        try {
            // Announce
            _log.info("Data compression starting at " + TimeUtil.toString(time_start));

            ConcurrentStatsCollector stats = ConcurrentStatsCollector.getInstance();
            runDBAnalyze();
            stats.addStat((now() - time_start), ConcurrentStatsCollector.DB_ANALYZE_TIME);

            final long start = now();
            dataCompress.compressData();
            stats.addStat((now() - start), ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);

        } catch (SQLException e) {
            _log.error("Unable to compress data: " + e, e);
        } finally {
            synchronized (compressRunningLock) {
                compressRunning = false;
            }
        }

        long time_end = System.currentTimeMillis();
        _log.info("Data compression completed in " + ((time_end - time_start) / 1000) + " seconds.");
        runDBMaintenance();
    }

    private final long now() {
        return System.currentTimeMillis();
    }

    private void runDBAnalyze() {
        // First check if we are already running
        long analyzeStart = System.currentTimeMillis();
        synchronized (analyzeRunningLock) {
            if (analyzeRunning) {
                _log.info("Not starting db analyze. (Already running)");
                return;
            } else if ((_lastAnalyze + ANALYZE_INTERVAL) > analyzeStart) {
                serverConfigManager.analyzeHqMetricTables(false);
                _log.info("Only running analyze on current metric data table " + "since last full run was at " +
                          TimeUtil.toString(_lastAnalyze));
                return;
            } else {
                analyzeRunning = true;
            }
        }
        try {
            _log.info("Performing database analyze");
            // Analyze the current and previous hq_metric_data table
            serverConfigManager.analyzeHqMetricTables(true);
            // Analyze all non-metric tables
            serverConfigManager.analyzeNonMetricTables();
            long secs = (System.currentTimeMillis() - analyzeStart) / 1000;
            _log.info("Completed database analyze " + secs + " secs");
        } finally {
            synchronized (analyzeRunningLock) {
                analyzeRunning = false;
                _lastAnalyze = analyzeStart;
            }
        }
    }

    private void runDBMaintenance() {
        // Once compression finishes, we check to see if databae maintaince
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
            _log.error("No data maintenance interval found");
            return;
        }

        long maintInterval = Long.parseLong(dataMaintenance);
        if (maintInterval <= 0) {
            _log.error("Maintenance interval was specified as [" + dataMaintenance + "] -- which is invalid");
            return;
        }

        long vacuumStart = System.currentTimeMillis();
        if (TimingVoodoo.roundDownTime(time_start, HOUR) == TimingVoodoo.roundDownTime(time_start, maintInterval)) {
            _log.info("Performing database maintenance (VACUUM ANALYZE)");
            serverConfigManager.vacuum();

            _log.info("Database maintenance completed in " + ((System.currentTimeMillis() - vacuumStart) / 1000) +
                      " seconds.");
        } else {
            _log.info("Not performing database maintenance");
        }
    }

    protected void purge(Properties conf, long now) throws NamingException {
        ConcurrentStatsCollector stats = ConcurrentStatsCollector.getInstance();
        long start = now();
        purgeEventLogs(conf, now);
        stats.addStat((now() - start), ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
        start = now();
        purgeMeasurements();
        stats.addStat((now() - start), ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
    }

    /**
     * Remove measurements no longer associated with a resource.
     */
    private void purgeMeasurements() {
        long start = System.currentTimeMillis();
        try {
            int dcount = measurementManager.removeOrphanedMeasurements();
            _log.info("Removed " + dcount + " measurements in " + ((System.currentTimeMillis() - start) / 1000) +
                      " seconds.");
        } catch (Throwable t) {
            // Do not allow errors to cause other maintenance functions to
            // not run.
            _log.error("Error removing measurements", t);
        }
    }

    /**
     * Purge Event Log data
     */
    private void purgeEventLogs(Properties conf, long now) throws NamingException {
        String purgeEventString = conf.getProperty(HQConstants.EventLogPurge);
        long purgeEventLog = Long.parseLong(purgeEventString);

        // Purge event logs

        _log.info("Purging event logs older than " + TimeUtil.toString(now - purgeEventLog));
        try {
            int rowsDeleted = eventLogManager.deleteLogs(-1, now - purgeEventLog);
            _log.info("Done (Deleted " + rowsDeleted + " event logs)");
        } catch (Exception e) {
            _log.error("Unable to delete event logs: " + e.getMessage(), e);
        }
    }
}
