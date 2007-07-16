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

package org.hyperic.hq.measurement.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The DataCompress EJB handles all compression and purging of
 * measurement data in the HQ system.
 *
 * @ejb:bean name="DataCompress"
 *      jndi-name="ejb/measurement/DataCompress"
 *      local-jndi-name="LocalDataCompress"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class DataCompressEJBImpl 
    extends SessionEJB 
    implements SessionBean {

    // !!!NEEDS TO BE CHANGED WHEN WE CONVERT DB FROM long to int
    private static final long BF_PURGE_INCR = 1200000;
    private static final String logCtx = DataCompressEJBImpl.class.getName();
    private final Log log = LogFactory.getLog(logCtx);
    private static final String BF_TABLE = MeasTabManagerUtil.OLD_MEAS_TABLE;
    private static final String METRIC_DATA_VIEW = MeasTabManagerUtil.MEAS_VIEW;

    // For purging alerts
    private AlertManagerLocal alertManager = null;

    // Data tables
    private final String TAB_DATA    = MeasurementConstants.TAB_DATA;
    private final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;
    private final String TAB_PROB    = MeasurementConstants.TAB_PROB;

    // Utility constants
    private final long HOUR     = MeasurementConstants.HOUR;
    private final long SIX_HOUR = MeasurementConstants.SIX_HOUR;
    private final long DAY      = MeasurementConstants.DAY;

    // Max delete rows per statement
    // on backfill table
    private final int MAX_BF_DEL_ROWS = 1000;

    // Purge intervals, loaded once on first invocation.
    private boolean purgeDefaultsLoaded = false;
    private long purgeRaw, purge1h, purge6h, purge1d, purgeAlert;

    private AlertManagerLocal getAlertManager() {
        if(this.alertManager == null){
            try {
                this.alertManager = AlertManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return this.alertManager;
    }

    /**
     * Get the server purge configuration, loaded on startup.
     */
    private void loadPurgeDefaults() 
    {
        this.log.info("Loading default purge intervals");
        Properties conf;
        try {
            conf = ServerConfigManagerUtil.getLocalHome().create().getConfig();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (ConfigPropertyException e) {
            // Not gonna happen
            throw new SystemException(e);
        }

        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        String purge1hString  = conf.getProperty(HQConstants.DataPurge1Hour);
        String purge6hString  = conf.getProperty(HQConstants.DataPurge6Hour);
        String purge1dString  = conf.getProperty(HQConstants.DataPurge1Day);
        String purgeAlertString = conf.getProperty(HQConstants.AlertPurge);

        try {
            this.purgeRaw = Long.parseLong(purgeRawString);
            this.purge1h = Long.parseLong(purge1hString);
            this.purge6h = Long.parseLong(purge6hString);
            this.purge1d = Long.parseLong(purge1dString);
            this.purgeAlert = Long.parseLong(purgeAlertString);
            this.purgeDefaultsLoaded = true;
        } catch (NumberFormatException e) {
            // Shouldn't happen unless manual edit of config table
            throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    private void truncateRawMeasurements(long truncateBefore)
        throws SQLException, NamingException
    {
        // we can't get any accurate metric tablenames if truncateBefore
        // is less than the base point in time which is used for the
        // tablename calculations
        if (truncateBefore < MeasTabManagerUtil.getBaseTime())
            return;
        long currtime = System.currentTimeMillis();
        String currTable = MeasTabManagerUtil.getMeasTabname(currtime);
        long currTruncTime = truncateBefore;
        //just in case truncateBefore is in the middle of a table
        currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(currTruncTime);
        String delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
        if (delTable.equals(currTable))
        {
            currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(currTruncTime);
            delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
        }
        log.info("Purging Raw Measurement Data older than " +
                 TimeUtil.toString(truncateBefore));
        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           DATASOURCE_NAME);
            stmt = conn.createStatement();
            StopWatch watch = new StopWatch();
            log.debug("Truncating tables, starting with -> "+delTable+
                      " (currTable -> "+currTable+")\n");
            HQDialect dialect = Util.getHQDialect();
            while (!currTable.equals(delTable) &&
                   truncateBefore > currTruncTime)
            {
                log.debug("Truncating table "+delTable);
                stmt.executeUpdate("truncate table "+delTable);
                String sql = dialect.getOptimizeStmt(delTable, 0);
                stmt.execute(sql);
                currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(
                                                              currTruncTime);
                delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
            }
            // for backwards compatibility
            truncateOldMeasTable(truncateBefore, stmt);
            log.info("Done Purging Raw Measurement Data (" +
                     ((watch.getElapsed()) / 1000) + " seconds)");
        }
        finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }
    }

    private void truncateOldMeasTable(long truncateBefore, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT max(timestamp) as maxtimestamp "+
                         " FROM "+BF_TABLE+
                         " HAVING max(timestamp) is not null";
            rs = stmt.executeQuery(sql);
            while (rs.next())
            {
                if (truncateBefore > rs.getLong("maxtimestamp"))
                    stmt.executeUpdate("truncate table "+BF_TABLE);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }
    
    /**
     * Clean backfilled data
     * @ejb:interface-method
     */
    public void purgeBackfilled() throws SQLException, NamingException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            long min_time = 0;
            long max_time = 0;
            conn = DBUtil
                    .getConnByContext(getInitialContext(), DATASOURCE_NAME);
            stmt = conn.createStatement();
            String sql = "SELECT min(timestamp) FROM "+BF_TABLE+
                         " HAVING min(timestamp) is not null";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                min_time = rs.getLong(1);

            sql = "SELECT max(timestamp) FROM "+BF_TABLE+
                  " HAVING max(timestamp) is not null";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                max_time = rs.getLong(1);

            HQDialect dialect = Util.getHQDialect();
            int totalrows = 0;
            int rows = 0;
            while (rows > 0)
            {
                String delTable   = BF_TABLE+" b",
                       commonKey  = "b.measurement_id",
                       joinTables = BF_TABLE+" b, "+METRIC_DATA_VIEW+" m",
                       joinKeys   = "m.measurement_id = b.measurement_id"+
                                    " and m.timestamp = b.timestamp",
                       condition  = "";

                sql = dialect.getDeleteJoinStmt(delTable,
                                                commonKey,
                                                joinTables,
                                                joinKeys,
                                                condition,
                                                MAX_BF_DEL_ROWS);
                rows = stmt.executeUpdate(sql);
                totalrows += rows;
            }
            if (totalrows > 0) {
                log.debug("Purged "+rows+" rows of backfilled data between"+
                          " "+TimeUtil.toString(max_time)+" and "+
                          TimeUtil.toString(max_time+BF_PURGE_INCR));
            }
        }
        finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }
        
    /**
     * Entry point for data compression routines
     * 
     * @ejb:interface-method
     */
    public void compressData() 
        throws NamingException, SQLException
    {
        // Load defaults if not already loaded
        if (!this.purgeDefaultsLoaded)
            loadPurgeDefaults();

        // Round down to the nearest hour.
        long now = TimingVoodoo.roundDownTime(System.currentTimeMillis(), HOUR);
        long last;

        // Compress hourly data
        last = compressData(TAB_DATA, TAB_DATA_1H, HOUR, now);
        // Purge, ensuring we don't purge data not yet compressed.
        truncateRawMeasurements(Math.min(now - this.purgeRaw, last));

        // Purge metric problems as well
        purgeMeasurements(TAB_PROB,
                          Math.min(now - this.purgeRaw, last));

        // Compress 6 hour data
        last = compressData(TAB_DATA_1H, TAB_DATA_6H, SIX_HOUR, now);
        // Purge, ensuring we don't purge data not yet compressed.
        purgeMeasurements(TAB_DATA_1H, 
                          Math.min(now - this.purge1h, last));

        // Compress daily data
        last = compressData(TAB_DATA_6H, TAB_DATA_1D, DAY, now);
        // Purge, ensuring we don't purge data not yet compressed.
        purgeMeasurements(TAB_DATA_6H, 
                          Math.min(now - this.purge6h, last));

        // Purge, we never store more than 1 year of data.
        purgeMeasurements(TAB_DATA_1D, now - this.purge1d);

        // Purge alerts
        log.info("Purging alerts older than " +
                 TimeUtil.toString(now - this.purgeAlert));
        int alertsDeleted =
            getAlertManager().deleteAlerts(0, now - this.purgeAlert);
        log.info("Done (Deleted " + alertsDeleted + " alerts)");
    }

    /**
     * Compress data.
     * XXX: Perhaps we should put a time limit on this?
     *      (Something like 5 min during business hrs, 45 min
     *       otherwise?)
     * @return The last timestamp that was compressed
     */
    private long compressData(String fromTable, String toTable, long interval,
                              long now) 
        throws NamingException, SQLException
    {
        // First determine the window to operate on.  If no previous
        // compression information is found, the last value from the
        // table to compress from is used.  (This will only occur on
        // the first compression run).
        long start = getMaxTimestamp(toTable);
        if (start == 0) {
            // No compressed data found, start from scratch.
            // Need to validate this behaviour with the oracle
            // JDBC driver.  If no data exists the Postgres driver 
            // returns 0 for MIN() or MAX().
            start = getMinTimestamp(fromTable);

            // No measurement data found. (Probably a new installation)
            if (start == 0) {
                return 0;
            }
        } else {
            // Start at next interval
            start = start + interval;
        }

        // Rounding only necessary since if we are starting from scratch.
        long begin = TimingVoodoo.roundDownTime(start, interval);

        // Compress all the way up to now.
        log.info("Compressing from: " + fromTable + " to " + toTable);
        
        return compactData(fromTable, toTable, begin, now, interval);
    }
    
    private long compactData(String fromTable, String toTable,
                             long begin, long now, long interval)
        throws SQLException, NamingException {
        Connection        conn    = null;
        PreparedStatement insStmt = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            // One special case.. If we are compressing from an
            // already compressed table, we'll take the MIN and
            // MAX from the already calculated min and max columns.
            String minMax;
            if (fromTable.equals(TAB_DATA)) {
                minMax = "AVG(value), MIN(value), MAX(value) ";
            } else {
                minMax = "AVG(value), MIN(minvalue), MAX(maxvalue) ";
            }

            insStmt = conn.prepareStatement(
                    "INSERT INTO " + toTable +
                    " (SELECT measurement_id, ? AS timestamp, " + minMax +
                    "FROM " + fromTable +
                    " WHERE timestamp >= ? AND timestamp < ? " +
                    "GROUP BY measurement_id)");

            StopWatch watch = new StopWatch();
            while (begin < now) {

                long end = begin + interval;

                log.info("Compression interval: " + TimeUtil.toString(begin) +
                         " to " + TimeUtil.toString(end));

                // Compress.
                watch.reset();
                
                int i = 1;
                try {
                    insStmt.setLong(i++, begin);
                    insStmt.setLong(i++, begin);
                    insStmt.setLong(i++, end);
                    insStmt.execute();
                } catch (SQLException e) {
                    // Just log the error and continue
                    log.debug("SQL exception when inserting data "
                              + " at " + TimeUtil.toString(begin), e);
                }

                log.info("Done (" + (watch.getElapsed() / 1000) + " seconds)");

                // Increment for next interation.
                begin = end;
            }
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, insStmt, null);
        }
        
        // Return the last interval that was compressed.
        return begin;
    }

    /**
     * Helper methods
     */

    /**
     * Get the oldest timestamp in the database.  Getting the minimum time
     * is expensive, so this is only called once when the compression 
     * routine runs for the first time.  After the first call, the range
     * is cached.
     */
    private long getMinTimestamp(String dataTable) 
        throws SQLException, NamingException
    {
        Connection        conn = null;
        Statement         stmt = null;
        ResultSet         rs   = null;
    
        try {
            String sql = "SELECT MIN(timestamp) FROM " + dataTable;
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            // We'll have a single result
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new SQLException("Unable to determine oldest " +
                                       "measurement");
            }
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Get the most recent measurement.
     */
    private long getMaxTimestamp(String dataTable) 
        throws SQLException, NamingException
    {
        Connection        conn = null;
        Statement         stmt = null;
        ResultSet         rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            String sql;
            if (DBUtil.isPostgreSQL(conn)) {
                // Postgres handles this much better
                sql = "SELECT timestamp FROM " + dataTable +
                    " ORDER BY timestamp DESC LIMIT 1";
            } else {
                sql = "SELECT MAX(timestamp) FROM " + dataTable;
            }

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            // We'll have a single result
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                // New installation
                return 0l;
            }
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Purge data older than a given time.
     */
    private void purgeMeasurements(String tableName, long purgeAfter)
        throws SQLException, NamingException
    {
        Connection        conn = null;
        PreparedStatement stmt = null;
        long interval = HOUR;
        long min = getMinTimestamp(tableName);

        // No data
        if (min == 0) {
            return;
        }
        
        log.info("Purging data older than " +
                 TimeUtil.toString(purgeAfter) + " in " +
                 tableName);

        StopWatch watch = new StopWatch();
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            conn.setAutoCommit(false);

            long endWindow = purgeAfter;
            long startWindow = endWindow - interval; 
            String sql = "DELETE FROM " + tableName +
                         " WHERE timestamp BETWEEN ? AND ?";
            stmt = conn.prepareStatement(sql);

            while (endWindow > min) {
                log.debug("Purging data between " +
                          TimeUtil.toString(startWindow) + " and " +
                          TimeUtil.toString(endWindow) + " in " +
                          tableName);
                
                stmt.setLong(1, startWindow);
                stmt.setLong(2, endWindow);
                stmt.execute();
                conn.commit();

                endWindow -= interval;
                startWindow -= interval;
            }
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }

        log.info("Done (" + ((watch.getElapsed()) / 1000) + " seconds)");
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}
