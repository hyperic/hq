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

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.EventLogManagerUtil;
import org.hyperic.hq.measurement.shared.DataCompressLocal;
import org.hyperic.hq.measurement.server.session.DataCompressEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.stats.ConcurrentStatsCollector;

public class DataPurgeJob implements Runnable {

    private static final Log _log = LogFactory.getLog(DataPurgeJob.class);

    static long HOUR = MeasurementConstants.HOUR;
    static long MINUTE = MeasurementConstants.MINUTE;
    private static long _lastAnalyze = 0l;
    private static long ANALYZE_INTERVAL = 6*HOUR;

    // We create a private static class, in case the DataPurgeJob is 
    // dynamically proxied (which would result in multiple instances of
    // static variables
    private static class DataPurgeLockHolder {
        private static final Object ANALYZE_RUNNING_LOCK = new Object();
        private static boolean analyzeRunning = false;
        private static final Object COMPRESS_RUNNING_LOCK = new Object();
        private static boolean compressRunning = false;
    }
    
    public synchronized void run() {
        try {
            compressData();
            Properties conf = null; 
            try { 
                conf = ServerConfigManagerEJBImpl.getOne().getConfig(); 
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
    public static void compressData()
        throws CreateException, NamingException
    {
        final ServerConfigManagerLocal serverConfig =
            ServerConfigManagerEJBImpl.getOne();

        final DataCompressLocal dataCompress = DataCompressEJBImpl.getOne();

        // First check if we are already running
        synchronized (DataPurgeLockHolder.COMPRESS_RUNNING_LOCK) {
            if (DataPurgeLockHolder.compressRunning) {
                _log.info("Not starting data compression. (Already running)");
                return;
            } else {
                DataPurgeLockHolder.compressRunning = true;
            }
        }

        final long time_start = now();
        
        try {
            // Announce
            _log.info("Data compression starting at " +
                      TimeUtil.toString(time_start));

            ConcurrentStatsCollector stats =
                ConcurrentStatsCollector.getInstance();
            runDBAnalyze(serverConfig);
            stats.addStat((now() - time_start),
                ConcurrentStatsCollector.DB_ANALYZE_TIME);

            final long start = now();
            dataCompress.compressData();
            stats.addStat((now() - start),
                ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);
            
        } catch (SQLException e) {
            _log.error("Unable to compress data: " + e, e);
        } finally {
            synchronized (DataPurgeLockHolder.COMPRESS_RUNNING_LOCK) {
                DataPurgeLockHolder.compressRunning = false;
            }
        }

        long time_end = System.currentTimeMillis();
        _log.info("Data compression completed in " +
                  ((time_end - time_start)/1000) +
                  " seconds.");
        runDBMaintenance(serverConfig);
    }
    
    private static final long now() {
        return System.currentTimeMillis();
    }

    private static void runDBAnalyze(ServerConfigManagerLocal serverConfig)
    {
        // First check if we are already running
        long analyzeStart = System.currentTimeMillis();
        synchronized (DataPurgeLockHolder.ANALYZE_RUNNING_LOCK) {
            if (DataPurgeLockHolder.analyzeRunning) {
                _log.info("Not starting db analyze. (Already running)");
                return;
            } else if ((_lastAnalyze + ANALYZE_INTERVAL) > analyzeStart) {
                serverConfig.analyzeHqMetricTables(false);
                _log.info("Only running analyze on current metric data table " +
                    "since last full run was at " +
                    TimeUtil.toString(_lastAnalyze));
                return;
            } else {
                DataPurgeLockHolder.analyzeRunning = true;
            }
        }
        try {
            _log.info("Performing database analyze");
            // Analyze the current and previous hq_metric_data table
            serverConfig.analyzeHqMetricTables(true);
            // Analyze all non-metric tables
            serverConfig.analyzeNonMetricTables();
            long secs = (System.currentTimeMillis()-analyzeStart)/1000;
            _log.info("Completed database analyze " + secs + " secs");
        } finally {
            synchronized (DataPurgeLockHolder.ANALYZE_RUNNING_LOCK) {
                DataPurgeLockHolder.analyzeRunning = false;
                _lastAnalyze = analyzeStart;
            }
        }
    }

    private static void runDBMaintenance(ServerConfigManagerLocal serverConfig)
    {
        // Once compression finishes, we check to see if databae maintaince
        // should be performed.  This is defaulted to 1 hour, so it should
        // always run unless changed by the user.

        long time_start = System.currentTimeMillis();
        Properties conf;
        
        try {
            conf = ServerConfigManagerUtil.getLocalHome().create().getConfig();
        } catch(Exception e) {
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
            _log.error("Maintenance interval was specified as [" + 
                       dataMaintenance + "] -- which is invalid");
            return;
        }

        long vacuumStart = System.currentTimeMillis();
        if (TimingVoodoo.roundDownTime(time_start, HOUR) ==
            TimingVoodoo.roundDownTime(time_start, maintInterval)) {
            _log.info("Performing database maintenance (VACUUM ANALYZE)");
            serverConfig.vacuum();

            _log.info("Database maintenance completed in " +
                      ((System.currentTimeMillis() - vacuumStart)/1000) +
                      " seconds.");
        } else {
            _log.info("Not performing database maintenance");
        }
    }

    protected void purge(Properties conf, long now)
        throws CreateException, NamingException {
        ConcurrentStatsCollector stats = ConcurrentStatsCollector.getInstance();
        long start = now();
        DataPurgeJob.purgeEventLogs(conf, now);
        stats.addStat((now() - start),
            ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
        start = now();
        DataPurgeJob.purgeMeasurements();
        stats.addStat((now() - start),
            ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
    }

    /**
     * Remove measurements no longer associated with a resource.
     */
    private static void purgeMeasurements() {
        long start = System.currentTimeMillis();
        try {
            int dcount =
                MeasurementManagerEJBImpl.getOne().removeOrphanedMeasurements();
            _log.info("Removed " + dcount + " measurements in " +
                      ((System.currentTimeMillis() - start)/1000) + " seconds.");
        } catch (Throwable t) {
            // Do not allow errors to cause other maintenance functions to
            // not run.
            _log.error("Error removing measurements", t);
        }
    }

    /**
     * Purge Event Log data
     */
    private static void purgeEventLogs(Properties conf, long now)
        throws CreateException, NamingException
    {
        String purgeEventString = conf.getProperty(HQConstants.EventLogPurge);
        long purgeEventLog = Long.parseLong(purgeEventString);

        // Purge event logs
        EventLogManagerLocal eventLogManager =
            EventLogManagerUtil.getLocalHome().create();

        _log.info("Purging event logs older than " +
            TimeUtil.toString(now - purgeEventLog));
        try {
            int rowsDeleted = 
                eventLogManager.deleteLogs(-1, now - purgeEventLog);
            _log.info("Done (Deleted " + rowsDeleted + " event logs)");
        } catch (Exception e) {
            _log.error("Unable to delete event logs: " + e.getMessage(), e);
        }
    }
}
