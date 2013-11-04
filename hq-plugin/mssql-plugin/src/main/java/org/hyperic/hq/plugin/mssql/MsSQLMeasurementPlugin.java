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

import java.util.List;
import java.util.Arrays;
import java.util.Enumeration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.MeasurementPlugin;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin extends MeasurementPlugin {

    private static Log log = LogFactory.getLog(MsSQLMeasurementPlugin.class);
    static final String DEFAULT_SQLSERVER_METRIC_PREFIX = "SQLServer";
    static final String DEFAULT_SQLAGENT_METRIC_PREFIX = "SQLAgent";

    @Override
    public String translate(String template, ConfigResponse config) {
        template = super.translate(template, config);
        log.debug("[translate] > template = " + template);
        if (template.contains(":collector:")) {
            int lastSemiColon = template.lastIndexOf(':');
            template = template.substring(0, lastSemiColon) + ',' + template.substring(lastSemiColon + 1);
        }
        log.debug("[translate] < template = " + template);
        return template;
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
            return MetricValue.NONE;
        } else if (metric.getDomainName().equalsIgnoreCase("dfp")) {
            return Collector.getValue(this, metric);
        } else {
            getLog().debug("Unable to retrieve value for: " + metric.getAttributeName());
            return MetricValue.NONE;
        }
    }

    @Override
    public Collector getNewCollector() {
        if (!getPluginData().getPlugin("collector", "MsSQL 2012 Database").equals(MsSQLDataBaseCollector.class.getName())) {
            getPluginData().addPlugin("collector", "MsSQL 2012 Database", MsSQLDataBaseCollector.class.getName());
            getPluginData().addPlugin("collector", "MsSQL 2008 Database", MsSQLDataBaseCollector.class.getName());
            getPluginData().addPlugin("collector", "MsSQL 2008 R2 Database", MsSQLDataBaseCollector.class.getName());
            getPluginData().addPlugin("collector", "MsSQL 2005 Database", MsSQLDataBaseCollector.class.getName());
        }
        Collector c = super.getNewCollector();
        getLog().debug("[getNewCollector] t:'" + getTypeInfo().getName() + "' c:" + c.getClass().getName());
        return c;
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
