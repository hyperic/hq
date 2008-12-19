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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.JDBCQueryCache;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.jdbc.DBUtil;

public class MySqlStatsMeasurementPlugin
    extends JDBCMeasurementPlugin
{
    private static final String _logCtx =
        MySqlStatsMeasurementPlugin.class.getName();
    private final Log _log = LogFactory.getLog(MySqlStatsMeasurementPlugin.class);
    static final String PROP_JDBC_DRIVER = "DEFAULT_DRIVER",
                        DEFAULT_DRIVER   = "com.mysql.jdbc.Driver";
    private static final String SLAVE_RUNNING      = "Slave_Running",
                                SHOW_SLAVE_STATUS  = "show slave status",
                                SHOW_GLOBAL_STATUS =
                                    "show /*!50002 global */ status";
    private String _driver;
    private JDBCQueryCache _globalStatus = null,
                           _replStatus   = null;
    
    protected double getQueryValue(Metric metric)
        throws MetricNotFoundException,
               PluginException,
               MetricUnreachableException {
        setDriver(metric);
        String keyColumn = metric.getObjectProperty("key");
        String valColumn = metric.getObjectProperty("value");
        setGlobalStatus(metric, SHOW_GLOBAL_STATUS, keyColumn);
        setReplStatus(metric, SHOW_SLAVE_STATUS, keyColumn);
        // will look like "mysqlstats:Type=Global,jdbcUser=user,jdbcPasswd=pass"
        String objectName = metric.getObjectName().toLowerCase();
        // will look like "availability"
        String alias = metric.getAttributeName();
        try {
            if (objectName.indexOf(SHOW_GLOBAL_STATUS) != -1) {
                if (alias.trim().equalsIgnoreCase(Metric.ATTR_AVAIL)) {
                    return getAvailability(metric).getValue();
                } else {
                    Connection conn = getCachedConnection(metric);
                    Double val = Double.valueOf(
                        _globalStatus.get(conn, alias, valColumn).toString());
                    return val.doubleValue();
                }
            } else if (objectName.indexOf(SHOW_SLAVE_STATUS) != -1) {
                Connection conn = getCachedConnection(metric);
                if (alias.trim().equalsIgnoreCase(Metric.ATTR_AVAIL)) {
                    Double val = Double.valueOf(
                        _replStatus.get(conn, SLAVE_RUNNING, valColumn).toString());
                    return val.doubleValue();
                } else {
                    Double val = Double.valueOf(
                        _replStatus.get(conn, alias, valColumn).toString());
                    return val.doubleValue();
                }
            }
        } catch (Exception e) {
            throw new MetricNotFoundException(
                "Service "+objectName+":"+alias+" not found", e);
        }
        throw new MetricNotFoundException(
            "Service "+objectName+":"+alias+" not found");
    }

    private void setReplStatus(Metric metric, String showGlobal,
                               String keyColumn)
        throws MetricNotFoundException
    {
        if (_replStatus == null) {
            _replStatus = new JDBCQueryCache(showGlobal, keyColumn, 10000);
        }
    }

    private void setGlobalStatus(Metric metric, String showGlobal,
                                 String keyColumn)
        throws MetricNotFoundException
    {
        if (_globalStatus == null) {
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
            stmt.execute("select @@version");
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

    protected String getQuery(Metric jdsn) {
        return null;
    }

    protected void initQueries() {
    }

    protected void getDriver() throws ClassNotFoundException {
    }
}