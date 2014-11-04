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
import org.hyperic.hq.product.PluginException;
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
        MsSQLDetector.debug(log, "[collect] props:" + properties);
        super.collect();

        List<String> scriptPropertiesList = getScript(properties);
        List<List<String>> res;
        try {
            res = executeSqlCommand(scriptPropertiesList);
            for (List<String> line : res) {
                if (line.size() == 4) {
                    String db = line.get(0);
                    String val = line.get(3);
                    MsSQLDetector.debug(log, "Database:'" + db + "' Value='" + val + "'");
                    setValue(db, val);
                } else {
                    MsSQLDetector.debug(log, "Unknown formatting from script output:" + line);
                }
            }
        } catch (PluginException ex) {
            MsSQLDetector.debug(log, ex.toString(), ex);
        }

    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MsSQLDetector.debug(log, "==> " + metric);
        if (metric.getDomainName().equalsIgnoreCase("dfp")) {
            return result.getMetricValue(metric.getAttributeName());
        } else {
            return super.getValue(metric, result);
        }
    }

    public static List<List<String>> executeSqlCommand(List<String> scriptPropertiesList) throws PluginException {
        final List<List<String>> res = new ArrayList<List<String>>();

        if (log.isDebugEnabled()) {
            MsSQLDetector.debug(log, "[executeSqlCommand] Script Properties = " + scriptPropertiesList);
        }

        String output = runCommand(scriptPropertiesList.toArray(new String[scriptPropertiesList.size()]), 60);
        List<String> lines = Arrays.asList(output.split("\n"));
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0) {
                MsSQLDetector.debug(log, "[executeSqlCommand] line: '" + line + "'");
                if (!line.startsWith("(") && !line.endsWith(")")) {
                    List<String> lineSplit = new ArrayList<String>();
                    for (String str : line.split(",")) {
                        lineSplit.add(str.trim());
                    }
                    res.add(lineSplit);
                }
            }
        }

        return res;
    }

    public static List<String> prepareSqlCommand(Properties props) {
        MsSQLDetector.debug(log, "==>" + props);
        List<String> scriptPropertiesList = new ArrayList<String>();

        if ("2000".equals(props.getProperty("v"))) {
            scriptPropertiesList.add("osql");
            scriptPropertiesList.add("-n");
        } else {
            scriptPropertiesList.add("sqlcmd");
        }

        String serverName = props.getProperty("sqlserver_name", "").replaceAll("\\%[^\\%]*\\%", "").trim();
        String instance = props.getProperty("instance", props.getProperty("instance-name", "")).replaceAll("\\%[^\\%]*\\%", "").trim();
        MsSQLDetector.debug(log, "sqlserver_name='" + serverName + "' instance=" + instance + "");
        if (serverName.length() > 0) {
            if (!MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME.equals(instance) && (instance.length() > 0)) {
                serverName += "\\" + instance;
            }
            scriptPropertiesList.add("-S");
            scriptPropertiesList.add(serverName);
        }

        scriptPropertiesList.add("-s");
        scriptPropertiesList.add(",");
        scriptPropertiesList.add("-l");
        scriptPropertiesList.add(System.getProperty(MSSQL_LOGIN_TIMEOUT, "3"));
        scriptPropertiesList.add("-h-1");
        scriptPropertiesList.add("-w");
        scriptPropertiesList.add("65535");

        if (log.isDebugEnabled()) {
            MsSQLDetector.debug(log, "Script Properties = " + scriptPropertiesList);
        }

        /* If the user specifies the username and password then it is it will use sql authentication.
         Otherwise, it will use the trusted connection user to access the db. 
         */
        String username = props.getProperty("user", "").replaceAll("\\%[^\\%]*\\%", "").trim();
        String password = props.getProperty("password", "").replaceAll("\\%[^\\%]*\\%", "").trim();

        if ((username.length() > 0) && (password.length() > 0)) {
            MsSQLDetector.debug(log, "Adding username to script properties: -U " + username);
            scriptPropertiesList.add("-U");
            scriptPropertiesList.add(username);

            MsSQLDetector.debug(log, "Adding password to script properties: -P *******");
            scriptPropertiesList.add("-P");
            scriptPropertiesList.add(password);
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

    public static String runCommand(String[] command, int timeout) throws PluginException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(output);
        ExecuteWatchdog wdog = new ExecuteWatchdog(timeout * 1000);
        Execute exec = new Execute(pumpStreamHandler, wdog);
        exec.setCommandline(command);
        MsSQLDetector.debug(log, "Running: " + exec.getCommandLineString());
        try {
            exec.execute();
        } catch (Exception e) {
            throw new PluginException("Fail to run command: " + e.getMessage(), e);
        }
        String out = output.toString().trim();
        MsSQLDetector.debug(log, "out: " + out);
        MsSQLDetector.debug(log, "ExitValue: " + exec.getExitValue());
        if (exec.getExitValue() != 0) {
            throw new PluginException(out);
        }
        return out;
    }
}
