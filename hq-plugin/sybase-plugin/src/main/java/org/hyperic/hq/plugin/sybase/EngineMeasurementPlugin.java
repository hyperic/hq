/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.sybase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public class EngineMeasurementPlugin extends SigarMeasurementPlugin {

    private static final HashMap connectionCache = new HashMap();
    private static final HashMap pidsCache = new HashMap();
    private static final String SIGAR_DOMAIN = "sybase.engine.sigar";

    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res;
        getLog().debug("[getValue] metric=" + metric);
        if (metric.getDomainName().equals(SIGAR_DOMAIN)) {
            res = getSigarValue(metric);
        } else if (metric.getAttributeName().equals(Metric.ATTR_AVAIL)) {
            res = getAvailability(metric);
        } else if (metric.getAttributeName().startsWith("EngineUtilization")) {
            res =  Collector.getValue(this, metric);
        } else {
            throw new MetricNotFoundException(metric.getAttributeName());
        }
        return res;
    }

    private MetricValue getAvailability(Metric metric) throws MetricNotFoundException {
        Statement stmt = null;
        ResultSet rs = null;
        Properties props = metric.getProperties();
        String eId = metric.getObjectProperties().getProperty("id");
        double res = Metric.AVAIL_DOWN;
        Connection conn = null;
        String url = props.getProperty(JDBCMeasurementPlugin.PROP_URL, "");
        String user = props.getProperty(JDBCMeasurementPlugin.PROP_USER, "");
        String pass = props.getProperty(JDBCMeasurementPlugin.PROP_PASSWORD, "");
        try {
            conn = getCachedConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select status from sysengines where engine=" + eId);
            if (rs.next()) {
                String status = rs.getString("status").trim();
                getLog().debug("[getAvailability] egine '" + eId + "' status='" + status + "'");
                if (status.equalsIgnoreCase("online")) {
                    res = Metric.AVAIL_UP;
                } else {
                    res = Metric.AVAIL_WARN;
                }
            } else {
                getLog().debug("[getAvailability] egine '" + eId + "' status not found");
            }
        } catch (SQLException e) {
            getLog().error("Error getting engine '" + eId + "' pid -> " + e.getMessage());
            DBUtil.closeJDBCObjects(getLog(), conn, null, null);
            removeCachedConnection(url, user, pass);
            conn=null;
        } finally {
            if (conn != null) {
                DBUtil.closeJDBCObjects(getLog(), null, stmt, rs);
            }
        }
        return new MetricValue(res);
    }

    public MetricValue getSigarValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = null;
        String eId = metric.getProperties().getProperty("id");
        String ePid = (String) pidsCache.get(eId);
        if (ePid == null) {
            ePid = getEnginePID(metric.getProperties());
            pidsCache.put(eId, ePid);
            getLog().debug("Engine '" + eId + "' pid='" + ePid + "'");
        }
        if (ePid == null) {
            throw new MetricNotFoundException("engine '" + eId + "' pid not found");
        }
        metric.setObjectName(metric.getObjectName().replace("%process.query%", ePid));
        try {
            res = super.getValue(metric);
        } catch (PluginException ex) {
            pidsCache.remove(eId);
            throw ex;
        } catch (MetricNotFoundException ex) {
            pidsCache.remove(eId);
            throw ex;
        } catch (MetricUnreachableException ex) {
            pidsCache.remove(eId);
            throw ex;
        }
        return res;
    }

    private String getEnginePID(Properties props) {
        Statement stmt = null;
        ResultSet rs = null;
        String pid = null;
        String eId = props.getProperty("id");
        try {
            String url = props.getProperty(JDBCMeasurementPlugin.PROP_URL, ""),
                    user = props.getProperty(JDBCMeasurementPlugin.PROP_USER, ""),
                    pass = props.getProperty(JDBCMeasurementPlugin.PROP_PASSWORD, "");
            Connection conn = getCachedConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select osprocid from sysengines where engine=" + eId);
            if (rs.next()) {
                pid = rs.getString("osprocid");
            }
        } catch (SQLException e) {
            getLog().debug("Error getting engine '" + eId + "' pid");
        } finally {
            DBUtil.closeJDBCObjects(getLog(), null, stmt, rs);
        }
        return pid;
    }

    protected Connection getCachedConnection(String url, String user, String pass) throws SQLException {
        String cacheKey = url + user + pass;
        Connection conn = null;

        synchronized (connectionCache) {
            conn = (Connection) connectionCache.get(cacheKey);

            if (conn == null) {
                conn = getConnection(url, user, pass);
                connectionCache.put(cacheKey, conn);
            }
        }

        return conn;
    }

    protected void removeCachedConnection(String url, String user, String pass) {
        String cacheKey = url + user + pass;
        synchronized (connectionCache) {
            connectionCache.remove(cacheKey);
        }
    }

    protected Connection getConnection(String url, String user, String password) throws SQLException {
        String pass = (password == null) ? "" : password;
        pass = (pass.matches("^\\s*$")) ? "" : pass;
        Properties props = new Properties();
        props.put("CHARSET_CONVERTER_CLASS", "com.sybase.jdbc3.utils.TruncationConverter");
        props.put("user", user);
        props.put("password", pass);
        return DriverManager.getConnection(url, props);
    }

    public String translate(String template, ConfigResponse config) {
        String rate = "";
        if (template.contains(":sigar:") && template.contains("%process.query%")) {
            template = template.replace(":sigar:", ":" + SIGAR_DOMAIN + ":");
            int ri = template.indexOf("__RATE__");
            if (ri > 0) {
                rate = template.substring(ri);
                template = template.substring(0, ri);
            }
            template += ",id=" + config.getValue("id") + rate;
        } else if (template.contains(":EngineUtilization")) {
            // need to postfix attribute with engineid from configuration
            // because collector collects multiple values
            template = template.replace(":EngineUtilization",
                ":EngineUtilization" + config.getValue("id"));
        }

        return super.translate(template, config);
    }
}
