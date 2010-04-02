/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.sybase;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public class SybaseSysmonCollector extends Collector {

    static Log trace = LogFactory.getLog("trace." + SybaseSysmonCollector.class);
    static Log log = LogFactory.getLog(SybaseSysmonCollector.class);
    private CallableStatement stmt;
    private Connection conn = null;

    protected void init() throws PluginException {
        Properties props = getProperties();

        String url = props.getProperty(JDBCMeasurementPlugin.PROP_URL, ""),
                user = props.getProperty(JDBCMeasurementPlugin.PROP_USER, ""),
                pass = props.getProperty(JDBCMeasurementPlugin.PROP_PASSWORD, "");
        
        try {
            Connection conn = createConnection(url, user, pass);
        } catch(SQLException e) {
            throw new PluginException(new MetricUnreachableException("Could not connect using information provided", e));
        } finally {
            if (conn != null) {
                DBUtil.closeJDBCObjects(log, conn, null, null);
                conn = null;
            }
        }
        
        super.init();
    }

    public void collect() {

        Properties props = getProperties();

        String url = props.getProperty(JDBCMeasurementPlugin.PROP_URL, ""),
                user = props.getProperty(JDBCMeasurementPlugin.PROP_USER, ""),
                pass = props.getProperty(JDBCMeasurementPlugin.PROP_PASSWORD, "");
        try {
            setAvailability(Metric.AVAIL_DOWN);
            if (conn == null) {
                conn = createConnection(url, user, pass);
            }
            stmt = conn.prepareCall("{call sp_sysmon '" + props.getProperty("interval") + "'}");
            stmt.executeUpdate();

            StringBuffer response = new StringBuffer();
            SQLWarning war = stmt.getWarnings();
            do {
                response.append(war.getMessage()).append("\n");
                war = war.getNextWarning();
            } while (war != null);
            trace.debug(response);

            String res = response.toString();

            Pattern pat = Pattern.compile("\n +Cache:(.*)\n");
            Matcher m = pat.matcher(res);
            while (m.find()) {
                final String cacheName = m.group(1).trim().replaceAll(" ", "_");
                trace.debug("->'" + cacheName + "'");
                trace.debug("->" + m.start());
                String sec = res.substring(m.start());
                setValue(cacheName + ".Availability", Metric.AVAIL_UP);
                setValue(cacheName + ".CacheHitsRatio", get(sec, "Cache Hits", 5) / 100);
                setValue(cacheName + ".CacheMissesRatio", get(sec, "Cache Misses", 5) / 100);
            }

            setValue("Deadlocks", get(res, "Deadlock Percentage", 5));
            setValue("TotalLockReqs", get(res, "Total Lock Requests", 5));
            setValue("AvgLockContention", get(res, "Avg Lock Contention", 5));
            setValue("TotalCacheHitsRatio", get(res, "Total Cache Hits", 6) / 100);
            setValue("TotalCacheMissesRatio", get(res, "Total Cache Misses", 6) / 100);
            setValue("TDSPacketsReceived", get(res, "Total TDS Packets Rec'd", 6) / 100);
            setValue("TDSPacketsSent", get(res, "Total Bytes Rec'd", 5) / 100);
            setAvailability(Metric.AVAIL_UP);

        } catch (SQLException e) {
            setValue("Availability", Metric.AVAIL_DOWN);
            log.debug("[collect] Error " + e.getMessage());
            log.debug("[collect] Error " + getResult().toString());
            if (conn != null) {
                DBUtil.closeJDBCObjects(log, conn, null, null);
                conn = null;
            }
        } finally {
            if (conn != null) {
                DBUtil.closeJDBCObjects(log, null, stmt, null);
            }
        }

    }

    public MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = super.getValue(metric, result);

        if (metric.isAvail() && res.isNone()) {
            res = new MetricValue(Metric.AVAIL_DOWN);
        }

        return res;
    }

    private double get(String txt, String pro, int index) {
        double res = Double.NaN;
        String[] vals = null;
        try {
            Pattern pat = Pattern.compile(".*" + pro + ".*\r?\n");
            Matcher lm = pat.matcher(txt);
            if (lm.find()) {
                String line = lm.group();
                log.debug(line);
                line = line.trim().replaceAll(" +", " ");
                vals = line.split(" ");
                log.debug(line);
                log.debug(Arrays.asList(vals));
                if (!vals[index].equals("n/a")) {
                    res = Double.parseDouble(vals[index]);
                } else {
                    res = 0;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            res = Double.NaN;
            log.debug("vals=> '" + Arrays.asList(vals) + "' pro='" + pro + "' index='" + index + "'");
        }
        return res;
    }

    protected Connection createConnection(String url, String user, String password) throws SQLException {
        String pass = (password == null) ? "" : password;
        pass = (pass.matches("^\\s*$")) ? "" : pass;
        java.util.Properties props = new java.util.Properties();
        props.put("CHARSET_CONVERTER_CLASS", "com.sybase.jdbc3.utils.TruncationConverter");
        props.put("user", user);
        props.put("password", pass);
        return DriverManager.getConnection(url, props);
    }
}
