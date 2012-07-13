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
package org.hyperic.hq.plugin.postgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.jmx.SigarInvokerJMX;
import org.hyperic.util.jdbc.DBUtil;

public class ServerMeasurement extends SigarMeasurementPlugin {

    private Log log = LogFactory.getLog(ServerMeasurement.class);
    private long pid = -1;
    private Sigar sigar = null;
    private SigarProxy sigarProxy = null;
    private String actualConfig = "";

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        if (metric.getDomainName().equals("collector")) {
            return Collector.getValue(this, metric);
        }

        MetricValue res;

        initSigar();

        log.debug("[getValue] metric=" + metric);
        try {
            findPid(metric);
        } catch (PluginException ex) {
            if (metric.isAvail()) {
                log.debug(ex, ex);
                return new MetricValue(Metric.AVAIL_DOWN);
            } else {
                throw ex;
            }
        }

        if (metric.isAvail()) {
            res = new MetricValue(Metric.AVAIL_UP);
        } else {
            String oName = "sigar:Type=" + metric.getObjectProperty("Type") + ",Arg=State.Ppid.eq=" + pid;
            log.debug("[getValue] oName=" + oName);
            String attr = metric.getAttributeName();
            SigarInvokerJMX invoker = SigarInvokerJMX.getInstance(this.sigarProxy, oName);
            try {
                Object systemValue;
                synchronized (this.sigar) {
                    systemValue = invoker.invoke(attr);
                }
                if (systemValue instanceof Double) {
                    res = new MetricValue((Double) systemValue);
                } else if (systemValue instanceof Long) {
                    res = new MetricValue(new Double(((Long) systemValue).longValue()));
                } else if (systemValue instanceof Integer) {
                    res = new MetricValue(new Double(((Integer) systemValue).intValue()));
                } else {
                    PluginException ex = new PluginException("Error on systemValue '" + systemValue + "' (" + systemValue.getClass() + ")");
                    log.debug(ex, ex);
                    throw ex;
                }
            } catch (SigarNotImplementedException e) {
                pid = -1;
                throw new MetricUnreachableException(e.getMessage(), e);
            } catch (SigarException e) {
                pid = -1;
                throw new MetricNotFoundException(e.getMessage(), e);
            }
        }
        return res;
    }

    protected void initSigar() throws PluginException {
        if (this.sigar == null) {
            try {
                this.sigar = new Sigar();
                this.sigarProxy = SigarProxyCache.newInstance(sigar);
            } catch (UnsatisfiedLinkError ex) {
                log.debug("unable to load sigar: " + ex.getMessage(), ex);
                this.sigar = null;
                this.sigarProxy = null;
                throw new PluginException(ex.getMessage(), ex);
            }
        }

    }

    private void findPid(Metric metric) throws PluginException {
        Properties props = metric.getProperties();
        String user = props.getProperty(PostgreSQL.PROP_USER);
        String pass = props.getProperty(PostgreSQL.PROP_PASS);
        String url = PostgreSQL.prepareUrl(props, null);

        String newConfig = url + "-" + user + "-" + pass;
        boolean isNewConfig = !newConfig.equalsIgnoreCase(actualConfig);
        if (isNewConfig) {
            log.debug("[findPid] new config detected");
            actualConfig = newConfig;
        }

        // look for the PID 
        // IF config have changed (to test is the new config is valid)
        // OR no PID
        // OR always for Availability
        if (isNewConfig || (pid == -1) || (metric.isAvail())) {

            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                conn = DriverManager.getConnection(url, user, pass);
                stmt = conn.createStatement();
                stmt.execute("select pg_backend_pid()");

                rs = stmt.getResultSet();
                if (rs.next()) {
                    int conectionPid = rs.getInt(1);
                    pid = new Sigar().getProcState(conectionPid).getPpid();
                    log.debug("[findPid] conection PID:" + conectionPid + " ==> main Process PID:" + pid);
                }
            } catch (SQLException ex) {
                pid = -1;
                throw new PluginException(ex.getMessage(), ex);
            } catch (SigarException ex) {
                pid = -1;
                throw new PluginException(ex.getMessage(), ex);
            } finally {
                DBUtil.closeJDBCObjects(getLog(), conn, stmt, rs);
            }
        }
    }
}
