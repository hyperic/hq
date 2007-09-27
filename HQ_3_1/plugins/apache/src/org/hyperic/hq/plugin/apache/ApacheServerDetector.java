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

package org.hyperic.hq.plugin.apache;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheServerDetector
    extends ServerDetector 
    implements FileServerDetector, AutoServerDetector {

    static final String TYPE_HTTPD = "Apache httpd";

    static final String DEFAULT_SERVICE_NAME = "Apache2";
    
    static final String VHOST_NAME = "VHost";

    static final String PROP_SERVER_NAME = "server.name";

    static final String[] PTQL_QUERIES = {
        "State.Name.eq=httpd,State.Name.Pne=$1",
        "State.Name.eq=apache2,State.Name.Pne=$1",
        "State.Name.eq=httpd2-worker,State.Name.Pne=$1",
        "State.Name.eq=httpd2-prefork,State.Name.Pne=$1",
    };

    private static final String SERVICE_ARGS =
        ",Args.1.Peq=-k,Args.2.Peq=runservice";

    static final String[] WIN32_EXES = {
        "Apache", "httpd"
    };

    static final String[] PTQL_QUERIES_WIN32 = {
        "State.Name.eq=" + WIN32_EXES[0] + SERVICE_ARGS,
        "State.Name.eq=" + WIN32_EXES[1] + SERVICE_ARGS,
    };

    private static Log log = LogFactory.getLog("ApacheServerDetector");

    private Properties props;
    private String defaultIp;
    private PortRange httpRange;
    private PortRange httpsRange;
    private boolean discoverModStatus;
    private boolean discoverModSnmp;

    public ApacheServerDetector () { 
        super();
    }

    public void init(PluginManager manager)
            throws PluginException {

        super.init(manager);
        this.props = manager.getProperties();
        this.defaultIp =
            this.props.getProperty("apache.listenAddress", "localhost");

        this.httpRange =
            new PortRange(this.props.getProperty("apache.http.ports"));

        this.httpsRange =
            new PortRange(this.props.getProperty("apache.https.ports"));

        //true == force discovery of mod_snmp based types when snmpd.conf does not exist
        this.discoverModSnmp =
            "true".equals(this.props.getProperty("apache.discover.mod_snmp"));

        //false == skip discovery of mod_status based types when snmpd.conf does not exist,
        //neither type will be reported.
        this.discoverModStatus =
            !"false".equals(this.props.getProperty("apache.discover.mod_status")) &&
            !this.discoverModSnmp;
    }

    private static void getServerInfo(ApacheBinaryInfo info, String[] args) {
        final String nameProp = "-Dhq.name=";
        String root = null;
        for (int i=1; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-d")) {
                root = arg.substring(2, arg.length());
                if (root.length() == 0) {
                    root = args[i+1];
                }
            }
            else if (arg.startsWith(nameProp)) {
                info.name = arg.substring(nameProp.length(),
                                          arg.length());
            }
        }

        if (root != null) {
            //-d overrides compiled in HTTPD_ROOT
            info.root = root;
        }
    }

    private static void findServerProcess(List servers, String query,
                                          String version) {
        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String httpd = getProcExe(pids[i]);

            if (httpd == null) {
                continue;
            }

            ApacheBinaryInfo info = ApacheBinaryInfo.getInfo(httpd, version);

            if (info == null) {
                continue;
            }

            getServerInfo(info, getProcArgs(pids[i]));

            if (info.root == null) {
                continue;
            }

            servers.add(info);
        }
    }

    protected static List getServerProcessList(String version,
                                               String[] queries) {
        ArrayList servers = new ArrayList();

        for (int i=0; i<queries.length; i++) {
            findServerProcess(servers, queries[i], version);
        }

        return servers;
    }

    protected boolean configureServer(ServerResource server)
        throws PluginException {

        ConfigResponse metricConfig, productConfig, controlConfig;
        String installpath = server.getInstallPath();
        File snmpConfig = getSnmpdConf(installpath);
        boolean snmpConfigExists = snmpConfig.exists();

        if (snmpConfigExists || this.discoverModSnmp) {
            if (!snmpConfigExists) {
                log.debug(snmpConfig +
                          " does not exist, cannot auto-configure " +
                          server.getName());
            }
            metricConfig = getSnmpConfig(snmpConfig);
            productConfig = getProductConfig(metricConfig);
            controlConfig = getControlConfig(installpath);

            if (productConfig != null) {
                setProductConfig(server, productConfig);
                setMeasurementConfig(server, metricConfig);
                if (controlConfig != null) {
                    setControlConfig(server, controlConfig);
                }

                server.setConnectProperties(new String[] {
                    SNMPClient.PROP_PORT,
                    SNMPClient.PROP_VERSION, //only need to avoid udp port conflicts
                });
            }

            server.setDescription("mod_snmp monitor");

            return true;
        }
        else if (this.discoverModStatus) {
            log.debug(snmpConfig +
                      " does not exist, discovering as type: " +
                      TYPE_HTTPD);
            productConfig = new ConfigResponse();

            //meant for command-line testing: -Dhttpd.url=http://localhost:8080/
            if (configureURL(getManagerProperty("httpd.url"), productConfig)) {
                server.setMeasurementConfig();
            }
            else {
                String port = "80";
                String address =
                    getListenAddress(port, this.defaultIp);

                productConfig.setValue(Collector.PROP_PROTOCOL,
                                       getConnectionProtocol(port));
                productConfig.setValue(Collector.PROP_SSL, isSSL(port));
                productConfig.setValue(Collector.PROP_HOSTNAME, address);
                productConfig.setValue(Collector.PROP_PORT, port);
                productConfig.setValue(Collector.PROP_PATH, "/server-status");
                //XXX need to auto-configure port and path
                //server.setMeasurementConfig();
            }

            server.setDescription("mod_status monitor");
            server.setType(TYPE_HTTPD);
            //in the event that mod_snmp is added later,
            //we won't clash w/ the other types
            server.setIdentifier(TYPE_HTTPD + " " + server.getInstallPath());
            server.setProductConfig(productConfig);

            return true;
        }
        else {
            log.debug("Ignoring " + server.getName() +
                      " at " + server.getInstallPath());
            return false;
        }
    }

    protected boolean configureServer(ServerResource server, ApacheBinaryInfo binary)
        throws PluginException {
        
        ConfigResponse cprops = new ConfigResponse(binary.toProperties());

        server.setCustomProperties(cprops);

        return configureServer(server);
    }

    public List getServerList(String installpath, String fullVersion,
                              ApacheBinaryInfo binary)
        throws PluginException {

        if (new File(installpath, "bin/httpsd.pthread").exists()) {
            //ERS 3.0: dont want this reported as a 1.3 server type
            return null;
        }

        ServerResource sValue = createServerResource(installpath);

        //name was already set, but we want to include .minor version too
        String name = getPlatformName() + " Apache " + fullVersion;
        if (binary.name != null) {
            name += " " + binary.name;
        }
        sValue.setName(name);

        List servers = new ArrayList();
        if (configureServer(sValue, binary)) {
            servers.add(sValue);
        }
        return servers;
    }

    protected File getSnmpdConf(String path) {
        //XXX conf dir is not always relative to installpath
        File file = new File(path, "conf" + File.separator + "snmpd.conf");
        return file;
    }

    protected ConfigResponse getSnmpConfig(File file) {
        ApacheSNMP.ConfigFile config = null;

        if (file.exists()) {
            try {
                config = ApacheSNMP.getConfig(file.toString());
                log.debug(file + " snmp agent=" + config);
                return new ConfigResponse(ApacheSNMP.getConfigProperties(config));
            } catch (IOException e) {
                log.warn("Unable to parse SNMP port from: " + file, e);
            }
        }

        return new ConfigResponse();
    }

    protected String getListenAddress(String port,
                                      String defaultAddress) {
        String address = super.getListenAddress(port);
        if (address.equals("localhost")) {
            return defaultAddress;
        }
        else {
            return address;
        }
    }

    protected ConfigResponse getProductConfig(ConfigResponse mConfig) {
        ApacheSNMP snmp = new ApacheSNMP();
        List servers;

        try {
            servers = snmp.getServers(mConfig);
        } catch (SNMPException e) {
            log.debug("getServers(" + mConfig + ") failed: " + e, e);
            return null;
        }

        if (servers.size() == 0) {
            log.debug("getServers(" + mConfig + ") == 0");
            return null;
        }

        Properties config = new Properties();

        //first entry will be the main server
        ApacheSNMP.Server server =
            (ApacheSNMP.Server)servers.get(0);

        String address =
            getListenAddress(server.port, this.defaultIp);

        config.setProperty(Collector.PROP_PROTOCOL,
                           getConnectionProtocol(server.port));
        config.setProperty(Collector.PROP_SSL,
                           isSSL(server.port));
        config.setProperty(Collector.PROP_HOSTNAME, address);
        config.setProperty(Collector.PROP_PORT, server.port);
        config.setProperty(PROP_SERVER_NAME,
                           server.name);

        log.debug("Configured server via snmp: " + server);

        return new ConfigResponse(config);
    }

    protected String getWindowsServiceName() {
        return DEFAULT_SERVICE_NAME;
    }

    protected String getDefaultControlScript() {
        String file = getTypeProperty("DEFAULT_SCRIPT");
        if (file != null) {
            return file;
        }
        else {
            return ApacheControlPlugin.DEFAULT_SCRIPT;
        }
    }

    protected String getDefaultPidFile() {
        String file = getTypeProperty("DEFAULT_PIDFILE");
        if (file != null) {
            return file;
        }
        else {
            return ApacheControlPlugin.DEFAULT_PIDFILE;
        }
    }

    protected ConfigResponse getControlConfig(String path) {
        Properties props = new Properties();

        if (isWin32()) {
            props.setProperty(Win32ControlPlugin.PROP_SERVICENAME,
                              getWindowsServiceName());
        }
        else {
            String script = path + "/" + getDefaultControlScript();

            if (new File(script).exists()) {
                props.setProperty(ApacheControlPlugin.PROP_PROGRAM,
                                  script);
            }
            else {
                return null; //XXX
            }

            props.setProperty(ApacheControlPlugin.PROP_PIDFILE,
                              path + "/" + getDefaultPidFile());
        }

        return new ConfigResponse(props);
    }

    private static String[] getPtqlQueries() {
        if (isWin32()) {
            return PTQL_QUERIES_WIN32;
        }
        else {
            return PTQL_QUERIES;
        }
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {

        String version = getTypeInfo().getVersion();
        List servers = new ArrayList();
        List binaries = getServerProcessList(version, getPtqlQueries());

        for (int i=0; i<binaries.size(); i++) {
            ApacheBinaryInfo info =
                (ApacheBinaryInfo)binaries.get(i);

            List found = getServerList(info.root, info.version, info);
            if (found != null) {
                servers.addAll(found);
            }
        }

        return servers;        
    }
    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain a single
     * AIServerValue (if a server was found).  Currently the 
     * DotOrgDetector does not support detecting multiple instances 
     * of Apache in a single directory.
     */
    public List getServerResources(ConfigResponse platformConfig, String path) throws PluginException {
        String version = getTypeInfo().getVersion();
        ApacheBinaryInfo binary = ApacheBinaryInfo.getInfo(path, version);
        if (binary == null) {
            return null; //does not match our server type version
        }

        String fullVersion = binary.version;

        if (fullVersion == null) {
            log.debug("Apache version " + version +
                      " not found in binary: " + path);
            return null;
        }

        //strip "bin/httpd"
        String installpath = getParentDir(path, 2);

        return getServerList(installpath, fullVersion, binary);
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        List services = new ArrayList();

        List vhosts = discoverVHostServices(serverConfig);
        if (vhosts != null) {
            services.addAll(vhosts);
        }

        List workers = discoverJkServices(serverConfig);
        if (workers != null) {
            services.addAll(workers);
        }

        return services;
    }

    protected boolean configureURL(String urlstr, ConfigResponse config) {
        if (urlstr == null) {
            return false;
        }

        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            log.error("Malformed url=" + urlstr);
            return false;
        }

        config.setValue(Collector.PROP_HOSTNAME, url.getHost());
        config.setValue(Collector.PROP_PORT, String.valueOf(url.getPort()));
        config.setValue(Collector.PROP_PATH, url.getPath());
        config.setValue(Collector.PROP_PROTOCOL, Collector.PROTOCOL_HTTP);
        if (url.getProtocol().equals(Collector.PROTOCOL_HTTPS)) {
            config.setValue(Collector.PROP_SSL, "true");
        }

        return true;
    }

    protected List discoverJkServices(ConfigResponse serverConfig)
        throws PluginException {

        ConfigResponse config = new ConfigResponse();

        //XXX must be defined in agent.properties for now
        if (!configureURL(getManagerProperty("jkstatus.url"), config)) {
            return null;
        }

        List services = new ArrayList();

        JkStatusCollector jkstatus = new JkStatusCollector();
        Map stats = jkstatus.getValues(this, config);
        Set workers = jkstatus.getWorkers();

        for (Iterator it=workers.iterator(); it.hasNext();) {
            String name = (String)it.next();
            ServiceResource worker =
                createServiceResource(JkStatusCollector.WORKER_NAME);
            worker.setServiceName(JkStatusCollector.WORKER_NAME + " " + name);

            ConfigResponse cprops = new ConfigResponse();
            for (int i=0; i<JkStatusCollector.WORKER_PROPS.length; i++) {
                String prop = JkStatusCollector.WORKER_PROPS[i];
                cprops.setValue(prop,
                                (String)stats.get(name + "." + prop));
            }

            ConfigResponse productConfig =
                new ConfigResponse(config.toProperties()); //clone
            productConfig.setValue("name", name);
            worker.setProductConfig(productConfig);
            worker.setCustomProperties(cprops);
            worker.setMeasurementConfig();

            services.add(worker);
        }

        return services;
    }

    protected List discoverVHostServices(ConfigResponse serverConfig)
        throws PluginException {
    
        if (serverConfig.getValue(SNMPClient.PROP_IP) == null) {
            return null; //server type "Apache httpd"
        }

        ApacheSNMP snmp = new ApacheSNMP();

        List servers;

        try {
            servers = snmp.getServers(serverConfig);
        } catch (SNMPException e) {
            throw new PluginException(e.getMessage(), e);
        }

        //inherit server hostname if something other than the default
        String serverHostname =
            serverConfig.getValue(Collector.PROP_HOSTNAME);
        String hostname =
            "localhost".equals(serverHostname) ?
            this.defaultIp : serverHostname;

        String serverProtocol =
            serverConfig.getValue(Collector.PROP_PROTOCOL);
        String protocol =
            ("ping".equals(serverProtocol) || //back-compat
             Collector.PROTOCOL_SOCKET.equals(serverProtocol)) ?       
            serverProtocol : null;

        List services = new ArrayList();
        Map serviceNames = new HashMap();

        for (int i=0; i<servers.size(); i++) {
            ApacheSNMP.Server server =
                (ApacheSNMP.Server)servers.get(i);

            String serviceName = server.toString();

            //XXX should not get any duplicates.
            //but if we do, just fold them, no point in having duplicate
            //services.
            if (serviceNames.get(serviceName) == Boolean.TRUE) {
                log.debug("Discovered duplicate service: " + serviceName);
                continue;
            }
            serviceNames.put(serviceName, Boolean.TRUE);
            log.debug("Discovered service: " + serviceName);

            ServiceResource service = new ServiceResource();
            service.setType(this, VHOST_NAME);
            service.setServiceName(VHOST_NAME + " " + serviceName);

            ConfigResponse config = new ConfigResponse();

            config.setValue(Collector.PROP_PORT, server.port);

            config.setValue(Collector.PROP_HOSTNAME,
                            getListenAddress(server.port, hostname));
            config.setValue(PROP_SERVER_NAME, server.name);

            //XXX snmp does not tell us the protocol
            String proto = protocol == null ?
                getConnectionProtocol(server.port) :
                protocol;

            config.setValue(Collector.PROP_PROTOCOL, proto);
            config.setValue(Collector.PROP_SSL,
                            isSSL(server.port));
            service.setProductConfig(config);
            service.setMeasurementConfig();

            services.add(service);
        }

        if (servers.size() > 0) {
            ApacheSNMP.Server server =
                (ApacheSNMP.Server)servers.get(0);
            ConfigResponse cprops = new ConfigResponse();
            cprops.setValue("version", server.version);
            cprops.setValue("serverTokens", server.description);
            cprops.setValue("serverAdmin", server.admin);
            setCustomProperties(cprops);
        }

        return services;
    }

    private String isSSL(String port) {
        int nPort = Integer.parseInt(port);
        if (this.httpRange.hasValue(nPort)) {
            return "false";
        }
        else if (this.httpsRange.hasValue(nPort)) {
            return "true";
        }
        else {
            return String.valueOf(isSSLPort(port));
        }
    }

    private static class PortRange {
        int start, end;

        public PortRange(String range) {
            parse(range);
        }

        public void parse(String range) {
            if (range == null) {
                this.start = this.end = 0;
                return;
            }

            int ix = range.indexOf("..");
            if (ix == -1) {
                throw new IllegalArgumentException("Invalid range: " + range);
            }

            this.start = Integer.parseInt(range.substring(0, ix));
            this.end   = Integer.parseInt(range.substring(ix+2,
                                                          range.length()));
        }

        public boolean hasValue(int value) {
            return
                (value >= this.start) &&
                (value <= this.end);
        }

        public String toString() {
            return this.start + ".." + this.end;
        }
    }

    public static void main(String[] args) throws Exception {
        String[] versions = {"1.3", "2.0"};

        for (int i=0; i<versions.length; i++) {
            List servers = getServerProcessList(versions[i],
                                                getPtqlQueries());

            for (int j=0; j<servers.size(); j++) {
                System.out.println(versions[i] + " " + servers.get(j));
            }
        }
    }
}
