/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.mssql;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPluginManager;

/**
 *
 * @author Administrator
 */
public class MsSQLDataBaseCollector extends MsSQLCollector {

    private static final String MSSQL_LOGIN_TIMEOUT = "mssql.login_timeout";
    private static final String MDF_FREE_SPACE_PCT2005_SQL = "MDF_FreeSpacePct2005.sql";
    private static final String MDF_FREE_SPACE_PCT2000_SQL = "MDF_FreeSpacePct2000.sql";
    private static Log log = LogFactory.getLog(MsSQLDataBaseCollector.class);

    @Override
    public void collect() {
        Properties properties = getProperties();
        log.debug("[collect] props:" + properties);
        super.collect();

        List<String> scriptPropertiesList = getScript(properties);
        try {
            ProcessBuilder process = new ProcessBuilder(scriptPropertiesList.toArray(new String[scriptPropertiesList.size()]));
            Process proc = process.start();
            StreamHandler stdout = new StreamHandler("StreamHandler-Input", proc.getInputStream(), log.isDebugEnabled()) {
                @Override
                protected void processString(String line) {
                    String[] lineSplit = line.split(",");
                    if (lineSplit.length == 4) {
                        String db = lineSplit[0].trim();
                        String val = lineSplit[3].trim();
                        log.debug("Database:'" + db + "' Value='" + val + "'");
                        setValue(db, val);
                    } else {
                        log.debug("Unknown formatting from script output:" + line);
                    }
                }
            };
            StreamHandler stderr = new StreamHandler("StreamHandler-Error", proc.getErrorStream(), log.isDebugEnabled());
            stdout.start();
            stderr.start();

            proc.waitFor();

            if (!((String) stderr.getResult()).equals("")) {
                log.debug("Unable to exec process: " + stderr.getResult());
            }

            if (stdout.hasError()) {
                log.debug("Error processing metric script output:" + stdout.getErrorString());
            }
        } catch (IOException e) {
            log.debug("Unable to exec process:", e);
        } catch (InterruptedException e) {
            log.debug("Unable to exec process:", e);
        }
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        log.debug("==> " + metric);
        if (metric.getDomainName().equalsIgnoreCase("dfp")) {
            return result.getMetricValue(metric.getAttributeName());
        } else {
            return super.getValue(metric, result);
        }
    }

    private static List<String> getScript(Properties props) {
        List<String> scriptPropertiesList = new ArrayList<String>();

        String serverName = getServerName(props);
        String instance = props.getProperty("instance");
        if ((instance != null) && (!instance.equals(MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME))) {
            serverName += "\\" + instance;
        }
        String username = props.getProperty("User");
        String password = props.getProperty("Password");
        File pdkWorkDir = new File(ProductPluginManager.getPdkWorkDir());

        File sqlScript;
        if ("2005".equals(props.getProperty("v"))) {
            scriptPropertiesList.add("sqlcmd");
            sqlScript = new File(pdkWorkDir, "/scripts/mssql/" + MDF_FREE_SPACE_PCT2005_SQL);
        } else {
            scriptPropertiesList.add("osql");
            scriptPropertiesList.add("-n");
            sqlScript = new File(pdkWorkDir, "/scripts/mssql/" + MDF_FREE_SPACE_PCT2000_SQL);
        }
        scriptPropertiesList.add("-S");
        scriptPropertiesList.add(serverName);
        scriptPropertiesList.add("-s");
        scriptPropertiesList.add(",");
        scriptPropertiesList.add("-i");
        try {
            scriptPropertiesList.add(sqlScript.getCanonicalPath());
        } catch (IOException ex) {
            scriptPropertiesList.add(sqlScript.getAbsolutePath());
        }
        scriptPropertiesList.add("-l");
        scriptPropertiesList.add(System.getProperty(MSSQL_LOGIN_TIMEOUT, "3"));
        scriptPropertiesList.add("-h-1");
        scriptPropertiesList.add("-w");
        scriptPropertiesList.add("300");

        if (log.isDebugEnabled()) {
            log.debug("Script Properties = " + scriptPropertiesList);
        }

        /* If the user specifies the username and password then it is it will use sql authentication.
         Otherwise, it will use the trusted connection user to access the db. 
         */
        if (username != null && !"%user%".equals(username) && password != null && !"%password%".equals(password)) {
            log.debug("Adding username to script properties: -U " + username);
            scriptPropertiesList.add("-U");
            scriptPropertiesList.add(username);

            log.debug("Adding password to script properties: -P *******");
            scriptPropertiesList.add("-P");
            scriptPropertiesList.add(password);
        } else {
            // -E means it is a trusted connection
            log.debug("Setting as trusted connection on script properties: -E");
            scriptPropertiesList.add("-E");
        }

        return scriptPropertiesList;
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
                        log.debug(line);
                    }
                    processString(line);
                    line = br.readLine();
                }
            } catch (IOException e) {
                log.debug("Exception reading stream: ", e);
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
            stringBuilder.append(line).append("\n");
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

    private static String getServerName(Properties props) {
        String serverName = props.getProperty("ServerName");
        log.debug("ServerName from config=" + serverName);
        // there is bug causing the default not to be set for sqlserver_name
        if (serverName == null || "".equals(serverName) || "%sqlserver_name%".equals(serverName)) {
            serverName = "localhost";
            log.debug("Setting serverName to default=" + serverName);
        }
        return serverName;
    }
}
