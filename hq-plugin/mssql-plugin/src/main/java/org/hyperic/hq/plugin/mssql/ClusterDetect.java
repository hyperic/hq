package org.hyperic.hq.plugin.mssql;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;

public class ClusterDetect {
    private static final Log log = LogFactory.getLog(ClusterDetect.class);
    private static final Pattern CLUSTER_NAME_PATTERN = Pattern.compile(
            "Listing properties for '(.*)'", Pattern.CASE_INSENSITIVE);

    public static final String CLUSTER_NAME_PROP = "cluster name";
    public static final String NETWORK_NAME_PROP = "network name";
    public static final String NODES_PROP = "nodes";
    public static final String IP_ADDRESS_PROP = "ip address";
    private static final String APPCMD = "C:/Windows/System32/cluster.exe";

    public static Properties getMssqlClusterProps(String instanceName) {
        String clusterName = getClusterName();
        if (clusterName == null) {
            log.debug("Cluster name not found");
            return null;
        }
        log.debug("Cluster name: " + clusterName);

        String clusterResources = runCommand(new String[] { APPCMD, "res",
        "/priv" });

        log.debug("Cluster resources: " + clusterResources);
        String sqlServerResource = getSqlServerResource(clusterResources,
                instanceName);
        if (sqlServerResource == null) {
            log.debug("SQL server resource is null");
            return null;
        }
        log.debug("SQL server resource: " + sqlServerResource);

        String networkName = getNetworkNameFromResource(sqlServerResource,
                clusterResources);

        if (networkName == null) {
            log.debug("Network name is null");
            return null;
        }
        log.debug("Network name: " + networkName);

        Properties props = new Properties();
        props.put(CLUSTER_NAME_PROP, clusterName);
        props.put(NETWORK_NAME_PROP, networkName);

        String clusterNodes = getClusterNodes();

        props.put(NODES_PROP, clusterNodes);
        return props;
    }

    private static String getClusterNodes() {
        String clusterNodesOutput = runCommand(new String[] { APPCMD, "node" });
        String result = "";

        Pattern nodeTablePattern = Pattern.compile("(\\-+)\\s+(\\-+)\\s+(\\-+)(.+)", Pattern.DOTALL);
        Matcher matcher = nodeTablePattern.matcher(clusterNodesOutput);
        if(!matcher.find()) {
            return result;
        }

        String table = matcher.group(4);

        Pattern nodeNamePattern = Pattern.compile("^(\\S+)\\s+", Pattern.MULTILINE);
        Matcher nameMatcher = nodeNamePattern.matcher(table);

        boolean isFirst = true;
        while(nameMatcher.find()) {
            if(!isFirst) {
                result += ",";
            }
            result += nameMatcher.group(1);
            isFirst = false;
        }

        return result;
    }

    private static String getSqlServerResource(String clusterResources,
            String instanceName) {
        Pattern sqlServerResourcePattern = Pattern.compile(
                "^(\\w+)\\s+(.+\\S)\\s+InstanceName\\s+" + instanceName,
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = sqlServerResourcePattern.matcher(clusterResources);
        if (!matcher.find()) {
            return null;
        }
        String sqlServerResource = matcher.group(2);
        return sqlServerResource;
    }

    private static String getNetworkNameFromResource(
            String networkNameResource, String clusterResources) {
        Pattern sqlNetworkNamePattern = Pattern.compile(Pattern.quote(networkNameResource)
                + "\\s+VirtualServerName\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = sqlNetworkNamePattern.matcher(clusterResources);
        if (!matcher.find()) {
            return null;
        }
        String networkName = matcher.group(1);
        return networkName;
    }

    private static String getClusterName() {
        String output = runCommand(new String[] { APPCMD, "/prop" });
        if (output == null) {
            return null;
        }
        Matcher matcher = CLUSTER_NAME_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }
        String clusterName = matcher.group(1);
        return clusterName;
    }

    private static String runCommand(String[] command) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Execute exec = new Execute(new PumpStreamHandler(output));
        exec.setCommandline(command);
        try {
            exec.execute();
        } catch (Exception e) {
            log.warn("Failed to run command: " + exec.getCommandLineString(), e);
            return null;
        }
        String out = output.toString().trim();
        return out;
    }
}