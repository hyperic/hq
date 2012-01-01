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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public class SybaseSysmonCollector extends Collector {

    public static final String INTERVAL = "interval";
    static Log trace = LogFactory.getLog("trace." + SybaseSysmonCollector.class);
    static Log log = LogFactory.getLog(SybaseSysmonCollector.class);
    private CallableStatement stmt;
    private Connection conn = null;

    protected void init() throws PluginException {
        Properties props = getProperties();

        Connection c = null;
        ResultSet rs = null;
        CallableStatement st = null;
        String sa_role = SybaseProductPlugin.getSaRole();

        try {
            c = createConnection(props);
            st = c.prepareCall("{call sp_displayroles}");
            rs = st.executeQuery();
            boolean roleOK = false;
            while (rs.next() && !roleOK) {
                roleOK = rs.getString(1).equals(sa_role);
            }
            if (!roleOK) {
                throw new PluginException("Could not connect using information provided: The user must have System Administrator ('" + sa_role + "') role");
            }
        } catch (SQLException e) {
            throw new PluginException("Could not connect using information provided", e);
        } finally {
                DBUtil.closeJDBCObjects(log, c, st, rs);
        }

        String interval = props.getProperty(SybaseSysmonCollector.INTERVAL);
        Pattern p = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d$");
        Matcher m = p.matcher(interval);
        if (!m.matches()) {
            String msg = "Configuration failed: bad INTERVAL format ##:##:## (" + interval + ")";
            throw new PluginException(msg);
        }

        super.init();
    }

    public void collect() {
        Properties props = getProperties();
        log.debug("[collect] props=" + props);

        try {
            setAvailability(Metric.AVAIL_DOWN);
            if (conn == null) {
                conn = createConnection(props);
            }
            stmt = conn.prepareCall("{call sp_sysmon '" + props.getProperty(INTERVAL) + "'}");
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
                if (trace.isDebugEnabled()) {
                    trace.debug("->'" + cacheName + "'");
                    trace.debug("->" + m.start());
                }
                String sec = res.substring(m.start());
                setValue(cacheName + ".Availability", Metric.AVAIL_UP);
                setValue(cacheName + ".CacheHitsRatio", get(sec, "Cache Hits", 5) / 100);
                setValue(cacheName + ".CacheMissesRatio", get(sec, "Cache Misses", 5) / 100);
            }
            
            // output per engine:
            // Engine 0                        0.0 %      0.0 %    100.0 %
            //
            // regex should only find lines starting with "Engine X                        X.X %"
            // engineid and percentage are in regex groups 1 and 2
            pat = Pattern.compile("\n +Engine (\\d)+\\s+(\\d+\\.\\d+) %.*");
            m = pat.matcher(res);
            while (m.find()) {
                try {
                    final String engineId = m.group(1);
                    final String cpuBusyVal = m.group(2);
                    if (engineId != null && cpuBusyVal != null) {
                        setValue("EngineUtilization" + engineId.trim(),
                            Double.parseDouble(cpuBusyVal.trim()) / 100);
                    }
                    if (trace.isDebugEnabled()) {
                        trace.debug("Found Engine Utilization for engineid=" + engineId.trim() +
                            " with value " + Double.parseDouble(cpuBusyVal.trim()) / 100);                        
                    }
                } catch (NumberFormatException e) {
                    if (trace.isDebugEnabled()) {
                        trace.debug("Unable to parse number from: " + e.toString());                        
                    }
                } catch (IndexOutOfBoundsException e) {
                    if (trace.isDebugEnabled()) {
                        trace.debug("Unable to find group from matcher: " + e.toString());                        
                    }
                }
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
                line = line.trim().replaceAll(" +", " ");
                vals = line.split(" ");
                if (trace.isDebugEnabled()) {
                    trace.debug(line);
                    trace.debug(line);
                    trace.debug(Arrays.asList(vals));
                }
                if (!vals[index].equals("n/a")) {
                    res = Double.parseDouble(vals[index]);
                } else {
                    res = 0;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            res = Double.NaN;
            trace.debug("vals=> '" + Arrays.asList(vals) + "' pro='" + pro + "' index='" + index + "'");
        }
        return res;
    }

    protected Connection createConnection(Properties p) throws SQLException {
        String url = p.getProperty(JDBCMeasurementPlugin.PROP_URL, "");
        String user = p.getProperty(JDBCMeasurementPlugin.PROP_USER, "");
        String pass = p.getProperty(JDBCMeasurementPlugin.PROP_PASSWORD, "");
        pass = (pass.matches("^\\s*$")) ? "" : pass;
        Properties props = new java.util.Properties();
        props.put("CHARSET_CONVERTER_CLASS", "com.sybase.jdbc3.utils.TruncationConverter");
        props.put("user", user);
        props.put("password", pass);
        return DriverManager.getConnection(url, props);
    }
}
