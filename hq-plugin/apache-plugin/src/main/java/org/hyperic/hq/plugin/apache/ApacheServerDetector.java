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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.ConfigFileTrackPlugin;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.LogFileTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheServerDetector
    extends DaemonDetector 
    implements FileServerDetector, AutoServerDetector {

    static final String TYPE_HTTPD = "Apache httpd";

    static final String DEFAULT_SERVICE_NAME = "Apache2";
    
    static final String VHOST_NAME = "VHost";

    static final String PROP_SERVER_NAME = "server.name";
    static final String PROP_SERVER_PORT = "server.port";

    static final String[] PTQL_QUERIES = {
        "State.Name.eq=httpd,State.Name.Pne=$1",
        "State.Name.eq=apache2,State.Name.Pne=$1",
        "State.Name.eq=apache,State.Name.Pne=$1",
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

    private static String defaultConfs[] = {"conf/httpd.conf", "conf/httpsd.conf"};
    
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

    private String getConfigValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.charAt(0) != '/') {
            return value;
        }
        StringTokenizer tok = new StringTokenizer(value, ",");
        while (tok.hasMoreTokens()) {
            String file = tok.nextToken();
            if (exists(file)) {
                return file;
            }
        }
        return null;
    }

    protected String getConfigProperty(String name, String defval) {
        String key = "httpd." + name;
        String val = getManager().getProperty(key); //agent.properties
        if (val != null) {
            log.debug("ConfigProperty: " + key + "==" + val);
            return val;
        }
        Properties props = getProperties();
        OperatingSystem os = OperatingSystem.getInstance();
        String[] prefix = {
           os.getVendor(),
           os.getName()
        };
        for (int i=0; i<prefix.length; i++) {
            key = prefix[i] + "." + name;
            val = getConfigValue(props.getProperty(key));
            if (val != null) {
                log.debug("ConfigProperty: " + key + "==" + val);
                return val;
            }
        }
        return defval;
    }

    protected String getConfigProperty(String name) {
        return getConfigProperty(name, null);
    }

    private void getServerInfo(ApacheBinaryInfo info, String[] args) {
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
            else if (arg.startsWith("-f")) {
                info.conf = arg.substring(2, arg.length());
                if (info.conf.length() == 0) {
                    info.conf = args[i+1];
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

        if (info.conf != null) {
            //check that httpd.conf exists and is absolute 
            File conf = new File(info.conf);
            if (!conf.isAbsolute() && (info.root != null)) {
                conf = new File(info.root, info.conf);
            }
            if (!conf.exists()) {
                info.conf = null; //use the defaults
            }
        }else{
            for(String conf:defaultConfs){
                File cf=new File(info.root,conf);
                if(cf.exists() && cf.isFile()){
                    info.conf=cf.getAbsolutePath();
                }
            }
        }
        log.debug("[getServerInfo] info.conf=" + info.conf + ", info.root=" + info.root + ", info.name=" + info.name);
    }

    //if httpd is started with a relative path, try to find it
    //using the -d or -f flags if given
    private String findAbsoluteExe(String httpd, String[] args) {
        //e.g. ./bin/httpd
        ApacheBinaryInfo info = new ApacheBinaryInfo();
        getServerInfo(info, args);
        String[] paths = { info.root, info.conf };
        for (int i=0; i<paths.length; i++) {
            String path = paths[i];
            if (path == null) {
                continue;
            }
            log.debug("Attempting to resolve '" + httpd +
                      "' relative to '" + path + "'");
            File dir = new File(path);
            while (dir != null) {
                File exe = new File(dir, httpd);
                if (exe.exists()) {
                    log.debug("Relative '" + httpd +
                              "' resolved to '" + exe + "'");
                    try {
                        return exe.getCanonicalPath();
                    } catch (IOException e) {
                        return exe.getPath();
                    }
                }
                dir = dir.getParentFile();
            }                    
        }
        return null;
    }

    private void findServerProcess(List servers, String query,
                                   String version) {
        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String httpd;
            if ((httpd = getProcExe(pids[i])) == null) {
                httpd = getConfigProperty("exe");
            }

            if (httpd == null) {
                continue;
            }

            String[] args = getProcArgs(pids[i]);
            if (!new File(httpd).isAbsolute()) {
                String exe = findAbsoluteExe(httpd, args);
                if (exe == null) {
                    log.warn("Unable to get absolute path for pid=" + pids[i] +
                             ", args=" + java.util.Arrays.asList(args));
                    if (!new File(httpd).exists()) {
                        continue;
                    } //else fallthru.. unlikely, but permit an ln -s workaround
                }
                else {
                    httpd = exe;
                }
            }

            ApacheBinaryInfo info = ApacheBinaryInfo.getInfo(httpd, version);

            if (info == null) {
                continue;
            }

            info.pid = pids[i];
            getServerInfo(info, args);

            if (info.root == null) {
                continue;
            }

            servers.add(info);
        }
    }

    protected List getServerProcessList(String version,
                                        String[] queries) {
        ArrayList servers = new ArrayList();

        for (int i=0; i<queries.length; i++) {
            findServerProcess(servers, queries[i], version);
        }

        return servers;
    }

    private void addTrackConfig(ConfigResponse config) {
        String[] keys = {
            LogFileTrackPlugin.PROP_FILES_SERVER,
            ConfigFileTrackPlugin.PROP_FILES_SERVER
        };
        for (int i=0; i<keys.length; i++) {
            if (config.getValue(keys[i]) != null) {
                continue;
            }
            String val = getConfigProperty(keys[i]);
            if (val != null) {
                config.setValue(keys[i], val);
            }
        }
    }

    private void setLogTrack(ConfigResponse config, String file) {
        config.setValue(LogFileTrackPlugin.PROP_FILES_SERVER, file);
    }

    private void setConfigTrack(ConfigResponse config, String file) {
        config.setValue(ConfigFileTrackPlugin.PROP_FILES_SERVER, file);
    }

    private void setPidFile(ConfigResponse config, String file) {
        config.setValue(ApacheControlPlugin.PROP_PIDFILE, file);
    }

    protected boolean configureServer(ServerResource server, ApacheBinaryInfo binary)
        throws PluginException {
        
        ConfigResponse metricConfig, productConfig, controlConfig;
        String installpath = server.getInstallPath();
        File snmpConfig = getSnmpdConf(installpath);
        boolean snmpConfigExists = snmpConfig.exists();

        controlConfig = getControlConfig(installpath);

        if (binary != null) {
            ConfigResponse cprops = new ConfigResponse(binary.toProperties());
            server.setCustomProperties(cprops);
        }

        getLog().debug("[configureServer] snmpConfigExists=" + snmpConfigExists
                + " this.discoverModSnmp=" + this.discoverModSnmp
                + " this.discoverModStatus="+this.discoverModStatus);
        
        if (snmpConfigExists || this.discoverModSnmp) {
            if (!snmpConfigExists) {
                log.debug(snmpConfig +
                          " does not exist, cannot auto-configure " +
                          server.getName());
            }
            metricConfig = getSnmpConfig(snmpConfig);

            productConfig = getProductConfig(metricConfig);
            populateListeningPorts(binary.pid , productConfig , true);         

            if (binary.conf == null) {
                String cfgPath = installpath;
                if (snmpConfigExists) {
                    cfgPath = snmpConfig.getParentFile().getParent();
                }
                for (String conf : defaultConfs) {
                    File cf = new File(cfgPath, conf);
                    getLog().debug("[configureServer] cf="+cf+" ("+(cf.exists() && cf.isFile())+")");
                    if (cf.exists() && cf.isFile()) {
                        binary.conf = cf.getAbsolutePath();
                    }
                }
            }

            if (productConfig != null) {
                if (binary.conf != null) {
                    productConfig.setValue("ServerConf", binary.conf);
                }
                addTrackConfig(productConfig);
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
        	populateListeningPorts(binary.pid , productConfig, true);

        	//meant for command-line testing: -Dhttpd.url=http://localhost:8080/
        	if (configureURL(getManagerProperty("httpd.url"), productConfig)) {
        		server.setMeasurementConfig();
        	}
        	else if (configureURL(binary, productConfig)){
        		server.setMeasurementConfig();
        		if (binary.conf != null) {
        			//ServerRoot location overrides compiled in HTTPD_ROOT
        			ConfigResponse srControlConfig = getControlConfig(binary.root);
                    if (srControlConfig != null) {
                        controlConfig = srControlConfig;
                    }
                }
            }
            addTrackConfig(productConfig);
            if (controlConfig != null) {
                String pidfile = productConfig.getValue(ApacheControlPlugin.PROP_PIDFILE);
                if (pidfile != null) {
                    setPidFile(controlConfig, pidfile); //propagate from httpd.conf
                }
                setControlConfig(server, controlConfig);
            }
            server.setDescription("mod_status monitor");
            server.setType(TYPE_HTTPD);
            String path = binary.conf;
            if (path == null) {
                path = server.getInstallPath();
            }
            else {
                //use -f file for path and AIID
                //since binary installpath will be the same
                //for multiple instances
                server.setInstallPath(path);
            }
            //in the event that mod_snmp is added later,
            //we won't clash w/ the other types
            server.setIdentifier(TYPE_HTTPD + " " + path);
            server.setProductConfig(productConfig);

            return true;
        }
        else {
            log.debug("Ignoring " + server.getName() +
                      " at " + server.getInstallPath());
            return false;
        }
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
        config.setProperty(PROP_SERVER_NAME, server.name);
        config.setProperty(PROP_SERVER_PORT, server.port);
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

    private boolean exists(String name) {
        if (name == null) return false;
        return new File(name).exists();
    }

    protected ConfigResponse getControlConfig(String path) {
        Properties props = new Properties();

        if (isWin32()) {
            String sname=getWindowsServiceName();
            if(sname!=null)
                props.setProperty(Win32ControlPlugin.PROP_SERVICENAME,sname);
            return new ConfigResponse(props);
        }
        else {
            String file = path + "/" + getDefaultControlScript();

            if (!exists(file)) {
                file = getConfigProperty(ApacheControlPlugin.PROP_PROGRAM);
            }
            if (file == null) {
                return null;
            }

            props.setProperty(ApacheControlPlugin.PROP_PROGRAM, file);

            file = path + "/" + getDefaultPidFile();
            if (!exists(file)) {
                file = getConfigProperty(ApacheControlPlugin.PROP_PIDFILE);
            }
            if (file != null) {
                props.setProperty(ApacheControlPlugin.PROP_PIDFILE, file);
            }
            return new ConfigResponse(props);
        }
    }

    private String[] getPtqlQueries() {
        if (isWin32()) {
            return PTQL_QUERIES_WIN32;
        }
        else {
            return PTQL_QUERIES;
        }
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {

        setPlatformConfig(platformConfig);
        String version = getTypeInfo().getVersion();
        List servers = new ArrayList();
        List binaries = getServerProcessList(version, getPtqlQueries());

        for (int i=0; i<binaries.size(); i++) {
            ApacheBinaryInfo info =
                (ApacheBinaryInfo)binaries.get(i);

            List found = getServerList(info.root, info.version, info);
            if (found != null) {
                for (int j=0; j<found.size(); j++) {
                    ServerResource server = (ServerResource)found.get(j);
                    //apply externally defined AUTOINVENTORY_NAME, etc.
                    discoverServerConfig(server, info.pid);
                    servers.add(server);
                }
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

    protected boolean configureURL(ApacheBinaryInfo binary, ConfigResponse config) {
        String port=null, address=null;
        if (binary.conf != null) {
            //XXX config parsing not complete
            Map cfg = ApacheBinaryInfo.parseConfig(binary.conf);
            String listen = (String)cfg.get("Listen");
            if (listen != null) {
                int ix = listen.lastIndexOf(':');
                if (ix == -1) {
                    port = listen;
                    address = (String)cfg.get("ServerName");
                    if (address != null) {
                        ix = address.indexOf(':');
                        if (ix != -1) {
                            address = address.substring(0, ix);
                        }
                    }
                }
                else {
                    address = listen.substring(0, ix);
                    port = listen.substring(ix+1);
                }
            }
            setConfigTrack(config, binary.conf);
            String root = (String)cfg.get("ServerRoot");
            if (root != null) {
                binary.root = root;
            }
            String log = (String)cfg.get("ErrorLog");
            if (binary.serverRootRelative(log).exists()) {
                setLogTrack(config, log);                        
            }
            String pid = (String)cfg.get("PidFile");
            if (pid != null) {
                File pidFile = binary.serverRootRelative(pid);
                if (pidFile.exists()) {
                    setPidFile(config, pidFile.getPath());    
                }
            }
        }
        if (port == null) {
            port = getConfigProperty(Collector.PROP_PORT, "80");
        }
        if (address == null) {
            address = getListenAddress(port, this.defaultIp);
        }

        config.setValue(Collector.PROP_PROTOCOL,
                        getConnectionProtocol(port));
        config.setValue(Collector.PROP_SSL, isSSL(port));
        config.setValue(Collector.PROP_HOSTNAME, address);
        config.setValue(Collector.PROP_PORT, port);
        config.setValue(Collector.PROP_PATH,
                        getConfigProperty(Collector.PROP_PATH, "/server-status"));
        return true;
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
    
        getLog().debug("[discoverVHostServices] serverConfig="+serverConfig);
        if (serverConfig.getValue(SNMPClient.PROP_IP) == null) {
            return null; //server type "Apache httpd"
        }
        
        Map<String,ApacheVHost> vHosts;
        ApacheConf aConf;
        try {
            File ServerConf=new File(serverConfig.getValue("ServerConf"));
            aConf=new ApacheConf(ServerConf);
            vHosts=aConf.getVHosts();
        } catch (IOException ex) {
            throw new PluginException("Error getting VHosts", ex);
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

            String exceptedServerName = server.name + ":" + server.port;
            ApacheVHost vhost=vHosts.get(exceptedServerName);
            if (vhost!=null) {
                config.setValue(Collector.PROP_HOSTNAME, vhost.getIp());
                config.setValue(Collector.PROP_PORT, vhost.getPort());
            }else if (config.getValue(Collector.PROP_HOSTNAME) == null) {
                ApacheListen parseListen = ApacheConf.parseListen(exceptedServerName);
                config.setValue(Collector.PROP_HOSTNAME, parseListen.getIp());
                config.setValue(Collector.PROP_PORT, parseListen.getPort());
            }
            
            config.setValue(PROP_SERVER_NAME, server.name);
            config.setValue(PROP_SERVER_PORT, server.port);

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
