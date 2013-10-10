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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.MeasurementPlugin;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin extends MeasurementPlugin {

    private static Log log = LogFactory.getLog(MsSQLMeasurementPlugin.class);
    private static final String MSSQL_LOGIN_TIMEOUT = "mssql.login_timeout";
    private static final String ATTR_NAME_DATABASE_FREE_PERCENT = "Database Free Percent";
    private static final String ATTR_NAME_DATABASE_FREE_PERCENT_2000 = "Database Free Percent 2000";
    private static final String MDF_FREE_SPACE_PCT2005_SQL = "MDF_FreeSpacePct2005.sql";
    private static final String MDF_FREE_SPACE_PCT2000_SQL = "MDF_FreeSpacePct2000.sql";
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
            return MetricValue.NONE;
        } else {
            // This metric requires SQL query, not available via perflib
            if (metric.getAttributeName().startsWith(ATTR_NAME_DATABASE_FREE_PERCENT)) {
                if (ATTR_NAME_DATABASE_FREE_PERCENT_2000.equals(metric.getAttributeName())) {
                    return getUnallocatedSpace(metric, MDF_FREE_SPACE_PCT2000_SQL);
                } else {
                    return getUnallocatedSpace(metric, MDF_FREE_SPACE_PCT2005_SQL);
                }
            } else {
                getLog().debug("Unable to retrieve value for: " + metric.getAttributeName());
                return MetricValue.NONE;
            }
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

    private List<String> getScript(Metric metric, String scriptName) {
        String dbNameWithQuotes = "\"" + metric.getDomainName() + "\"";
        String serverName = getServerName(metric);
        String username = metric.getObjectProperty("User");
        String password = metric.getObjectProperty("Password");
        String pdkWorkDir = "\"" + ProductPluginManager.getPdkWorkDir();
        String sqlScript = pdkWorkDir + "/scripts/mssql/" + scriptName + "\"";
        String outputPath = pdkWorkDir + "/scripts/mssql/MDF_FreeSpacePct.out\"";
        String serverNameWithQuotes = "\"" + serverName + "\"";
        List<String> scriptPropertiesList = new ArrayList<String>();
        if (MDF_FREE_SPACE_PCT2005_SQL.equals(scriptName)) {
            scriptPropertiesList.add("sqlcmd");

        } else {
            scriptPropertiesList.add("osql");
            scriptPropertiesList.add("-n");
        }
        scriptPropertiesList.add("-S");
        scriptPropertiesList.add(serverNameWithQuotes);
        scriptPropertiesList.add("-d");
        scriptPropertiesList.add(dbNameWithQuotes);
        scriptPropertiesList.add("-s");
        scriptPropertiesList.add(",");
        scriptPropertiesList.add("-i");
        scriptPropertiesList.add(sqlScript);
        scriptPropertiesList.add("-l");
        scriptPropertiesList.add(System.getProperty(MSSQL_LOGIN_TIMEOUT, "5"));
        scriptPropertiesList.add("-h-1");
        scriptPropertiesList.add("-w");
        scriptPropertiesList.add("300");

        if (getLog().isDebugEnabled()) {
            getLog().debug("Script Properties = " + scriptPropertiesList);
        }

        /* If the user specifies the username and password then it is it will use sql authentication.
         Otherwise, it will use the trusted connection user to access the db. 
         */
        if (username != null && !"%user%".equals(username)
                && password != null && !"%password%".equals(password)) {
            getLog().debug(
                    "Adding username to script properties: -U " + username);
            scriptPropertiesList.add("-U");
            scriptPropertiesList.add(username);

            getLog().debug(
                    "Adding password to script properties: -P *******");
            scriptPropertiesList.add("-P");
            scriptPropertiesList.add(password);
        } else {
            // -E means it is a trusted connection
            getLog().debug("Setting as trusted connection on script properties: -E");
            scriptPropertiesList.add("-E");
        }
        return scriptPropertiesList;
    }

    /**
     * Runs a sqlcmd command piped to an output file, then parses the output
     * file to get database free percent. Not a particularly elegant way to get
     * the data, but it works.
     *
     * @param dbName - Name of the SQL Server database
     * @param serverName - Name of the SQL Server database instance (config
     * option defaults to localhost)
     * @return
     * @throws MetricNotFoundException
     */
    private synchronized MetricValue getUnallocatedSpace(Metric metric, String scriptName)
            throws MetricNotFoundException {
        final String dbName = metric.getDomainName();
        final String serverName = getServerName(metric);
        List<String> scriptPropertiesList = getScript(metric, scriptName);
        try {
            Process proc = new ProcessBuilder(
                    scriptPropertiesList
                    .toArray(new String[scriptPropertiesList.size()]))
                    .start();
            StreamHandler stdout = new StreamHandler("StreamHandler-Input", proc.getInputStream(),
                    getLog().isDebugEnabled()) {
                private Map<String, String> processMap = new HashMap<String, String>();

                @Override
                protected void processString(String line) {
                    String[] lineSplit = line.split(",");
                    if (lineSplit.length == 2) {
                        if (dbName.equals(lineSplit[0].trim())) {
                            getLog().debug("Database name found: " + dbName + " Value=" + lineSplit[1].trim());
                            processMap.put(lineSplit[0].trim(), lineSplit[1].trim());
                        }
                    } else {
                        getLog().debug("Unknown formatting from script output:" + line);
                    }
                }

                public Object getResult() {
                    return processMap;
                }
            };
            StreamHandler stderr = new StreamHandler("StreamHandler-Error", proc.getErrorStream(),
                    getLog().isDebugEnabled());
            stdout.start();
            stderr.start();

            proc.waitFor();

            if (!((String) stderr.getResult()).equals("")) {
                throw new MetricNotFoundException("Unable to exec process: " + stderr.getResult());
            }

            if (stdout.hasError()) {
                throw new MetricNotFoundException("Error processing metric script output:" + stdout.getErrorString());
            } else {
                String freePercent = ((Map<String, String>) stdout.getResult())
                        .get(dbName);
                if (freePercent != null) {
                    MetricValue metricVal = new MetricValue(
                            Double.parseDouble(freePercent));
                    getLog().debug("Database Free Out Percent: " + freePercent);
                    return metricVal;
                } else {
                    return MetricValue.NONE;
                }
            }
        } catch (IOException e) {
            getLog().debug("Unable to exec process:", e);
            throw new MetricNotFoundException("Unable to exec process: " + e);
        } catch (InterruptedException e) {
            //ignore
        }
        return MetricValue.NONE;
    }

    private class StreamHandler extends Thread {

        private InputStream inputStream;
        private boolean verbose;
        private StringBuilder stringBuilder = new StringBuilder();
        private boolean hasError = false;
        private String errorString = null;

        public StreamHandler(String threadName, InputStream inputStream, boolean verbose) {
            super(threadName);
            this.inputStream = inputStream;
            this.verbose = verbose;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line = br.readLine();
                while (line != null) {
                    if (verbose) {
                        getLog().debug(line);
                    }
                    processString(line);
                    line = br.readLine();
                }
            } catch (IOException e) {
                getLog().debug("Exception reading stream: ", e);
                errorString = e.getMessage();
                hasError = true;
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        }

        protected void processString(String line) {
            stringBuilder.append(line + "\n");
        }

        public String getErrorString() {
            return errorString;
        }

        public boolean hasError() {
            return hasError;
        }

        public Object getResult() {
            return stringBuilder.toString();
        }
    }
}
