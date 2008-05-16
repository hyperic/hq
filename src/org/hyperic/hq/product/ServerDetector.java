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

package org.hyperic.hq.product;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xpath.XPathAPI;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;

import org.hyperic.hq.autoinventory.ServerSignature;

import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.file.FileUtil;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Encapsulate the know-how to determine all kinds of 
 * server-specific information about a single type of server.
 */
public abstract class ServerDetector
    extends GenericPlugin
    implements RuntimeDiscoverer {

    private static final String SERVER_SIGS =
        "etc/hq-server-sigs.properties";
    private static final String VERSION_FILE = "VERSION_FILE";
    private static final String INSTALLPATH_MATCH = "INSTALLPATH_MATCH";
    private static final String INSTALLPATH_NOMATCH = "INSTALLPATH_NOMATCH";
    private static final String INSTALLPATH = "INSTALLPATH";
    private static final String INVENTORY_ID = "INVENTORY_ID";

    private static final String[] NO_ARGS = new String[0];
    private static final long[] NO_PIDS = new long[0];
    private static final List NO_MODULES = Arrays.asList(NO_ARGS);

    private static Sigar sigarImpl = null;
    private static SigarProxy sigar = null;
    private ServerSignature sig = null;
    private ProductPluginManager ppm;
    private AutoinventoryPluginManager manager;
    private ConfigResponse cprops = null;
    private String description = null;
    private Properties properties;

    public ServerDetector() {}

    /**
     * @deprecated - Plugins should not use this method.
     * @see #discoverServers
     * @see #discoverServices
     */
    public RuntimeDiscoverer getRuntimeDiscoverer() {
        //XXX get rid of this method.
        return this;
    }

    /**
     * If plugins do not override discoverServers or discoverServices,
     * returning false here will prevent those methods from being called.
     *  
     * @return true by default.
     */
    public boolean isRuntimeDiscoverySupported() {
        return true;
    }

    private AIPlatformValue getPlatform(HashMap platforms, String fqdn) {
        AIPlatformValue platform = (AIPlatformValue)platforms.get(fqdn);
        if (platform != null) {
            return platform;
        }

        platform = new AIPlatformValue();
        platform.setFqdn(fqdn);
        platforms.put(fqdn, platform);

        return platform;
    }
    
    private RuntimeResourceReport
        discoverServerResources(int serverId,
                                AIPlatformValue sPlatform,
                                ConfigResponse config,
                                List servers)
        throws PluginException {

        if (servers.size() == 0) {
            return null;
        }

        HashMap platforms = new HashMap();
        
        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);

        for (int i=0; i<servers.size(); i++) {
            AIPlatformValue platform;
            ServerResource server = (ServerResource)servers.get(i);
            AIServerExtValue resource =
                (AIServerExtValue)server.getResource(); 
            String fqdn = server.getPlatformFqdn();
            if (fqdn != null) {
                platform = getPlatform(platforms, fqdn);
            }
            else {
                platform = sPlatform;
                platforms.put("__DEFAULT__", platform);
            }
            //this server does not have runtime ai enable since
            //we discovered it and its services.
            resource.setServicesAutomanaged(true);

            AIServiceValue[] services =
                new AIServiceValue[server.services.size()];
            server.services.toArray(services);
            resource.setAIServiceValues(services);

            platform.addAIServerValue(resource);            
        }

        for (Iterator it=platforms.values().iterator();
             it.hasNext();)
        {
            AIPlatformValue platform = (AIPlatformValue)it.next(); 
            rrr.addAIPlatform(platform);
        }
        
        return rrr;
    }
    
    /**
     * @deprecated - Plugins should not use this method.
     * @see #discoverServers
     * @see #discoverServices
     */
    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue platform,
                                                   ConfigResponse config)
        throws PluginException {

        this.cprops = null;

        //uncommon, weblogic, websphere and iplanet discover servers
        List servers = discoverServers(config);

        if (servers != null) {
            getLog().debug("discovered " + servers.size() + " servers");
            return discoverServerResources(serverId, platform, config, servers);
        }
        
        //common case discover services only
        List services = discoverServices(config);
        if (services == null) {
            getLog().debug("no services discovered");
            return null;
        }

        getLog().debug("discovered " + services.size() + " services");

        Long timeNow = new Long(System.currentTimeMillis());
        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);
        AIServiceValue[] values = new AIServiceValue[services.size()];

        for (int i=0; i<services.size(); i++) {
            ServiceResource service = (ServiceResource)services.get(i);

            service.resource.setCTime(timeNow);

            //XXX do we actually need to do this?
            service.resource.setServerId(serverId);

            values[i] = service.resource;
        }
        
        AIServerExtValue server = new AIServerExtValue();
        if (this.cprops != null) {
            try {
                server.setCustomProperties(cprops.encode());
            } catch (EncodingException e) {
                getLog().error("Error encoding cprops: " + e.getMessage());
            }
        }
        if (this.description != null) {
            server.setDescription(this.description);
        }

        server.setPlaceholder(true);
        server.setId(new Integer(serverId));
        server.setAIServiceValues(values);
        platform.addAIServerValue(server);
        rrr.addAIPlatform(platform);
        return rrr;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    protected void setCustomProperties(ConfigResponse cprops) {
        this.cprops = cprops;
    }

    /**
     * Override to discover servers for the server type of
     * the plugin instance.  In most cases plugins will override
     * discoverServices rather than this method.  
     * The general use of this method is where a plugin 
     * FileServerDetector or AutoServerDetector finds
     * the Admin server and this method discovers the managed server nodes.
     * Such plugins include WebLogic, WebSphere and iPlanet.
     *
     * @param config Configuration of the parent server resource.
     * @return List of type <a href="ServerResource">ServerResource</a>
     * @throws PluginException If an error occured during discovery.
     * @see #discoverServices
     * @see ServerResource
     * @since 1.7
     */
    protected List discoverServers(ConfigResponse config)
        throws PluginException {
        return null;
    }

    /**
     * Override to discover services for the server type of
     * the plugin instance.
     * @param config Configuration of the parent server resource.
     * @return List of type ServiceResource.
     * @throws PluginException If an error occured during discovery.
     */
    protected List discoverServices(ConfigResponse config)
        throws PluginException {
        return null;
    }

    public void init(PluginManager manager)
        throws PluginException
    {
        super.init(manager);
        this.manager = (AutoinventoryPluginManager)manager;
        this.ppm = (ProductPluginManager)manager.getParent();
        this.properties = manager.getProperties();
    }

    /**
     * @return The plugin manager for this plugin.
     */
    public AutoinventoryPluginManager getManager() {
        return this.manager;
    }
    
    /**
     * Helper for RegistryServerDetector implementors.
     * Gets the scan keys from hq-plugin.xml
     */
    public List getRegistryScanKeys() {
        if (this.data != null) {
            List keys =
                this.data.getRegistryScanKeys(getTypeInfo().getName());
            if (keys != null){
                return keys;
            }
        }

        return new ArrayList();
    }
    
    /**
     * The server signature is defined by plugins in
     * etc/hq-plugin.xml if the plugin implements
     * FileServerDetector or RegistryServerDetector.
     */
    public ServerSignature getServerSignature() {
        if (this.sig != null) {
            return this.sig;
        }

        //XXX we will be moving the hq-server-sigs.properties functionality
        //to hq-plugin.xml
        Properties props = loadProperties();
        if (props == null) {
            String name = getTypeInfo().getName();
            if (this.data != null) {
                List matches = this.data.getFileScanIncludes(name);
                List regkeys = this.data.getRegistryScanIncludes(name);
                //XXX nobody uses exclude, should we support it?
                this.sig = new ServerSignature(name, matches, null, regkeys);
            }
            //just need a sig w/ the server type to make the auto scan run
            else if (this instanceof AutoServerDetector) {
                this.sig =
                    new ServerSignature(name,
                                        new String[0],
                                        new String[0], 
                                        new String[0]);
            }
            else {
                this.sig = new ServerSignature();
            }
            return this.sig;
        }

        List matches = new ArrayList();
        List excludes = new ArrayList();
        List regkeys = new ArrayList();

        Enumeration propNames = props.propertyNames();

        String type = getTypeInfo().getName();
        String typeName = TypeInfo.formatName(type);
        String matchPrefix    = typeName + ".filescan.include.";
        String excludePrefix  = typeName + ".filescan.exclude.";
        String regMatchPrefix = typeName + ".regscan.include.";

        while (propNames.hasMoreElements()) {
            String prop = (String)propNames.nextElement();
            if (prop.startsWith(matchPrefix)) {
                matches.add(props.getProperty(prop));
            }
            else if (prop.startsWith(excludePrefix)) {
                excludes.add(props.getProperty(prop));
            }
            else if (prop.startsWith(regMatchPrefix)) {
                regkeys.add(props.getProperty(prop));
            }
        }

        this.sig =
            new ServerSignature(type, matches, excludes, regkeys);

        getLog().debug("Loaded ServerSignature for: " + getName());

        return this.sig;
    }

    private Properties loadProperties() {
        InputStream is = null;
        ClassLoader cl = this.getClass().getClassLoader();

        try {
            is = cl.getResourceAsStream(SERVER_SIGS);
            if (is == null) {
                return null;
            }

            Properties props = new Properties();
            props.load(is);
            String msg =
                "Loaded " + SERVER_SIGS + " for: " + getName();
            getLog().debug(msg);
            return props;
        } catch (IOException e) {
            String msg =
                "Unable to load " + SERVER_SIGS + " for: " + getName() +
                ": " + e.getMessage();
            getLog().error(msg, e);
            return null;
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }
    }

    /**
     * Test if server type version filters apply:
     * VERSION_FILE - Return true if given file exists within installpath
     * INSTALLPATH_MATCH - Return true if installpath matches given substring
     * INSTALLPATH_NOMATCH - Return false if installpath matches given substring
     * @param installpath The server instance installpath
     * @return false installpath does not match type version criteria, true otherwise
     */
    protected boolean isInstallTypeVersion(String installpath) {
        String versionFile = getTypeProperty(VERSION_FILE);
        String installPathMatch = getTypeProperty(INSTALLPATH_MATCH);
        String installPathNoMatch = getTypeProperty(INSTALLPATH_NOMATCH);

        if (versionFile != null) {
            File instPath = new File(installpath);
            if (instPath.isFile() && !instPath.isDirectory()) {
                instPath = instPath.getParentFile();
            }
            File file = (instPath != null) ? new File(instPath, versionFile) :
                new File(installpath, versionFile);
            if (!file.exists()) {
                String[] expanded = PluginLoader.expand(file);
                if ((expanded == null) || (expanded.length == 0)) {
                    getLog().debug(file + " does not exist, skipping");
                    return false;
                }
                else {
                    getLog().debug(VERSION_FILE + "=" + versionFile +
                                   " matches -> " + expanded[0]);
                }
            }
        }

        if (installPathMatch != null) {
            if (!(installpath.indexOf(installPathMatch) != -1)) {
                getLog().debug(installpath + " not a match for " +
                               installPathMatch + ", skipping");
                return false;
            }
        }

        if (installPathNoMatch != null) {
            if (installpath.indexOf(installPathNoMatch) != -1) {
                getLog().debug(installpath + " is a match for " +
                               installPathNoMatch + ", skipping");
                return false;
            }
        }

        return true;
    }

    /**
     * Initialize an ServerResource with default values.
     * Defaults are set for:
     * <ul>
     * <li> Type - Using getTypeInfo().getName() 
     * <li> Name - Using getPlatformName() + " " + Type
     * <li> InstallPath - The installpath param passed to getCanonicalPath.
     * <li> Identifier - The installpath param.
     * </ul>
     * @param installpath Used to set InstallPath and Identifier
     */
    protected ServerResource createServerResource(String installpath) {

        ServerResource server = new ServerResource();

        String type = getTypeInfo().getName();
        server.setType(type);
        server.setName(getPlatformName() + " " + type);
        server.setInstallPath(getCanonicalPath(installpath));
        //allow hardcoded property to override discovered installpath
        installpath = getTypeProperty(INSTALLPATH);
        if (installpath != null) {
            server.setInstallPath(installpath);
        }
        String aiid = getTypeProperty(INVENTORY_ID);
        if (aiid == null) {
            aiid = server.getInstallPath();
        }
        server.setIdentifier(aiid);

        return server;
    }

    /**
     * @return ServiceResource with setType(this, type)
     */
    protected ServiceResource createServiceResource(String type) {
        ServiceResource service = new ServiceResource();
        service.setType(this, type);
        return service;
    }

    /**
     * Format the auto-inventory name as defined by the plugin, for example: 
     * <property name="AUTOINVENTORY_NAME" value="My %Name% Service @ %Location%"/> 
     * %value%s are replaced using the ConfigResponse parameters.
     * @param type The resource type name used to lookup AUTOINVENTORY_NAME
     * @param parentConfig The platform or server configuration
     * @param config The server or services configuration
     * @param cprops Custom properties
     * @return The formatted name or null if AUTOINVENTORY_NAME is not defined
     * for the given resource type name
     */
    protected String formatAutoInventoryName(String type,
                                             ConfigResponse parentConfig,
                                             ConfigResponse config,
                                             ConfigResponse cprops) {
        String name =
            getTypeProperty(type, "AUTOINVENTORY_NAME");

        if (name == null) {
            return null;
        }

        if (parentConfig != null) {
            name = Metric.translate(name, parentConfig);
        }
        if (config != null) {
            name = Metric.translate(name, config);
        }
        if (cprops != null) {
            name = Metric.translate(name, cprops);
        }

        return name;
    }

    /**
     * @see FileUtil#getParentDir(String)
     */
    protected static String getParentDir(String path) {
        return FileUtil.getParentDir(path);
    }

    /**
     * @see FileUtil#getParentDir(String, int)
     */
    protected static String getParentDir(String path, int levels) {
        return FileUtil.getParentDir(path, levels);
    }
    
    /**
     * Fixup the installpath.
     * Quotes are removed.
     * Canonicalized to removing double slashes, uppercase drive letter, etc.
     */
    protected static String getCanonicalPath(String installpath) {
        if (installpath.charAt(0) == '"') {
            int idx = installpath.indexOf("\"", 1);
            if (idx == -1) {
                installpath = installpath.substring(1);
            }
            else {
                installpath = installpath.substring(1, idx);
            }
        }

        try {
            installpath = new File(installpath).getCanonicalPath();
        } catch (IOException e) {
        }

        return installpath;
    }

    /**
     * @return A SigarProxyCache instance with an expiration time of
     * 10 minutes.  The cache is cleared by the plugin manager after
     * all AutoServerDetectors have been run.
     */
    protected static SigarProxy getSigar() {
        if (sigar == null) {
            //long timeout, we are not using this to gather metrics
            //but to discover running processes
            int timeout = 10 * 60 * 1000; //10 minutes
            sigarImpl = new Sigar();
            sigar = SigarProxyCache.newInstance(sigarImpl, timeout);
        }
        return sigar;
    }

    protected String getListenAddress(String port) {
        return getListenAddress(Long.parseLong(port));
    }
    
    protected String getListenAddress(long port) {
        String address = null;

        try {
            address =
                getSigar().getNetListenAddress(port);
        } catch (SigarException e) {
        }

        if ((address == null) ||
            NetFlags.isLoopback(address) ||
            NetFlags.isAnyAddress(address))
        {
            address = "localhost";
        }

        getLog().debug("ListenAddress for port " + port + "=" + address);

        return address;
    }

    static void clearSigarCache() {
        if (sigar == null) {
            return;
        }
        SigarProxyCache.clear(sigar);
        sigarImpl.close();
        sigar = null;
    }

    /**
     * Wrapper for Sigar.getProcArgs which catches SigarException
     * and returns a String[] with length 0 if the SigarException
     * is caught.
     * @param pid Process identifier
     * @return Arguments that were passed to the process.
     */
    protected static String[] getProcArgs(long pid) {
        try {
            return getSigar().getProcArgs(pid);
        } catch (SigarException e) {
            return NO_ARGS;
        }
    }

    /**
     * Wrapper for Sigar.getProcModules which catches SigarException
     * and returns a String[] with length 0 if the SigarException
     * is caught.
     * @param pid Process identifier
     */
    protected static List getProcModules(long pid) {
        try {
            return getSigar().getProcModules(pid);
        } catch (SigarException e) {
            return NO_MODULES;
        }
    }

    protected static String getProcExe(long pid) {
        return getProcExe(pid, null);
    }

    /**
     * Attempt to find the absolute name of the process executable.
     * If the name cannot be determined, <code>null</code> is returned.
     * @param pid Process identifier
     * @param name Binary base name to match against
     * @return The process executable name.
     */
    protected static String getProcExe(long pid, String name) {
        try {
            String exe = getSigar().getProcExe(pid).getName();
            //possible to be "" on solaris
            if (exe.length() > 0) {
                return exe;
            } //else fallthru
        } catch (SigarException e) {
            //likely permission denied
        }

        String argv0 = null;
        String[] args = getProcArgs(pid);
        if (args.length != 0) {
            //might not be absolute path.
            argv0 = args[0];
            File bin = new File(argv0);
            if (bin.exists() && bin.isAbsolute()) {
                return argv0;
            }
        }

        List modules = getProcModules(pid);
        if (modules.size() > 0) {
            if (name == null) {
                return (String)modules.get(0);
            }
            name = File.separator + name;
            for (int i=0; i<modules.size(); i++) {
                String bin = (String)modules.get(i);
                if (bin.endsWith(name)) {
                    return bin;
                }
            }
        }

        return argv0;
    }

    /**
     * Attempt to get the current working directory of a process.
     * If the directory cannot be determined, <code>null</code> is returned.
     * @param pid Process identifier
     * @return The process current working directory.
     */
    protected static String getProcCwd(long pid) {
        try {
            return getSigar().getProcExe(pid).getCwd();
        } catch (SigarException e) {
            return null;
        }
    }

    /**
     * Wrapper for Sigar's ProcessFinder.find method.
     * @param query SIGAR Process Table Query
     * @return Array of pids that match the query, length == 0 if
     * there were no matches.
     */
    protected static long[] getPids(String query) {
        try {
            return ProcessFinder.find(getSigar(), query);
        } catch (SigarException e) {
            return NO_PIDS;
        }
    }
    
    /**
     * @param name The Service name shown in the Windows service panel
     *  Properties General tab, not the Display name.  For example,
     * "Terminal Services" is the Display name, "TermService" is the
     * Service name that should be used here.
     * @return true If the service exists and is running, false otherwise.
     */
    public boolean isWin32ServiceRunning(String name) {
        if (!isWin32()) {
            return false;
        }
        return Win32MeasurementPlugin.isServiceRunning(name);
    }

    public boolean isSSLPort(String port) {
        return port.endsWith(Collector.DEFAULT_HTTPS_PORT);
    }

    public String getConnectionProtocol(String port) {
        if (port.equals(Collector.DEFAULT_FTP_PORT)) {
            return Collector.PROTOCOL_FTP;
        }
        else {
            //note: for 'https' we use the 'http' protocol
            //with flag ssl=true
            return Collector.PROTOCOL_HTTP;
        }
    }

    /**
     * DocumentBuilder.parse() wrapper
     * @param file File to parse
     * @return parsed Document
     * @throws IOException For any exception
     */
    protected Document getDocument(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);        
        try {
            DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(fis);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            fis.close();
        }
    }

    /**
     * XPathAPI.eval() wrapper.  XPathAPI.eval never returns null,
     * however this wrapper method will return null if
     * XObject.toString().length() == 0
     * @param node Node to search
     * @param xpath XPath string
     * @return XObject.toString() or null if Exception is caught
     */
    protected String getXPathValue(Node node, String xpath) {
        try {
            String val =
                XPathAPI.eval(node, xpath).toString();
            if ((val == null) || (val.length() == 0)) {
                return null;
            }
            return val; 
        } catch (Exception e) {
            return null;
        }
    }

    protected String getXPathValue(File file, String xpath) {
        try {
            return getXPathValue(getDocument(file), xpath);
        } catch (Exception e) {
            return null;
        }
    }

    protected ConfigSchema getConfigSchema(String name, int type) {
        if (this.data == null) {
            return null;
        }
        return this.data.getConfigSchema(name, type);        
    }
    
    private void mergeConfigDefaults(ConfigResponse config,
                                     ConfigSchema schema) {
        mergeConfigDefaults(config, schema, true);
    }

    private void mergeConfigDefaults(ConfigResponse config,
                                     ConfigSchema schema,
                                     boolean allowOverride) {

        List options = schema.getOptions();
        for (int i=0; i<options.size(); i++) {
            ConfigOption opt = (ConfigOption)options.get(i);
            String key = opt.getName();

            if (allowOverride) {
                //commandline or agent.properties
                String propValue =
                    this.properties.getProperty(key);
                if (propValue != null) {
                    config.setValue(key, propValue);
                    continue;
                }
            }

            if (config.getValue(key) == null) {
                String defval = opt.getDefault();
                if ((defval != null) && (defval.length() != 0)) {
                    config.setValue(key, opt.getDefault());
                }
            }
        }        
    }

    //can't use PluginManager/getTypeInfo() combo since
    //the TypeInfo will be that of the server.  lookup
    //directly in PluginData for services.
    private void mergeConfigDefaults(String name, int type,
                                     ConfigResponse config) {
        ConfigSchema schema = getConfigSchema(name, type);
        if (schema == null){
            return;
        }
        mergeConfigDefaults(config, schema);
    }

    private void mergeConfigDefaults(String name,
                                     PluginManager manager,
                                     ConfigResponse config) {
        TypeInfo type = getTypeInfo();
        ConfigSchema schema;

        try {
            GenericPlugin plugin = manager.getPlugin(name);
            schema = plugin.getConfigSchema(type, config);
        } catch (PluginNotFoundException e) {
            getLog().error("'" + name + "' " + manager.getName() +
                           " plugin not found", e);
            return;
        }

        mergeConfigDefaults(config, schema);
    }
    
    /**
     * Merge default values from server's product ConfigSchema and
     * saves to ServerResource.setProductConfig.
     */
    protected void setProductConfig(ServerResource server,
                                    ConfigResponse config) {

        String name = getProductPlugin(server.getType()).getName();
        mergeConfigDefaults(name, this.ppm, config);
        server.setProductConfig(config);
    }
    
    //NOTE we pass getName() to mergeConfigDefaults, important for
    //platform-specific ConfigSchemas
    /**
     * Merge default values from server's measurement ConfigSchema and
     * saves to ServerResource.setMeasurementConfig.
     */
    protected void setMeasurementConfig(ServerResource server,
                                        ConfigResponse config) {

        PluginManager manager = this.ppm.getMeasurementPluginManager();
        mergeConfigDefaults(getName(), manager, config);
        server.setMeasurementConfig(config);
    }

    /**
     * Merge default values from server's Custom Properties schema and
     * saves to ServerResource.setCustomProperties.
     */
    protected void setCustomProperties(ServerResource server,
                                       ConfigResponse config) {
        ConfigSchema cprops = getCustomPropertiesSchema();
        mergeConfigDefaults(config, cprops, false);
        server.setCustomProperties(config);
    }

    /**
     * Merge default values from server's control ConfigSchema and
     * saves to ServerResource.setControlConfig.
     */
    protected void setControlConfig(ServerResource server,
                                    ConfigResponse config) {

        PluginManager manager = this.ppm.getControlPluginManager();
        mergeConfigDefaults(getName(), manager, config);
        server.setControlConfig(config);
    }

    /**
     * Merge default values from service's product ConfigSchema and
     * saves to ServiceResource.setProductConfig.
     */
    protected void setProductConfig(ServiceResource service,
                                    ConfigResponse config) {
        
        mergeConfigDefaults(service.getType(),
                            ProductPlugin.CFGTYPE_IDX_PRODUCT,
                            config);

        service.setProductConfig(config);
    }
    
    /**
     * Merge default values from service's measurement ConfigSchema and
     * saves to ServiceResource.setMeasurementConfig.
     */
    protected void setMeasurementConfig(ServiceResource service,
                                        ConfigResponse config) {

        mergeConfigDefaults(service.getType(),
                            ProductPlugin.CFGTYPE_IDX_MEASUREMENT,
                            config);

        service.setMeasurementConfig(config);
    }
    
    /**
     * Merge default values from services's control ConfigSchema and
     * saves to ServiceResource.setControlConfig.
     */
    protected void setControlConfig(ServiceResource service,
                                    ConfigResponse config) {

        mergeConfigDefaults(service.getType(),
                            ProductPlugin.CFGTYPE_IDX_CONTROL,
                            config);

        service.setControlConfig(config);
    }
    
    public Map getServiceInventoryPlugins() {
        TypeInfo type = getTypeInfo();
        return this.data.getServiceInventoryPlugins(type.getName());
    }
    
    public List getServiceConfigs(String type) {
        return ((AutoinventoryPluginManager)getManager()).
            getServiceConfigs(type);
    }
}
