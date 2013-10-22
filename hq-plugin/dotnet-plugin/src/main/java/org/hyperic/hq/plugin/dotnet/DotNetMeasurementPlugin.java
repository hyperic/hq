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
package org.hyperic.hq.plugin.dotnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class DotNetMeasurementPlugin
        extends Win32MeasurementPlugin {

    private static final String DATA_DOMAIN = ".NET CLR Data";
    private static final String DATA_PREFIX = "SqlClient: ";
    private static final String RUNTIME_NAME = "_Global_";
    private static Log log = LogFactory.getLog(DotNetMeasurementPlugin.class);
    private static final Map<String, List<String>> sqlPidsCache = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> oraclePidsCache = new HashMap<String, List<String>>();
    
    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue val = null;
        if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            val = getPDHMetric(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdhSQLDP")) {
            val = getPDHSQLPDMetric(metric, sqlPidsCache, DotNetDetector.SQL_SERVER_PROVIDER_STR);
        } else if (metric.getDomainName().equalsIgnoreCase("pdhOracleDP")) {
            val = getPDHSQLPDMetric(metric, oraclePidsCache, DotNetDetector.ORACLE_PROVIDER_STR);

        } else {
            try {
                val = super.getValue(metric);
                if (metric.isAvail()) {
                    val = new MetricValue(Metric.AVAIL_UP);
                }
            } catch (MetricNotFoundException ex) {
                if (metric.isAvail()) {
                    val = new MetricValue(Metric.AVAIL_DOWN);
                } else {
                    throw ex;
                }
            }
        }
        return val;
    }

    @Override
    protected String getAttributeName(Metric metric) {
        //avoiding Metric parse errors on ':' in DATA_PREFIX.
        if (metric.getDomainName().equals(DATA_DOMAIN)) {
            return DATA_PREFIX + metric.getAttributeName();
        } else {
            return metric.getAttributeName();
        }
    }

    @Override
    public String translate(String template, ConfigResponse config) {
        if (log.isDebugEnabled()) {
            log.debug("[translate] >> template=" + template);
            for (String key : config.getKeys()) {
                if (key.toLowerCase().startsWith("app")) {
                    log.debug("[translate]  > " + key + "=" + config.getValue(key));
                }
            }
        }

        template = super.translate(template, config);

        template = StringUtil.replace(template, "__percent__", "%");

        // default value for .net server
        final String prop = DotNetDetector.PROP_APP;
        template = StringUtil.replace(template, "${" + prop + "}", config.getValue(prop, RUNTIME_NAME));

        log.debug("[translate] << template=" + template);

        return template;
    }

    private MetricValue getPDHSQLPDMetric(Metric metric, Map<String, List<String>> pidsCache, String providerStr) {
        if (metric.isAvail()) {
            pidsCache.clear();
        }
        if (pidsCache.isEmpty()) {
            try {
                String[] instances = Pdh.getInstances(providerStr);
                Pattern regex = Pattern.compile("([^\\[]*)\\[([^\\]]*)\\]"); // name[pid]
                for (int i = 0; i < instances.length; i++) {
                    String instance = instances[i];
                    log.debug("[getPDHSQLPDMetric] " + providerStr  + " instance = " + instance);
                    Matcher m = regex.matcher(instance);
                    if (m.find()) {
                        String nonTrimmedName = m.group(1);
                        String name = nonTrimmedName.trim();
                        if (name.length() == 0) {
                            continue;
                        }
                        List<String> pids = pidsCache.get(name);
                        if (pids == null) {
                            pids = new ArrayList<String>();
                            pidsCache.put(name, pids);
                        }
                        // nira  oracle is of the form "app [x,y]"
                        //sql is of the form "app[x]
                        // if there are spaces in between we will include them in the pid
                        //(so pid of oracle will be " [x,y]")
                        // the pid of sql server will be "[x]"
                        String spaces = nonTrimmedName.substring(nonTrimmedName.indexOf(name)+name.length());
                        String pid = spaces+"["+m.group(2)+"]";
                        pids.add(pid);                        
                    }
                }
            } catch (Win32Exception e) {
                log.debug("Error getting PIDs data for '.NET Data Provider for SqlServer': " + e, e);
            }
            log.debug("[getPDHSQLPDMetric] pidsCache = " + pidsCache);            
        }

        log.debug("[getPDHSQLPDMetric] metric:'" + metric);
        String appName = metric.getObjectPropString();
        List<String> pids = pidsCache.get(appName);
        MetricValue res;
        if (pids == null) {
            pidsCache.clear();
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
            } else {
                res = MetricValue.NONE;
            }
        } else {
            if (metric.isAvail()) {
                res = new MetricValue(pids.size() > 0 ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);
            } else if (metric.getAttributeName().equalsIgnoreCase("instances")) {
                res = new MetricValue(pids.size());
            } else if (pids.isEmpty()) {
                res = MetricValue.NONE;
            } else {
                double val = 0;
                for (int i = 0; i < pids.size(); i++) {
                    String pid = pids.get(i); 
                    String obj = "\\" + providerStr + "(" + appName +  pid + ")\\" + metric.getAttributeName();
                    log.debug("[getPDHSQLPDMetric] obj:'" + obj);
                    val += getPDHMetric(obj, metric.isAvail()).getValue();
                }
                res = new MetricValue(val);
            }
        }
        return res;
    }

    private MetricValue getPDHMetric(Metric metric) {
        String obj = "\\" + metric.getObjectPropString();
        if (!metric.isAvail()) {
            obj += "\\" + metric.getAttributeName();
        }
        log.debug("[getPDHMetric] metric:'" + metric);
        log.debug("[getPDHMetric] obj:'" + obj);
        return getPDHMetric(obj, metric.isAvail());
    }

    private MetricValue getPDHMetric(String obj, boolean avail) {
        MetricValue res;
        try {
            Double val = new Pdh().getFormattedValue(obj);
            res = new MetricValue(val);
            if (avail) {
                res = new MetricValue(Metric.AVAIL_UP);
            }
        } catch (Win32Exception ex) {
            if (avail) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                log.debug("[getPDHMetric] error on obj:'" + obj + "' :" + ex.getLocalizedMessage(), ex);
            } else {
                res = MetricValue.NONE;
                log.debug("[getPDHMetric] error on obj:'" + obj + "' :" + ex.getLocalizedMessage());
            }
        }
        return res;
    }
}
