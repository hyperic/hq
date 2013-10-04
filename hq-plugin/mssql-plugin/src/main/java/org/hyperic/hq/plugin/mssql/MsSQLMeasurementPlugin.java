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
package org.hyperic.hq.plugin.mssql;

import java.util.Properties;
import java.util.List;
import java.util.Arrays;
import java.util.Enumeration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin
        extends Win32MeasurementPlugin {

    private static Log log = LogFactory.getLog(MsSQLMeasurementPlugin.class);
    private static final String MSSQL_LOGIN_TIMEOUT = "mssql.login_timeout";
    private static final String ATTR_NAME_DATABASE_FREE_PERCENT = "Database Free Percent";
    private static final String ATTR_NAME_DATABASE_FREE_PERCENT_2000 = "Database Free Percent 2000";
    private static final String MDF_FREE_SPACE_PCT2005_SQL = "MDF_FreeSpacePct2005.sql";
    private static final String MDF_FREE_SPACE_PCT2000_SQL = "MDF_FreeSpacePct2000.sql";
    private static final String PROP_LOCK = "lock.name";
    private static final String PROP_CACHE = "cache.name";
    private static final String TOTAL_NAME = "_Total";
    static final String DEFAULT_SQLSERVER_METRIC_PREFIX = "SQLServer";
    static final String DEFAULT_SQLAGENT_METRIC_PREFIX = "SQLAgent";

    private String getServiceName(Metric metric) {

        // For the SQLServer: 
        // the sqlServerServiceName will be "MSSQLSERVER" (for default instance name)
        // or "MSSQL$<given_instance_name>" (for given instance name)

        // For the SQLAgent:
        // the service name will be "SQLSERVERAGENT" (for default instance name)
        // or "SQLAgent$<given_instance_name>" (for given instance name)
        Properties props = metric.getProperties();
        String sqlServiceName = props.getProperty(Win32ControlPlugin.PROP_SERVICENAME,
                MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME);

        return sqlServiceName;
    }

    protected String getDomainName(Metric metric) {
        String fullPrefix = "";
        String serviceName = getServiceName(metric);

        if (serviceName.equalsIgnoreCase(MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME)) {
            // service name is "MSSQLSERVER"  so this is a default instance and  
            // the perfmon metric name will be prefixed by "SQLSERVER:"
            fullPrefix = DEFAULT_SQLSERVER_METRIC_PREFIX;
        } else {
            if (serviceName.equalsIgnoreCase(MsSQLDetector.DEFAULT_SQLAGENT_SERVICE_NAME)) {
                // service name is "SQLSERVERAGENT"  so this is a default instance and  
                // the perfmon metric name will be prefixed by "SQLAgent:"
                fullPrefix = DEFAULT_SQLAGENT_METRIC_PREFIX;
            } else {
                // service name is not one of the above so this is not a default instance
                // the perfmon metric name will be prefixed by the service name
                // i.e. something like "MSSQL$<instance_name>" or "SQLAgent$<instance_name>
                fullPrefix = serviceName;
            }
        }

        return fullPrefix + ":" + metric.getDomainName();
    }

    protected double adjustValue(Metric metric, double value) {
        if (metric.getAttributeName().startsWith("Percent")) {
            value /= 100;
        }

        return value;
    }

    public String translate(String template, ConfigResponse config) {
        template = super.translate(template, config);
        log.debug("[translate] > template = "+template);
        if (template.contains(":collector:")) {
            int lastSemiColon = template.lastIndexOf(':');
            template = template.substring(0, lastSemiColon)+','+template.substring(lastSemiColon+1);
        }
        log.debug("[translate] < template = "+template);
        return template;
    }

    private static int getServiceStatus(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            return svc.getStatus();
        } catch (Win32Exception e) {
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    private String getServerName(Metric metric) {
        String serverName = metric.getObjectProperty("ServerName");
        getLog().debug("ServerName from config=" + serverName);
        // there is bug causing the default not to be set for sqlserver_name
        if (serverName == null || "".equals(serverName) || "%sqlserver_name%".equals(serverName)) {
            serverName = "localhost";
            getLog().debug("Setting serverName to default=" + serverName);
        }
        return serverName;
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {

        if (metric.getDomainName().equalsIgnoreCase("collector")) {
            return Collector.getValue(this, metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            return getPDHMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdh2")) {
            return getPDHInstaceMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdhDBAvail")) {
            return getPDHDBAvailMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("service")) {
            return checkServiceAvail(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("mssql")) {
            if (metric.getObjectPropString().equals("process")) {
                return getInstanceProcessMetric(metric);
            }
            getLog().debug("Unable to retrieve value for: " + metric);
            System.exit(-1);
            return MetricValue.NONE;
        } else {
            getLog().debug("Unable to retrieve value for: " + metric.getAttributeName());
            System.exit(-1);
            return MetricValue.NONE;
        }
    }

    private MetricValue getInstanceProcessMetric(Metric metric) {
        try {
            log.debug("[gipm] metric='" + metric + "'");
            String serviceName = metric.getProperties().getProperty("service_name");
            Sigar sigar = new Sigar();
            long servicePID = sigar.getServicePid(serviceName);
            log.debug("[gipm] serviceName='" + serviceName + "' servicePID='" + servicePID + "'");

            List<String> instances = Arrays.asList(PDH.getInstances("Process"));
            String serviceInstance = null;
            for (int i = 0; (i < instances.size()) && (serviceInstance == null); i++) {
                String instance = instances.get(i);
                if (instance.startsWith("sqlservr")) {
                    String obj = "\\Process(" + instance + ")\\ID Process";
                    log.debug("[gipm] obj='" + obj + "'");
                    double pid = PDH.getValue(obj);
                    if (pid == servicePID) {
                        serviceInstance = instance;
                        log.debug("[gipm] serviceName='" + serviceName + "' serviceInstance='" + serviceInstance + "'");
                    }
                }
            }

            if (serviceInstance != null) {
                String obj = "\\Process(" + serviceInstance + ")\\" + metric.getAttributeName();
                log.debug("[gipm] obj = '" + obj + "'");

                double res = PDH.getValue(obj);
                log.debug("[getPDH] obj:'" + obj + "' val:'" + res + "'");

                return new MetricValue(res);
            } else {
                log.debug("[gipm] Process for serviceName='" + serviceName + "' not found, returning " + MetricValue.NONE.getValue());
                return MetricValue.NONE;
            }

        } catch (Exception ex) {
            log.debug("[gipm] " + ex, ex);
            return MetricValue.NONE;
        }
    }

    private MetricValue checkServiceAvail(Metric metric) {
        String service = metric.getObjectProperty("service_name");
        log.debug("[checkServiceAvail] service='" + service + "'");
        double res = Metric.AVAIL_DOWN;
        try {
            if (service != null) {
                Service s = new Service(service);
                if (s.getStatus() == Service.SERVICE_RUNNING) {
                    res = Metric.AVAIL_UP;
                }
                log.debug("[checkServiceAvail] service='" + service + "' metric:'" + metric + "' res=" + res);
            }
        } catch (Win32Exception ex) {
            log.debug("[checkServiceAvail] error. service='" + service + "' metric:'" + metric + "'", ex);
        }
        return new MetricValue(res);
    }

    private MetricValue getPDHDBAvailMetric(Metric metric) {
        String dbName = metric.getObjectProperty("db.name");
        String service = metric.getProperties().getProperty("service_name");
        if (MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equalsIgnoreCase(service)) {
            log.debug("[getPDHDBAvailMetric] service='" + service + "' ==> ='" + MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME + "''");
            service = DEFAULT_SQLSERVER_METRIC_PREFIX;
        }
        String obj = service + ":Databases";
        log.debug("[getPDHDBAvailMetric] dbName='" + dbName + "' service='" + service + "' obj='" + obj + "'");
        double res = Metric.AVAIL_DOWN;
        try {
            if (dbName != null) {
                List<String> instances = Arrays.asList(PDH.getInstances(obj));
                if (instances.contains(dbName)) {
                    res = Metric.AVAIL_UP;
                }
                log.debug("[getPDHDBAvailMetric] service='" + service + "' dbName:'" + dbName + "' res=" + res);
            }
        } catch (PluginException ex) {
            log.debug("[getPDHDBAvailMetric] error. service='" + service + "' dbName:'" + dbName + "'", ex);
        }
        return new MetricValue(res);
    }

    private MetricValue getPDHInstaceMetric(Metric metric) {
        String obj = "\\" + metric.getObjectPropString();
        obj += "\\" + metric.getAttributeName();

        Enumeration<Object> ks = metric.getProperties().keys();
        while (ks.hasMoreElements()) {
            String k = (String) ks.nextElement();
            String v = metric.getProperties().getProperty(k);
            obj = obj.replaceAll("%" + k + "%", v);
        }

        getPDH(obj, metric);
        return getPDH(obj, metric);
    }

    private MetricValue getPDHMetric(Metric metric) {
        String prefix = metric.getProperties().getProperty("pref_prefix");
        if (prefix == null) {
            prefix = metric.getProperties().getProperty("service_name");
        }

        if (MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equalsIgnoreCase(prefix)) {
            prefix = DEFAULT_SQLSERVER_METRIC_PREFIX;
        }

        String obj = "\\" + prefix + ":" + metric.getObjectPropString();

        if (!metric.isAvail()) {
            obj += "\\" + metric.getAttributeName();
        }

        return getPDH(obj, metric);
    }

    private MetricValue getPDH(String obj, Metric metric) {
        MetricValue res;
        try {
            double val = PDH.getValue(obj);
            log.debug("[getPDH] obj:'" + obj + "' val:'" + val + "'");
            res = new MetricValue(val);
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_UP);
            }
        } catch (Exception ex) {
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                log.debug("[getPDH] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage(), ex);
            } else {
                res = MetricValue.NONE;
                log.debug("[getPDH] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage());
            }
        }
        return res;
    }
}
