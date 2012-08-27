/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2010], VMware, Inc.
 * This file is part of Hyperic.
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

package org.hyperic.hq.plugin.mysql_stats;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.JDBCQueryCache;
import org.hyperic.hq.product.JDBCQueryCacheException;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.jdbc.DBUtil;

public class MySqlStatsMeasurementPlugin
    extends JDBCMeasurementPlugin
{
    private static final String _logCtx =
        MySqlStatsMeasurementPlugin.class.getName();
    private final Log _log = LogFactory.getLog(MySqlStatsMeasurementPlugin.class);
    static final String PROP_JDBC_DRIVER = "DEFAULT_DRIVER",
                        DEFAULT_DRIVER   = "com.mysql.jdbc.Driver";
    private static final String AVAIL_SELECT  = "select 1",
                           SHOW_DATABASES     = "show databases",
                           SLAVE_STATUS       = "slavestatus",
                           TABLE_SERVICE      = "type=table",
                           SHOW_SLAVE_STATUS  = "show slave status",
                           SHOW_GLOBAL_STATUS = "show /*!50002 global */ status",
                           // computed for mysql replication
                           BYTES_BEHIND_MASTER     = "Bytes_Behind_Master",
                           SECONDS_BEHIND_MASTER   = "Seconds_Behind_Master",
                           LOG_FILES_BEHIND_MASTER = "Log_Files_Behind_Master",
                           SLAVE_SQL_RUNNING       = "Slave_SQL_Running";
    private String _driver;
    private static final int CACHE_TIMEOUT = Integer.parseInt(System.getProperty("mysql_stats.cache.timeout", "10000"));
    private Map<String, Map<String, JDBCQueryCache>> _urlQueryCacheMap = new HashMap<String, Map<String, JDBCQueryCache>>();
    private final Map<String, JDBCQueryCache> _tableStatusCacheMap = new HashMap<String, JDBCQueryCache>();
    
    private static final int TIMEOUT_VALUE = Integer.parseInt(System.getProperty("mysql_stats.jdbc.timeout", "60000"));
    private int _consecutiveErrors = 0;
    private static final int MAX_ERRORS = Integer.parseInt(System.getProperty("mysql_stats.max_errors", "3"));
    
    protected double getQueryValue(Metric metric)
        throws MetricNotFoundException,
               PluginException,
               MetricUnreachableException {
        // will look like "mysqlstats:Type=Global,jdbcUser=user,jdbcPasswd=pass"
        String objectName = metric.getObjectName().toLowerCase();
        
        // will look like "availability"
        String alias = metric.getAttributeName();
        boolean sqlException = false;
        try {
            setDriver(metric);
            if (objectName.indexOf(SHOW_GLOBAL_STATUS) != -1) {
                return getGlobalStatusMetric(metric);
            } else if (objectName.indexOf(SHOW_SLAVE_STATUS) != -1) {
                return getSlaveStatusMetric(metric);
            } else if (objectName.indexOf(SLAVE_STATUS) != -1) {
                return getMasterSlaveStatusMetric(metric);
            } else if (objectName.indexOf(SHOW_DATABASES) != -1) {
                return getNumberOfDatabases(metric);
            } else if (objectName.indexOf(TABLE_SERVICE) != -1) {
                return getTableMetric(metric);
            }
        } catch (MetricUnreachableException e) {
            throw e;
        } catch (SQLException e) {
            sqlException = true;
            _log.debug(e, e);
            if (metric.isAvail()) {
                return Metric.AVAIL_DOWN;
            }
            throw new MetricNotFoundException(
                "Service " + objectName + ":" + alias +
                " not found: " + e.getMessage());
        } catch (Exception e) {
            _log.debug(e, e);
            if (metric.isAvail()) {
                return Metric.AVAIL_DOWN;
            }
            throw new MetricNotFoundException(
                "Service " + objectName + ":" + alias +
                " not found: " + e.getMessage());
        } finally {
            setErrorState(sqlException);
        }
        throw new MetricNotFoundException(
            "Service "+objectName+":"+alias+" not found");
    }
    
    private void setErrorState(final boolean sqlExeceptionOccured) {
        if (sqlExeceptionOccured) {
            _consecutiveErrors++;
            if (_consecutiveErrors > MAX_ERRORS) {
                setCacheExpireTime(System.currentTimeMillis() + 60000);
            }
        } else {
            _consecutiveErrors = 0;
        }
    }

    private void setCacheExpireTime(long expireTime) {
        _log.info("Received more than " + MAX_ERRORS + " SQLExceptions, " +
            "disabling all " + _tableStatusCacheMap.size() +
            " JDBCQueryCaches until " + TimeUtil.toString(expireTime));
        for (JDBCQueryCache cache : _tableStatusCacheMap.values()) {
            cache.setExpireTime(expireTime);
            cache.clearCache();
        }
    }

    private double getTableMetric(Metric metric)
        throws NumberFormatException,
               SQLException,
               JDBCQueryCacheException {
        final String table = metric.getObjectProperty("table");
        final String dbname = metric.getObjectProperty("database");
        final String jdbcUrl = metric.getObjectProperty(PROP_URL);
        final String cacheKey = jdbcUrl +"-"+ table;
        String alias = metric.getAttributeName();
        JDBCQueryCache tableCache =
            (JDBCQueryCache)_tableStatusCacheMap.get(cacheKey);
        // don't want to cache all of "show table status" in the case
        // we have a deployment with 1000s of tables.  They probably don't care
        // about all the tables.  There is still a speed/efficiency improvement
        // over the old mysql plugin since the query only has to be run once for
        // all the individual table metrics.
        if (tableCache == null) {
            final String sql = new StringBuffer()
                .append("SELECT * FROM information_schema.tables")
                .append(" WHERE lower(table_name) = '")
                    .append(table.toLowerCase()).append('\'')
                .append(" AND lower(table_schema) = '")
                	.append(dbname.toLowerCase()).append('\'')
                .append(" AND engine is not null").toString();
            tableCache = new JDBCQueryCache(sql, "table_name", CACHE_TIMEOUT);
            _tableStatusCacheMap.put(cacheKey, tableCache);
        }
        if (metric.isAvail()) {
            alias = "TABLE_ROWS";
        }
        Connection conn = getCachedConnection(metric);
        Object cachedVal = null;
        cachedVal = tableCache.get(conn, table, alias);
        if (cachedVal == null) {
            if (metric.isAvail()) {
                return Metric.AVAIL_DOWN;
            } else {
                final String msg = "Could not get metric for table " + table;
                throw new SQLException(msg);
            }
        }
        Double val = Double.valueOf(cachedVal.toString());
        if (metric.isAvail()) {
            return Metric.AVAIL_UP;
        }
        return val.doubleValue();
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        // Override JDBCMeasurementPlugin.
        return new ConfigSchema();
    }

    private double getMasterSlaveStatusMetric(Metric metric)
        throws SQLException,
               MetricUnreachableException {
        Connection conn = getCachedConnection(metric);
        Statement stmt = null;
        ResultSet rs = null;
        final boolean isAvail = metric.isAvail();
        final String slaveAddr = metric.getObjectProperty("slaveAddress");
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("show full processlist");
            int userCol = rs.findColumn("User"),
                addrCol = rs.findColumn("Host"),
                timeCol = rs.findColumn("Time");
            while (rs.next()) {
                String pUser = rs.getString(userCol);
                String addr = rs.getString(addrCol);
                if (!pUser.equalsIgnoreCase("slave") && !addr.equals(slaveAddr)) {
                    continue;
                }
                if (isAvail) {
                    return Metric.AVAIL_UP;
                }
                return rs.getDouble(timeCol);
            }
        } finally {
            // don't close connection, it is cached
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
        }
        if (isAvail) {
            return Metric.AVAIL_DOWN;
        }
        throw new MetricUnreachableException(
            "Cannot retrieve mysql process time for slave " + slaveAddr);
    }

    private double getGlobalStatusMetric(Metric metric)
        throws NumberFormatException,
               SQLException,
               JDBCQueryCacheException {
        JDBCQueryCache globalStatus = getQueryCache(metric, SHOW_GLOBAL_STATUS);
        String valColumn = metric.getObjectProperty("value");
        String alias = metric.getAttributeName();
        if (metric.isAvail()) {
            return getAvailability(metric).getValue();
        } else {
            Connection conn = getCachedConnection(metric);
            Double val = Double.valueOf(
                globalStatus.get(conn, alias, valColumn).toString());
            return val.doubleValue();
        }
    }
    
    private double getSlaveStatusMetric(Metric metric)
        throws NumberFormatException,
               MetricUnreachableException,
               SQLException,
               JDBCQueryCacheException,
               MetricNotFoundException {
        String valColumn = metric.getObjectProperty("value");
        String alias = metric.getAttributeName();
        JDBCQueryCache replStatus = getQueryCache(metric, SHOW_SLAVE_STATUS);
        if (alias.equalsIgnoreCase(BYTES_BEHIND_MASTER)) {
            final double tmp = getBytesBehindMaster(metric);
            return (tmp >= 0) ? tmp : 0d;
        } else if (alias.equalsIgnoreCase(LOG_FILES_BEHIND_MASTER)) {
            return getLogFilesBehindMaster(metric);
        } else if (alias.equalsIgnoreCase(SECONDS_BEHIND_MASTER)) {
            // HHQ-3207 need to special case this since the metric may be null
            // http://feedblog.org/2007/09/29/where-does-mysql-lie-about-seconds_behind_master/
            Connection conn = getCachedConnection(metric);
            Double val = Double.valueOf(
                replStatus.getOnlyRow(conn, valColumn).toString());
            if (val == null) {
                throw new MetricNotFoundException("query for " +
                    SECONDS_BEHIND_MASTER + " returned null");
            }
            return val.doubleValue();
        } else if (metric.isAvail()) {
            Connection conn = getCachedConnection(metric);
            String s = replStatus.getOnlyRow(conn, SLAVE_SQL_RUNNING).toString();
            return (s.equalsIgnoreCase("yes")) ?
                Metric.AVAIL_UP : Metric.AVAIL_DOWN;
        } else {
            Connection conn = getCachedConnection(metric);
            String s = replStatus.getOnlyRow(conn, valColumn).toString();
            Double val = null;
            if (s.equalsIgnoreCase("yes")) {
                val = new Double(1);
            } else if (s.equalsIgnoreCase("no")) {
                val = new Double(0);
            } else {
                val = Double.valueOf(s);
            }
            return val.doubleValue();
        }
    }

    private double getBytesBehindMaster(Metric metric)
        throws NumberFormatException,
               SQLException,
               JDBCQueryCacheException,
               MetricUnreachableException {
        Connection conn = getCachedConnection(metric);
        JDBCQueryCache replStatus = getQueryCache(metric, SHOW_SLAVE_STATUS);
        double slaveLogPos = -1d;
        try {
            slaveLogPos = Double.valueOf(replStatus.getOnlyRow(
                conn, "Exec_Master_Log_Pos").toString()).doubleValue();
        } catch (Exception e) {
            // for 4.0 replication, the cases are different
            slaveLogPos = Double.valueOf(replStatus.getOnlyRow(
                conn, "Exec_master_log_pos").toString()).doubleValue();
        }
        double masterLogPos = Double.valueOf(replStatus.getOnlyRow(
            conn, "Read_Master_Log_Pos").toString()).doubleValue();
        return masterLogPos - slaveLogPos;
    }

    private double getLogFilesBehindMaster(Metric metric)
        throws SQLException,
               JDBCQueryCacheException {
        Connection conn = getCachedConnection(metric);
        JDBCQueryCache replStatus = getQueryCache(metric, SHOW_SLAVE_STATUS);
        String masterLogFile =  replStatus.getOnlyRow(
                conn, "Master_Log_File").toString();
        String slaveLogFile =  replStatus.getOnlyRow(
                conn, "Relay_Master_Log_File").toString();
        if (masterLogFile.equals(slaveLogFile)) {
            return 0d;
        }
        String[] toks = masterLogFile.split("\\.");
        int masterNum = Integer.valueOf(toks[1]).intValue();
        toks = slaveLogFile.split("\\.");
        int slaveNum = Integer.valueOf(toks[1]).intValue();
        return masterNum - slaveNum;
    }
    
    private JDBCQueryCache getQueryCache(Metric metric, String query){
        String jdbcUrl = metric.getProperties().getProperty(PROP_URL);
        String keyColumn = metric.getObjectProperty("key");
        JDBCQueryCache queryCache;        
        if (!_urlQueryCacheMap.containsKey(jdbcUrl)){
            Map<String, JDBCQueryCache> queryMap = new HashMap<String, JDBCQueryCache>();
            queryCache = new JDBCQueryCache(query, keyColumn, CACHE_TIMEOUT);
            queryMap.put(query,  queryCache);
            _urlQueryCacheMap.put(jdbcUrl, queryMap);
        } else if (!_urlQueryCacheMap.get(jdbcUrl).containsKey(query)){
            queryCache = new JDBCQueryCache(query, keyColumn, CACHE_TIMEOUT);
            _urlQueryCacheMap.get(jdbcUrl).put(query, queryCache);
        } else {
            queryCache = _urlQueryCacheMap.get(jdbcUrl).get(query);
        }
        return queryCache;
    }
    
    protected Connection getCachedConnection(Metric metric) throws SQLException {
        Connection conn = super.getCachedConnection(metric);
        final boolean autocommit = conn.getAutoCommit();
        if (!autocommit) {
            conn.setAutoCommit(true);
        }
        return conn;
    }

    /**
     * Used to validate connection to db based on user defined config props.
     * Therefore need to throw MetricUnreachableException
     */
    private double getNumberOfDatabases(Metric metric)
        throws MetricUnreachableException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getCachedConnection(metric);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(SHOW_DATABASES);
            double rtn = 0;
            while (rs.next()) {
                rtn++;
            }
            return rtn;
        } catch (Exception e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        } finally {
            // don't close connection, it is cached
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
        }
    }

    private void setDriver(Metric metric) {
        if (_driver == null) {
            Properties p = metric.getObjectProperties();
            _driver = p.getProperty(PROP_JDBC_DRIVER, DEFAULT_DRIVER);
        }
    }

    private MetricValue getAvailability(Metric metric) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getCachedConnection(metric);
            stmt = conn.createStatement();
            stmt.executeQuery(AVAIL_SELECT).close();
            return new MetricValue(MeasurementConstants.AVAIL_UP);
        } catch (SQLException e) {
            _log.debug(e, e);
        } finally {
            // don't close conn, it is cached
            DBUtil.closeStatement(_logCtx, stmt);
        }
        return new MetricValue(MeasurementConstants.AVAIL_DOWN);
    }

    protected Connection getConnection(String url, String user, String password)
        throws SQLException {
        try {
            password = (password == null) ? "" : password;
            password = (password.matches("^\\s*$")) ? "" : password;
            Driver driver = (Driver)Class.forName(_driver).newInstance();
            final Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);
            return driver.connect(getJdbcUrl(url), props);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage());
        }
    }

    private String getJdbcUrl(String url) {
        if (url == null) {
            return url;
        }
        else if (url.indexOf('?') > 0) {
            return url + "&socketTimeout=" + TIMEOUT_VALUE + "&connectTimeout="
                    + TIMEOUT_VALUE + "&autoReconnect=true";
        }
        else {
            return url + "?socketTimeout=" + TIMEOUT_VALUE + "&connectTimeout="
                    + TIMEOUT_VALUE + "&autoReconnect=true";
        }
    }

    // This is legacy before config props were specified in hq-plugin.xml,
    // not needed
    protected String getDefaultURL() {
        return null;
    }

    /**
     * Did not implement this method in favor of getQueryValue.  Doesn't work
     * well with the JDBCQueryCache scheme.
     */
    protected String getQuery(Metric jdsn) {
        return null;
    }

    protected void initQueries() {
    }

    protected void getDriver() throws ClassNotFoundException {
    }
}
