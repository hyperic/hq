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

package org.hyperic.hq.plugin.mysql_stats;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final String  SELECT_VERSION     = "select @@version",
                           SHOW_DATABASES     = "show databases",
                           SLAVE_STATUS       = "slavestatus",
                           SHOW_SLAVE_STATUS  = "show slave status",
                           SHOW_GLOBAL_STATUS = "show /*!50002 global */ status",
                           // computed for mysql replication
                           BYTES_BEHIND_MASTER     = "Bytes_Behind_Master",
                           LOG_FILES_BEHIND_MASTER = "Log_Files_Behind_Master";
    private String _driver;
    private JDBCQueryCache _globalStatus = null,
                           _replStatus   = null;
    
    protected double getQueryValue(Metric metric)
        throws MetricNotFoundException,
               PluginException,
               MetricUnreachableException {
        // will look like "mysqlstats:Type=Global,jdbcUser=user,jdbcPasswd=pass"
        String objectName = metric.getObjectName().toLowerCase();
        // will look like "availability"
        String alias = metric.getAttributeName();
        try {
            setDriver(metric);
            setGlobalStatus(metric, SHOW_GLOBAL_STATUS);
            setReplStatus(metric, SHOW_SLAVE_STATUS);
            if (objectName.indexOf(SHOW_GLOBAL_STATUS) != -1) {
                return getGlobalStatusMetric(metric);
            } else if (objectName.indexOf(SHOW_SLAVE_STATUS) != -1) {
                return getSlaveStatusMetric(metric);
            } else if (objectName.indexOf(SLAVE_STATUS) != -1) {
                return getMasterSlaveStatusMetric(metric);
            } else if (objectName.indexOf(SHOW_DATABASES) != -1) {
                return getNumberOfDatabases(metric);
            }
        } catch (Exception e) {
            throw new MetricNotFoundException(
                "Service "+objectName+":"+alias+" not found", e);
        }
        throw new MetricNotFoundException(
            "Service "+objectName+":"+alias+" not found");
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
        final boolean isAvail =
            metric.getAttributeName().equals(Metric.ATTR_AVAIL);
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
        String valColumn = metric.getObjectProperty("value");
        String alias = metric.getAttributeName();
        if (alias.trim().equalsIgnoreCase(Metric.ATTR_AVAIL)) {
            return getAvailability(metric).getValue();
        } else {
            Connection conn = getCachedConnection(metric);
            Double val = Double.valueOf(
                _globalStatus.get(conn, alias, valColumn).toString());
            return val.doubleValue();
        }
    }
    
    private double getSlaveStatusMetric(Metric metric)
        throws NumberFormatException,
               MetricUnreachableException,
               SQLException,
               JDBCQueryCacheException {
        String valColumn = metric.getObjectProperty("value");
        String alias = metric.getAttributeName();
        if (alias.equalsIgnoreCase(BYTES_BEHIND_MASTER)) {
            return getBytesBehindMaster(metric);
        } else if (alias.equalsIgnoreCase(LOG_FILES_BEHIND_MASTER)) {
            return getLogFilesBehindMaster(metric);
        } else if (alias.trim().equalsIgnoreCase(Metric.ATTR_AVAIL)) {
            // XXX need to figure out how to determine if repl is down from slave
            return Metric.AVAIL_UP;
        } else {
            Connection conn = getCachedConnection(metric);
            Double val = Double.valueOf(
                _replStatus.get(conn, "master", valColumn).toString());
            return val.doubleValue();
        }
    }

    private double getBytesBehindMaster(Metric metric)
        throws NumberFormatException,
               SQLException,
               JDBCQueryCacheException,
               MetricUnreachableException {
        Connection conn = getCachedConnection(metric);
        double slaveLogPos = -1d;
        try {
            slaveLogPos = Double.valueOf(_replStatus.get(
                conn, "master", "Exec_Master_Log_Pos").toString()).doubleValue();
        } catch (Exception e) {
            // for 4.0 replication, the cases are different
            slaveLogPos = Double.valueOf(_replStatus.get(
                conn, "master", "Exec_master_log_pos").toString()).doubleValue();
        }
        double masterLogPos = Double.valueOf(_replStatus.get(
            conn, "master", "Read_Master_Log_Pos").toString()).doubleValue();
        return masterLogPos - slaveLogPos;
    }

    private double getLogFilesBehindMaster(Metric metric)
        throws SQLException,
               JDBCQueryCacheException {
        Connection conn = getCachedConnection(metric);
        String masterLogFile =  _replStatus.get(
                conn, "master", "Master_Log_File").toString();
        String slaveLogFile =  _replStatus.get(
                conn, "master", "Relay_Master_Log_File").toString();
        if (masterLogFile.equals(slaveLogFile)) {
            return 0d;
        }
        String[] toks = masterLogFile.split("\\.");
        int masterNum = Integer.valueOf(toks[1]).intValue();
        toks = slaveLogFile.split("\\.");
        int slaveNum = Integer.valueOf(toks[1]).intValue();
        return masterNum - slaveNum;
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

    private void setReplStatus(Metric metric, String showGlobal)
        throws MetricNotFoundException
    {
        if (_replStatus == null) {
            String keyColumn = metric.getObjectProperty("key");
            _replStatus = new JDBCQueryCache(showGlobal, keyColumn, 10000);
        }
    }

    private void setGlobalStatus(Metric metric, String showGlobal)
        throws MetricNotFoundException
    {
        if (_globalStatus == null) {
            String keyColumn = metric.getObjectProperty("key");
            _globalStatus = new JDBCQueryCache(showGlobal, keyColumn, 10000);
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
            stmt.execute(SELECT_VERSION);
            return new MetricValue(MeasurementConstants.AVAIL_UP);
        } catch (SQLException e) {
        } finally {
            // don't close conn, it is cached
            DBUtil.closeStatement(_logCtx, stmt);
        }
        return new MetricValue(MeasurementConstants.AVAIL_DOWN);
    }

    protected Connection getConnection(String url, String user, String password)
        throws SQLException {
        try {
            Driver driver = (Driver)Class.forName(_driver).newInstance();
            final Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);
            return driver.connect(url, props);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage());
        }
    }

    protected String getDefaultURL() {
        // XXX need to add timeout params
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