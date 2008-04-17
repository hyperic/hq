/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class OracleMeasurementPlugin
    extends JDBCMeasurementPlugin {

    private static String logCtx = OracleMeasurementPlugin.class.getName();
    private static Log _log = LogFactory.getLog(logCtx);

    public static final String PROP_URL      = "jdbcUrl";
    public static final String PROP_USER     = "jdbcUser";
    public static final String PROP_PASSWORD = "jdbcPassword";

    static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";

    static final String PROP_USERNAME   = "user";
    static final String PROP_TABLESPACE = "tablespace";
    static final String PROP_SEGMENT    = "segment";

    private static HashMap ora8Queries    = null;  // Oracle 8i only
    private static HashMap ora9Queries    = null;  // Oracle 9i only
    private static HashMap ora10Queries   = null;  // Oracle 10g only
    private static HashMap genericQueries = null;  // Any

    private static final String TABLESPACE_QUERY =
        "SELECT * FROM DBA_TABLESPACES WHERE TABLESPACE_NAME=";
    private static final String SEGMENT_QUERY = "select SEGMENT_NAME" +
        " FROM USER_SEGMENTS WHERE SEGMENT_NAME=";

    protected void getDriver()
        throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    protected String getDefaultURL() {
        return getPluginProperty("DEFAULT_URL");
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {   
        // Override JDBCMeasurementPlugin.
        return new ConfigSchema();
    }

    protected void initQueries() {
        if (genericQueries != null) {
            return;
        }

        /**
         * The following mess defines the mapping tables between the Metric
         * attibute name and the SQL query that is used to look up the value.
         * This can be confusing for many reasons:
         *
         * 1. All Oracle servers/services (regarless of version) will use these
         * 2. Many of them are no longer used, but only kept here so to save
         *    someone from having to add them again at a later time.  For
         *    definitive list of which metrics are collected, see the xml
         *    definition file.
         *
         * Oracle 10g exposes most of these through v$sysmetric which avoids
         * this manual process.  Needed for backwards compat though.
         */
        ora8Queries = new HashMap();
        ora9Queries = new HashMap();
        ora10Queries = new HashMap();
        genericQueries = new HashMap();

        String baseQuery = "SELECT value FROM V$SYSSTAT WHERE name = ";

        genericQueries.put("ConsistentChanges", baseQuery +
                           "'consistent changes'");
        genericQueries.put("ConsistentGets", baseQuery +
                           "'consistent gets'");
        genericQueries.put("BlockChanges", baseQuery +
                           "'db block changes'");
        genericQueries.put("BlockGets", baseQuery +
                           "'db block gets'");
        genericQueries.put("BufferInspected", baseQuery +
                           "'free buffer inspected'");
        genericQueries.put("BufferRequested", baseQuery +
                           "'free buffer requested'");
        genericQueries.put("BytesSent", baseQuery +
                           "'bytes sent via SQL*Net to client'");
        genericQueries.put("BytesReceived", baseQuery +
                           "'bytes received via SQL*Net from client'");
        genericQueries.put("ClientRoundtrips", baseQuery +
                            "'SQL*Net roundtrips to/from client'");
        genericQueries.put("CPUUsage", baseQuery +
                            "'CPU used when call started'");
        genericQueries.put("CPUUsageRecursive", baseQuery +
                            "'recursive cpu usage'");
        genericQueries.put("CPUUsageParse", baseQuery +
                            "'parse time cpu'");
        genericQueries.put("LogonsCumulative", baseQuery +
                            "'logons cumulative'");
        genericQueries.put("LogonsCurrent", baseQuery +
                            "'logons current'");
        genericQueries.put("LogonsCurrentUserActive",
                           "SELECT COUNT(*) FROM V$SESSION " +
                           "WHERE UPPER(USERNAME) = UPPER('%user%') AND " +
                           "STATUS = 'ACTIVE'");
        genericQueries.put("LogonsCurrentUserInactive",
                           "SELECT COUNT(*) FROM V$SESSION " +
                           "WHERE UPPER(USERNAME) = UPPER('%user%') AND " +
                           "STATUS = 'INACTIVE'");
        genericQueries.put("OpenedCursorsCumulative", baseQuery +
                            "'opened cursors cumulative'");
        genericQueries.put("OpenedCursorsCurrent", baseQuery +
                            "'opened cursors current'");
        genericQueries.put("OpenedCursorsCurrentUser",
                           "SELECT COUNT(*) FROM V$OPEN_CURSOR " +
                           "WHERE UPPER(USER_NAME) = UPPER('%user%')");
        genericQueries.put("ParseCount", baseQuery +
                           "'parse count (total)'");
        genericQueries.put("HardParseCount", baseQuery +
                           "'parse count (hard)'");
        genericQueries.put("ExecuteCount", baseQuery +
                           "'execute count'");
        genericQueries.put("PhysicalReads", baseQuery +
                           "'physical reads'");
        genericQueries.put("LogicalReads", baseQuery +
                           "'session logical reads'");
        genericQueries.put("PhysicalWrites", baseQuery +
                           "'physical writes'");
        genericQueries.put("RedoEntries", baseQuery +
                           "'redo entries'");
        genericQueries.put("RedoLogSpace", baseQuery +
                           "'redo log space requests'");
        genericQueries.put("RedoSize", baseQuery + 
                           "'redo size'");
        genericQueries.put("RedoLogSize",
                           "SELECT SUM(bytes) from V$LOG");
        genericQueries.put("RedoSyncWrites", baseQuery +
                           "'redo synch writes'");
        genericQueries.put("SessionUGAMemory",
                           "SELECT SUM(s.value) FROM V$STATNAME sn, " +
                           "V$SESSTAT s WHERE sn.statistic#=s.statistic# AND " +
                           "sn.name='session uga memory' GROUP BY sn.name");
        genericQueries.put("SessionPGAMemory",
                           "SELECT SUM(s.value) FROM V$STATNAME sn, " +
                           "V$SESSTAT s WHERE sn.statistic#=s.statistic# AND " +
                           "sn.name='session pga memory' GROUP BY sn.name");
        genericQueries.put("TableFetchContinuedRow", baseQuery +
                           "'table fetch continued row'");
        genericQueries.put("TableFetchRowId", baseQuery +
                           "'table fetch by rowid'");
        genericQueries.put("TableScanBlocks", baseQuery +
                           "'table scan blocks gotten'");
        genericQueries.put("TableScanRows", baseQuery +
                           "'table scan rows gotten'");
        genericQueries.put("TableScansLong", baseQuery +
                           "'table scans (long tables)'");
        genericQueries.put("TableScansShort", baseQuery +
                           "'table scans (short tables)'");
        genericQueries.put("UserCalls", baseQuery +
                           "'user calls'");
        genericQueries.put("UserCommits", baseQuery +
                           "'user commits'");
        genericQueries.put("UserRollbacks", baseQuery +
                           "'user rollbacks'");
        genericQueries.put("CallsPerTx",
                           "SELECT t1.value/t2.value FROM V$SYSSTAT t1, " +
                           "V$SYSSTAT t2 WHERE t1.name = 'user calls' AND " +
                           "t2.name = 'user commits'");
        genericQueries.put("BlockChangesPerTx",
                           "SELECT t1.value/t2.value FROM V$SYSSTAT t1, " +
                           "V$SYSSTAT t2 WHERE " +
                           "t1.name = 'db block changes' AND " +
                           "t2.name = 'user calls'");
        genericQueries.put("BlockVisitsPerTx",
                           "SELECT (t1.value+t2.value)/t3.value FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2, " +
                           "V$SYSSTAT t3 WHERE " +
                           "t1.name = 'db block gets' AND " +
                           "t2.name = 'consistent gets' AND " +
                           "t3.name = 'user commits'");
        genericQueries.put("CacheHitRatio",
                           "SELECT (t1.value+t2.value-t3.value) / " +
                           "(t4.value + t5.value) FROM V$SYSSTAT t1, " +
                           "V$SYSSTAT t2, V$SYSSTAT t3, V$SYSSTAT t4, " +
                           "V$SYSSTAT t5 WHERE " +
                           "t1.name = 'consistent gets' AND " +
                           "t2.name = 'db block gets' AND " +
                           "t3.name = 'physical reads' AND " +
                           "t4.name = 'consistent gets' AND " +
                           "t5.name = 'db block gets'");
        genericQueries.put("ChangedBlockRatio",
                           "SELECT t1.value/(t2.value + t3.value) FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2, " +
                           "V$SYSSTAT t3 WHERE " +
                           "t1.name = 'db block changes' AND " +
                           "t2.name = 'db block gets' AND " +
                           "t3.name = 'consistent gets'");
        genericQueries.put("ConsistentChangeRatio",
                           "SELECT t1.value/t2.value FROM V$SYSSTAT t1, " +
                           "V$SYSSTAT t2 WHERE " +
                           "t1.name = 'consistent changes' AND " +
                           "t2.name = 'consistent gets'");
        genericQueries.put("ContinuedRowRatio",
                           "SELECT t1.value/(t2.value+t3.value) FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2, " +
                           "V$SYSSTAT t3 WHERE " +
                           "t1.name = 'table fetch continued row' AND " +
                           "t2.name = 'table fetch by rowid' AND " +
                           "t3.name = 'table scan rows gotten'");
        genericQueries.put("RecursiveToUserCallRatio",
                           "SELECT t1.value/t2.value FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2 WHERE " +
                           "t1.name = 'recursive calls' AND " +
                           "t2.name = 'user calls'");
        genericQueries.put("RowSourceRatio",
                           "SELECT t1.value/(t2.value+t3.value) FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2, " +
                           "V$SYSSTAT t3 WHERE " +
                           "t1.name = 'table scan rows gotten' AND " +
                           "t2.name = 'table fetch by rowid' AND " +
                           "t3.name = 'table scan rows gotten'");
        genericQueries.put("UserCallsPerParse",
                           "SELECT t1.value/t2.value FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2 WHERE " +
                           "t1.name = 'user calls' AND " +
                           "t2.name = 'parse count (total)'");
        genericQueries.put("UserRollbackRatio",
                           "SELECT t1.value/(t2.value+t3.value) FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2, " +
                           "V$SYSSTAT t3 WHERE " +
                           "t1.name = 'user rollbacks' AND " +
                           "t2.name = 'user commits' AND " +
                           "t3.name = 'user rollbacks'");
        genericQueries.put("RedoLogSpaceWaitRatio",
                           "SELECT t1.value/t2.value FROM " +
                           "V$SYSSTAT t1, V$SYSSTAT t2 WHERE " +
                           "t1.name = 'redo log space requests' AND " +
                           "t2.name = 'redo entries'");
        genericQueries.put("UpTime",
                           "SELECT CAST(((sysdate - startup_time) * 3600 " +
                           "* 24 * 1000) AS INTEGER) from V$INSTANCE WHERE" +
                           " UPPER(INSTANCE_NAME) = UPPER('%instance%')");
        genericQueries.put("InstanceUsedSpace",
                           "SELECT SUM(bytes) FROM SYS.DBA_DATA_FILES");
        genericQueries.put("InstanceFreeSpace",
                           "SELECT SUM(bytes) FROM SYS.DBA_FREE_SPACE");
        genericQueries.put("UsedSpace",
                           "SELECT SUM(bytes) FROM SYS.DBA_SEGMENTS " +
                           "WHERE UPPER(owner) = UPPER('%user%')");

        // Tablespace metrics
        genericQueries.put("TSFreeSpace",
                           "SELECT SUM(bytes) FROM DBA_FREE_SPACE " +
                           "WHERE TABLESPACE_NAME='%tablespace%'");
        genericQueries.put("TSUsedDiskSpace",
                           "SELECT SUM(bytes) FROM DBA_DATA_FILES " +
                           "WHERE TABLESPACE_NAME='%tablespace%'");
        genericQueries.put("TSFreeExtents",
                           "SELECT SUM(bytes/initial_extent) " +
                           "FROM DBA_TABLESPACES ts, DBA_FREE_SPACE fs " +
                           "WHERE ts.tablespace_name = fs.tablespace_name " +
                           "AND ts.tablespace_name='%tablespace%'");
        genericQueries.put("TSNumDataFiles",
                           "SELECT COUNT(*) FROM DBA_DATA_FILES " +
                           "WHERE TABLESPACE_NAME='%tablespace%'");
        genericQueries.put("TSSpaceUsedPercent",
                           "SELECT 1-(SELECT sum(bytes)/1024" +
                           " FROM sys.dba_free_space" +
                           " WHERE tablespace_name = '%tablespace%') /" +
                           " (SELECT sum(bytes/1024) from sys.dba_data_files" +
                           " WHERE tablespace_name = '%tablespace%')"+
                           " AS percent_used" +
                           " FROM dual");

        // Oracle 8i queries
        ora8Queries.put("SortsDisk", baseQuery + "'sorts (disk)'");
        ora8Queries.put("SortsMemory", baseQuery +
                        "'sorts (memory)'");
        ora8Queries.put("SortsRows", baseQuery + "'sorts (rows)'");
        ora8Queries.put("SortsOverflowRatio",
                        "SELECT t1.value/(t2.value+t3.value) FROM " +
                        "V$SYSSTAT t1, V$SYSSTAT t2, " +
                        "V$SYSSTAT t3 WHERE t1.name = 'sorts (disk)' AND " +
                        "t2.name = 'sorts (memory)' AND " +
                        "t3.name = 'sorts (disk)'");

        // Oracle 9i queries
        ora9Queries.put("SortsDisk", baseQuery + "'sorts disk'");
        ora9Queries.put("SortsMemory", baseQuery + "'sorts memory'");
        ora9Queries.put("SortsRows", baseQuery + "'sorts rows'");
        ora9Queries.put("SortsOverflowRatio",
                        "SELECT t1.value/(t2.value+t3.value) FROM " +
                        "V$SYSSTAT t1, V$SYSSTAT t2, " +
                        "V$SYSSTAT t3 WHERE t1.name = 'sorts disk' AND " +
                        "t2.name = 'sorts memory' AND " +
                        "t3.name = 'sorts disk'");

        // Oracle 10g queries
        ora10Queries.put("SegmentSize", "select sum(bytes)" +
            " FROM USER_SEGMENTS" +
            " WHERE SEGMENT_NAME not like 'BIN$%'" +
            " and SEGMENT_NAME not like 'SYS_%'" +
            " and SEGMENT_NAME = '%segment%'");
        ora10Queries.put("NumberOfRows", "select num_rows" +
            " FROM %tablename%" +
            " WHERE %identifier% = '%segment%'");

        // Alias for avail.
        // If we can fetch any metric, consider the server/service available
        //
        genericQueries.put(AVAIL_ATTR,
                           genericQueries.get("PhysicalReads"));
    }

    private String getSegmentQuery(Metric metric)
    {
        String segment = metric.getObjectProperty(PROP_SEGMENT);
        String tablespace = metric.getObjectProperty(PROP_TABLESPACE);
        String sql = (String)ora10Queries.get(metric.getAttributeName());
        sql = StringUtil.replace(sql, "%segment%", segment);
        try {
            Connection conn = getCachedConnection(metric);
            if (OracleControlPlugin.isTable(conn, segment, tablespace)) {
                sql = StringUtil.replace(sql, "%tablename%", "all_tables");
                sql = StringUtil.replace(sql, "%identifier%", "table_name");
            }
            else {
                sql = StringUtil.replace(sql, "%tablename%", "all_indexes");
                sql = StringUtil.replace(sql, "%identifier%", "index_name");
            }
        } catch (SQLException e) {
            _log.error(e.getMessage(), e);
        }
        return sql;
    }

    protected String getQuery(Metric metric)
    {
        String alias = metric.getAttributeName();
        String objName = metric.getObjectName();
        if (-1 != objName.indexOf("Type=Segment")) {
            return getSegmentQuery(metric);
        }

        String query = (String)genericQueries.get(alias);
        Properties props = metric.getObjectProperties();

        if (query == null) {
            // Not in the generic queries, check the version specific table
            // XXX: grab the version from the Metric, this will currently ignore
            //      any Oracle 9i specific Metric
            query = (String)ora8Queries.get(alias);
        }

        //XXX: Would have been nice to put the processed query to execute in the
        //     template to avoid this substituion on each collection.

        // Do substituion on the user name in the SQL query
        String user = metric.getObjectProperties().getProperty(PROP_USERNAME);
        if (user == null) {
            // Backwards compat
            user = metric.getProperties().getProperty(PROP_USERNAME);
        }
        if (user != null) {
            query = StringUtil.replace(query, "%user%", user);
        }

        String tablespace = metric.getObjectProperties().
            getProperty(PROP_TABLESPACE);
        if (tablespace != null) {
            if (tablespaceIsOffline(tablespace, metric)) {
                _log.debug("Tablespace " + tablespace +
                           " is offline, will return 0 for all metrics.");
                return "select 0 from dual";
            }
            query = StringUtil.replace(query, "%tablespace%", tablespace);
        }

        // XXX: could cache this
        String url = metric.getProperties().getProperty(PROP_URL);
        String instance = url.substring(url.lastIndexOf(":") + 1,
                                        url.length());
        query = StringUtil.replace(query, "%instance%", instance);

        return query;
    }

    private boolean tablespaceIsOffline(String tablespace, Metric metric)
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = getCachedConnection(metric);
            stmt = conn.createStatement();
            String sql = "select lower(status) as status" +
                         " FROM dba_tablespaces " +
                         " WHERE lower(tablespace_name) = " +
                         "lower('"+tablespace+"')";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return (rs.getString("status").equals("offline")) ? true : false;
            }
        } catch (SQLException e) {
            // not sure what we can do if this happens, so just log the error
            _log.error(e.getMessage(), e);
        } finally {
            // don't close the conn, since it is shared
            DBUtil.closeJDBCObjects(getLog(), null, stmt, rs);
        }
        // we don't want to collect any metrics if the tablespace does not exist
        return true;
    }

    /**
     * Override for tablespace avail.
     * AND segment avail.
     */
    protected double getQueryValue(Metric metric)
        throws MetricNotFoundException, PluginException,
               MetricUnreachableException
    {
        String alias = metric.getAttributeName();
        boolean isAvail = alias.equalsIgnoreCase(AVAIL_ATTR);

        if (!isAvail) {
            return super.getQueryValue(metric);
        }

        String tablespace = metric.getObjectProperties().
            getProperty(PROP_TABLESPACE);
        String segment = metric.getObjectProperties().
            getProperty(PROP_SEGMENT);
        if (tablespace == null && segment == null) {
            return super.getQueryValue(metric);
        } else if (tablespace != null) {
            return (tablespaceIsOffline(tablespace, metric)) ?
                MeasurementConstants.AVAIL_DOWN : MeasurementConstants.AVAIL_UP;
        }

        // else, tablespace avail
        Properties props = metric.getProperties();
        String
            url = props.getProperty(PROP_URL),
            user = props.getProperty(PROP_USER),
            pass = props.getProperty(PROP_PASSWORD);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getCachedConnection(url, user, pass);
            String query = SEGMENT_QUERY + "'" + segment + "'";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                return MeasurementConstants.AVAIL_UP;
            } else {
                return MeasurementConstants.AVAIL_DOWN;
            }
        } catch (SQLException e) {

            if (isAvail) {
                getLog().debug("AVAIL_DOWN", e);
                return MeasurementConstants.AVAIL_DOWN;
            }

            // Remove this connection from the cache.
            removeCachedConnection(url, user, pass);

            String msg = "Query failed for " + alias +
                ": " + e.getMessage();

            // Catch divide by 0 errors and return 0
            if(e.getErrorCode() == DBUtil.ORACLE_ERROR_DIVIDE_BY_ZERO)
                return 0;
            if(e.getErrorCode() == DBUtil.ORACLE_ERROR_NOT_AVAILABLE)
                throw new MetricUnreachableException(msg, e);
                
            throw new MetricNotFoundException(msg, e);
        } finally {
            DBUtil.closeJDBCObjects(getLog(), null, ps, rs);
        }
    }
}
