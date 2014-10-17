/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.mssql;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

/**
 *
 * @author Administrator
 */
public class MsSQLDataBaseCollector extends MsSQLCollector {

    private static final String MSSQL_LOGIN_TIMEOUT = "mssql.login_timeout";
    private static final String MDF_FREE_SPACE_PCT2005_SQL = "MDF_FreeSpacePct2005.sql";
    private static final String MDF_FREE_SPACE_PCT2000_SQL = "MDF_FreeSpacePct2000.sql";
    private static final Log log = LogFactory.getLog(MsSQLDataBaseCollector.class);

    @Override
    public void collect() {
        Properties properties = getProperties();
        log.debug("[collect] props:" + properties);
        super.collect();

        List<String> scriptPropertiesList = getScript(properties);
        List<List<String>> res = executeSqlCommand(scriptPropertiesList);

        for (List<String> line : res) {
            if (line.size() == 4) {
                String db = line.get(0);
                String val = line.get(3);
                log.debug("Database:'" + db + "' Value='" + val + "'");
                setValue(db, val);
            } else {
                log.debug("Unknown formatting from script output:" + line);
            }
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

    public static List<List<String>> executeSqlCommand(List<String> scriptPropertiesList) {
        if (log.isDebugEnabled()) {
            List<String> p = new ArrayList<String>(scriptPropertiesList);
            int i = p.indexOf("-P");
            if (i != -1) {
                p.set(i + 1, "**********");
            }
            log.debug("[executeSqlCommand] Script Properties = " + scriptPropertiesList);
        }

        final List<List<String>> res = new ArrayList<List<String>>();
        String output = runCommand(scriptPropertiesList.toArray(new String[scriptPropertiesList.size()]), 60);
        List<String> lines = Arrays.asList(output.split("\n"));
        for (String line : lines) {
            line = line.trim();
            if ((line.length() > 0) && !line.startsWith("(") && !line.endsWith(")")) {
                List<String> lineSplit = new ArrayList<String>();
                for (String str : line.split(",")) {
                    lineSplit.add(str.trim());
                }
                if (log.isDebugEnabled()) {
                    log.debug("[executeSqlCommand] line:" + lineSplit);
                }
                res.add(lineSplit);
            }
        }
        return res;
    }

    public static List<String> prepareSqlCommand(Properties props) {
        log.debug("==>" + props);
        List<String> scriptPropertiesList = new ArrayList<String>();

        String serverName = getServerName(props);
        String instance = props.getProperty("instance");
        if ((instance != null) && (!instance.equals(MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME))) {
            serverName += "\\" + instance;
        }
        String username = props.getProperty("user");
        String password = props.getProperty("password");
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
        scriptPropertiesList.add("-l");
        scriptPropertiesList.add(System.getProperty(MSSQL_LOGIN_TIMEOUT, "3"));
        scriptPropertiesList.add("-h-1");
        scriptPropertiesList.add("-w");
        scriptPropertiesList.add("65535");

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

    private static List<String> getScript(Properties props) {
        List<String> scriptPropertiesList = prepareSqlCommand(props);
        File pdkWorkDir = new File(ProductPluginManager.getPdkWorkDir());

        File sqlScript;
        if ("2005".equals(props.getProperty("v"))) {
            sqlScript = new File(pdkWorkDir, "/scripts/mssql/" + MDF_FREE_SPACE_PCT2005_SQL);
        } else {
            sqlScript = new File(pdkWorkDir, "/scripts/mssql/" + MDF_FREE_SPACE_PCT2000_SQL);
        }

        scriptPropertiesList.add("-i");
        try {
            scriptPropertiesList.add(sqlScript.getCanonicalPath());
        } catch (IOException ex) {
            scriptPropertiesList.add(sqlScript.getAbsolutePath());
        }

        return scriptPropertiesList;
    }

    public static String runCommand(String[] command, int timeout) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(output);
        ExecuteWatchdog wdog = new ExecuteWatchdog(timeout * 1000);
        Execute exec = new Execute(pumpStreamHandler, wdog);
        exec.setCommandline(command);
        log.debug("Running: " + exec.getCommandLineString());
        try {
            exec.execute();
        } catch (Exception e) {
            log.debug("Fail to run command: " + exec.getCommandLineString() + " " + e.getMessage());
            return null;
        }
        String out = output.toString().trim();
        log.debug("out: " + out);
        log.debug("done: " + exec.getCommandLineString());
        return out;
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
