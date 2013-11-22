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
package org.hyperic.hq.plugin.websphere;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.websphere.jmx.WebsphereRuntimeDiscoverer;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.util.config.ConfigResponse;

public class WebsphereDetector
        extends ServerDetector
        implements FileServerDetector,
        RegistryServerDetector,
        AutoServerDetector {

    private static final String[] METRIC_CONNECT_PROPS = {
        WebsphereProductPlugin.PROP_ADMIN_PORT,
        WebsphereProductPlugin.PROP_ADMIN_HOST
    };
    static protected Log log =
            LogFactory.getLog(WebsphereDetector.class.getName());
    private static final String PTQL_QUERY =
            "State.Name.eq=java,Args.*.eq=com.ibm.ws.runtime.WsServer";
    private static final String SOAP_PORT_EXPR =
            "//serverEntries[@serverName=\"{0}\"]//specialEndpoints[@endPointName=\"SOAP_CONNECTOR_ADDRESS\"]//@port";
    private static final String SOAP_HOST_EXPR =
            "//serverEntries[@serverName=\"{0}\"]//specialEndpoints[@endPointName=\"SOAP_CONNECTOR_ADDRESS\"]//@host";
    protected WebsphereRuntimeDiscoverer discoverer = null;
    private String node = null;

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        if (!WebsphereProductPlugin.VALID_JVM) {
            return new ArrayList();
        }

        if (this.discoverer == null) {
            String version = getTypeInfo().getVersion();
            this.discoverer = new WebsphereRuntimeDiscoverer(version, this);
        }
        return this.discoverer.discoverServices(config);
    }

    protected String getProcessQuery() {
        return PTQL_QUERY;
    }

    public String getAdminHost(WebSphereProcess proc) {
        return getAdminHost(findServerIndex(proc), proc.getServer());
    }

    public String getAdminHost(File index, String serverName) {
        String host = null;
        final String prop = WebsphereProductPlugin.PROP_ADMIN_HOST;
        Object[] servers = {serverName};
        if (index != null) {
            host = getXPathValue(index, MessageFormat.format(SOAP_HOST_EXPR, servers));
            getLog().debug("Configuring " + prop + "=" + host + " from: " + index);
        }
        if (host == null) {
            host = getManager().getProperty(prop, "localhost");
        }

        if (host.equals("*")) {
            host = "localhost";
        }
        return host;
    }

    private File findServerIndex(WebSphereProcess proc) {
        if (proc == null) {
            return null;
        }
        File index = new File(proc.getServerRoot() + "/config/cells/" + proc.getCell() + "/nodes/" + proc.getNode() + "/serverindex.xml");
        if (!index.exists()) {
            index = null;
        }
        return index;
    }

    public String getAdminPort(WebSphereProcess proc) {
        return getAdminPort(findServerIndex(proc), proc.getServer());
    }

    public String getAdminPort(File index, String serverName) {
        String port = null;
        final String prop = WebsphereProductPlugin.PROP_ADMIN_PORT;

        Object[] servers = {serverName};
        if (index != null) {
            String query = MessageFormat.format(SOAP_PORT_EXPR, servers);
            port = getXPathValue(index, query);
            getLog().debug("Configuring " + prop + "=" + port + " from: " + index);
        }
        return port;
    }

    protected String getNodeName() {
        return this.node;
    }

    public final String getControlScript(String script) {
        return "bin" + File.separatorChar + script + getScriptExtension();
    }

    public final ConfigResponse getControlConfig(WebSphereProcess proc) {
        String type = proc.getServer().equals("nodeagent") ? "Node" : "Server";
        File cs1 = new File(proc.getServerRoot(), getControlScript("start" + type));
        File cs2 = new File(proc.getServerRoot(), getControlScript("stop" + type));
        assert cs1.exists() : cs1.getAbsolutePath();
        assert cs2.exists() : cs2.getAbsolutePath();
        ConfigResponse cc = new ConfigResponse();
        cc.setValue(ServerControlPlugin.PROP_PROGRAM + ".start", cs1.getAbsolutePath());
        cc.setValue(ServerControlPlugin.PROP_PROGRAM + ".stop", cs2.getAbsolutePath());
        log.debug("[getControlConfig] server=" + proc.getServer());
        log.debug("[getControlConfig] cc=" + cc);
        return cc;
    }

    protected void initDetector(File root) {
        //sadly, the setupCmdLine script is the
        //best way to determine the node name
        final String NODE_PROP = "WAS_NODE=";

        File cmdline =
                new File(root, "bin/setupCmdLine"
                + getScriptExtension());
        Reader reader = null;

        try {
            reader = new FileReader(cmdline);
            BufferedReader buffer =
                    new BufferedReader(reader);
            String line;

            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                int ix = line.indexOf(NODE_PROP);
                if (ix == -1) {
                    continue;
                }
                this.node =
                        line.substring(ix + NODE_PROP.length());
                break;
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static String getRunningInstallPath() {
        return getRunningInstallPath(PTQL_QUERY);
    }

    static List getServerProcessList() {
        return getServerProcessList(PTQL_QUERY);
    }

    protected Properties loadProps(File file) {
        Properties props = new Properties();
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            props.load(is);
        } catch (IOException e) {
            //ok
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        return props;
    }

    protected String getNodeNameFromFQDN(String fqdn) {
        int idx = fqdn.indexOf(".");
        if (idx > 0) {
            return fqdn.substring(0, idx);
        }
        return fqdn;
    }

    protected Properties getProductConfig(WebSphereProcess proc) {
        Properties productProps = new Properties();

        productProps.setProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                getAdminHost(proc));

        productProps.setProperty(WebsphereProductPlugin.PROP_ADMIN_PORT,
                getAdminPort(proc));

        productProps.setProperty(WebsphereProductPlugin.PROP_SERVER_NODE, proc.getNode());

        productProps.setProperty(WebsphereProductPlugin.PROP_SERVER_CELL, proc.getCell());

        productProps.setProperty(WebsphereProductPlugin.PROP_SERVER_NAME, proc.getServer());

        return productProps;
    }

    protected boolean isServiceControl() {
        return isWin32();
    }

    protected static List getServerProcessList(String query) {
        final String wasProp = "-Dwas.install.root=";
        final String rootProp = "-Dserver.root="; //5.x, optional in 6.1

        ArrayList servers = new ArrayList();

        long[] pids = getPids(query);

        for (int i = 0; i < pids.length; i++) {
            String[] args = getProcArgs(pids[i]);
            WebSphereProcess process = new WebSphereProcess();
            process.setPid(pids[i]);

            // next-to-last arg should be node name
            int ai = args.length;
            if (args[ai - 1].trim().equals("")) {
                ai--; // some times the las arg is a " "
            }
            if (args.length > 3) {
                process.setServer(args[ai - 1]);
                process.setNode(args[ai - 2]);
                process.setCell(args[ai - 3]);
            }

            for (int j = 0; j < args.length; j++) {
                String arg = args[j];

                if (arg.startsWith(wasProp)) {
                    process.setInstallRoot(arg.substring(wasProp.length(), arg.length()));
                } else if (arg.startsWith(rootProp)) {
                    process.setServerRoot(arg.substring(rootProp.length(), arg.length()));
                }

                if (process.isPropsConfigured()) {
                    log.debug("[getServerProcessList] process=" + process);
                    servers.add(process);
                    break;
                }
            }
        }

        return servers;
    }

    protected static String getRunningInstallPath(String query) {
        List servers = getServerProcessList(query);

        if (servers.size() == 0) {
            return null;
        }

        return ((WebSphereProcess) servers.get(0)).getInstallRoot();
    }
    //used for 6.0 and 6.1
    static final String VERSION_START = "<version>";
    static final String VERSION_END = "</version>";

    protected static String getComponentVersion(File file) {
        Reader reader = null;
        String res = "";

        try {
            reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            String line;

            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                int ix = line.indexOf(VERSION_END);
                if (line.startsWith(VERSION_START) && (ix != -1)) {
                    res = line.substring(VERSION_START.length(), ix).trim();
                }
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Error getting the WAS version: " + e.getMessage(), e);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return res;
    }

    protected boolean isComponentVersion(File file) {
        String version = getComponentVersion(file);
        boolean res = version.startsWith(getTypeInfo().getVersion());
        log.debug("version= '" + version + "' type='" + getTypeInfo().getVersion() + "' res=" + res);
        return res;
    }

    protected List getServerList(File serverDir, String version, WebSphereProcess proc)
            throws PluginException {

        log.debug("[getServerList] (" + version + ") " + proc.getInstallRoot());
        List servers = new ArrayList();

        //make sure detector version is that of was version
        //else the 5.0 detector will report 6.0 servers
        if (version != null) {
            char majVersion =
                    getTypeInfo().getVersion().charAt(0);
            if (version.charAt(0) != majVersion) {
                return null;
            }
        }

        //distinquish between versions using a unique file
        //since there is no simple way to get the version on disk.
        String uniqueFile =
                getTypeProperty("UNIQUE_FILE");

        if (uniqueFile != null) {
            File file = new File(proc.getInstallRoot(), uniqueFile);
            boolean exists = file.exists();

            log.debug(getTypeInfo().getName() + " '"
                    + file + "'.exists()=" + exists);

            if (!exists) {
                return null;
            }
            if (file.getName().equals("WAS.product")) {
                if (!isComponentVersion(file)) {
                    return null;
                }
            }
        }

        initDetector(serverDir);

        String installpath = serverDir.getAbsolutePath();
        ServerResource server = createServerResource(installpath);

        String type = getTypeInfo().getName();

        //for example, having WebSphere 6.0 and 6.1 installed
        //on the same machine, use version in the name to make
        //them unique.
        if ((version != null) && (version.length() == 3)) {
            if (!type.endsWith(version)) {
                type = type.substring(0, type.length() - 3) + version;
            }
        }

        server.setIdentifier(proc.getIdentifier());
        server.setName(getPlatformName() + " " + type + " " + proc.getServerName());

        ConfigResponse productConfig =
                new ConfigResponse(getProductConfig(proc));

        populateListeningPorts(proc.getPid(), productConfig, true);

        if (WebsphereProductPlugin.isOSGi()) {
            String prop = WebsphereProductPlugin.PROP_INSTALL_ROOT;
            String root =
                    System.getProperty(prop, getTypeProperty(prop));
            if (root != null) {
                productConfig.setValue(prop, root);
            }
        }

        //if we find more than one server w/ the same connect config,
        //this will make sure only 1 gets auto-enabled for metrics/ai
        server.setConnectProperties(METRIC_CONNECT_PROPS);

        setProductConfig(server, productConfig);
        server.setMeasurementConfig();
        setControlConfig(server, getControlConfig(proc));

        servers.add(server);

        log.debug("Detected " + server.getName() + " in " + serverDir);
        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current)
            throws PluginException {

        path = path.trim(); //trim trailing ^@

        log.debug("checking path=" + path);

        String version = current.getSubKeyName();
        //5.0.0.0, 5.1.0.0, etc.
        if ((version != null) && (version.length() > 3)) {
            version = version.trim().substring(0, 3);
        }

        return getServerList(new File(path), version, null);
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();
        List processes = getServerProcessList(getProcessQuery());

        for (int i = 0; i < processes.size(); i++) {
            WebSphereProcess p = (WebSphereProcess) processes.get(i);
            if (!p.getServer().equals("nodeagent")) {
                String nodeAgentPort = getAdminPort(findServerIndex(p), "nodeagent"); // skip if has nodeagent?
                if (nodeAgentPort == null) {
                    List found = getServerList(new File(p.getServerRoot()), null, p);
                    if (found != null) {
                        servers.addAll(found);
                    }
                }
            }
        }
        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path)
            throws PluginException {

        log.debug("Looking for " + getName() + " in " + path);

        File jar = new File(path);

        //loose lib/foo.jar defined in etc/cam-server-sigs.properties
        File serverDir = jar.getParentFile().getParentFile();

        return getServerList(serverDir, null, null);
    }

    private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}
