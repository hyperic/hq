/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.PropertyEncryptionUtil;
import org.hyperic.util.PropertyUtil;
import org.hyperic.util.PropertyUtilException;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.security.SecurityUtil;

/**
 * The configuration object for the AgentDaemon.  This class performs
 * validation on application properties, and provides a type-specific
 * API for consumption by the Agent.
 */
public class AgentConfig {
    protected final static Log logger = LogFactory.getLog(AgentConfig.class.getName());

    private static final String DEV_URANDOM = "/dev/urandom";
    
    //moved from ClientPluginDeployer
    public static final String WORK_DIR = "work";

    static {
        //linux/freebsd/etc may block on /dev/random forever
        if (new File(DEV_URANDOM).exists()) {
            System.setProperty("java.security.egd",
                               "file:" + DEV_URANDOM);
        }
    }
    
    // properties used with JSW
    public static final String JSW_PROP_AGENT_BUNDLE = "set.HQ_AGENT_BUNDLE";
    public static final String JSW_PROP_AGENT_ROLLBACK_BUNDLE = 
        "set.HQ_AGENT_ROLLBACK_BUNDLE";
    public static final String JSW_PROP_AGENT_JAVA_HOME = "set.HQ_JAVA_HOME";
    
    public static final String PROP_LATHER_PROXYHOST = "lather.proxyHost";
    public static final String PROP_LATHER_PROXYPORT = "lather.proxyPort";

    public static final String IP_GLOBAL = "*";
    
    private static final String DEFAULT_PROXY_HOST = "";
    private static final int DEFAULT_PROXY_PORT = -1;
    private static final int DEFAULT_NOTIFY_UP_PORT = -1;
    private static final String DEFAULT_ENBALED_CIPHERS = "SSL_RSA_WITH_RC4_128_MD5,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA";

    //PluginDumper needs these to be constant folded
    public static final String PDK_DIR_KEY = "agent.pdkDir";
    public static final String PDK_LIB_DIR_KEY = "agent.pdkLibDir";
    public static final String PDK_PLUGIN_DIR_KEY = "agent.pdkPluginDir";
    public static final String PDK_WORK_DIR_KEY = "agent.pdkWorkDir";
    public static final String AGENT_BUNDLE_HOME = "agent.bundle.home";
  
    //Property name for keystore
    public static final String SSL_KEYSTORE_ALIAS = "agent.keystore.alias";
    public static final String DEFAULT_SSL_KEYSTORE_ALIAS = "hq";
    public static final String SSL_KEYSTORE_PATH = "agent.keystore.path";
    public static final String SSL_KEYSTORE_PASSWORD = "agent.keystore.password";
    public static final String SSL_KEYSTORE_ACCEPT_UNVERIFIED_CERT = "accept.unverified.certificates";
    public static final String DEFAULT_AGENT_KEY_FILE_NAME = "propEncKey";
    // The following final objects are the properties which are usable
    // within the configuation object.  The first element in the array
    // is the property name, the second is the default value
    public static final String[] PROP_LISTENPORT         =
    { "agent.listenPort", Integer.toString(AgentCommandsAPI.DEFAULT_PORT) };
    public static final String[] PROP_LISTENIP           =
    { "agent.listenIp",   IP_GLOBAL };
    public static final String[] PROP_STORAGEPROVIDER    = 
    { "agent.storageProvider", 
      "org.hyperic.hq.agent.server.AgentDListProvider" };
    public static final String[] PROP_STORAGEPROVIDERINFO =
    { "agent.storageProvider.info", "${agent.dataDir}|m|1000|20|50" };
    public static final String[] PROP_INSTALLHOME =
    { "agent.install.home", System.getProperty("agent.install.home", System.getProperty("user.dir")) };
    // has no default since we want to throw an error when property is not set
    public static final String[] PROP_BUNDLEHOME =
    { AGENT_BUNDLE_HOME, System.getProperty(AGENT_BUNDLE_HOME) };    
    public static final String[] PROP_TMPDIR =
    { "agent.tmpDir", System.getProperty("agent.tmpDir", PROP_BUNDLEHOME[1] + "/tmp") };
    public static final String[] PROP_LOGDIR =
    { "agent.logDir", System.getProperty("agent.logDir", PROP_INSTALLHOME[1] + "/log") };
    public static final String[] PROP_DATADIR = 
    { "agent.dataDir", System.getProperty("agent.dataDir", PROP_INSTALLHOME[1] + "/data") };
    public static final String[] PROP_KEYSTORE_ACCEPT_UNVERIFIED_CERT = 
    { SSL_KEYSTORE_ACCEPT_UNVERIFIED_CERT, "true" }; //for pre-4.6 version upgrade only. Should be changed to "false" once pre-4.6 is not supported. 
    public static final String[] PROP_KEYSTORE_PATH = 
    { SSL_KEYSTORE_PATH, PROP_DATADIR[1] + "/keystore" };
    public static final String[] PROP_KEYSTORE_PASSWORD = 
    { SSL_KEYSTORE_PASSWORD, "storePW" };    
    public static final String[] PROP_LIB_HANDLERS = 
    { "agent.lib.handlers", PROP_BUNDLEHOME[1] + "/lib/handlers" };
    public static final String[] PROP_LIB_HANDLERS_LIB = 
    { "agent.lib.handlers.lib", PROP_LIB_HANDLERS[1] + "/lib" };
    public static final String[] PROP_PDK_DIR = 
    { PDK_DIR_KEY, System.getProperty(PDK_DIR_KEY, PROP_BUNDLEHOME[1] + "/pdk") };
    public static final String[] PROP_PDK_LIB_DIR = 
    { PDK_LIB_DIR_KEY, System.getProperty(PDK_LIB_DIR_KEY, PROP_PDK_DIR[1] + "/lib") };    
    public static final String[] PROP_PDK_PLUGIN_DIR = 
    { PDK_PLUGIN_DIR_KEY, 
        System.getProperty(PDK_PLUGIN_DIR_KEY, PROP_PDK_DIR[1] + "/plugins") };  
    public static final String[] PROP_PDK_WORK_DIR = 
    { PDK_WORK_DIR_KEY, 
        System.getProperty(PDK_WORK_DIR_KEY,
                PROP_PDK_DIR[1] + "/" + WORK_DIR) };      
    public static final String[] PROP_PROXYHOST = 
    { "agent.proxyHost", DEFAULT_PROXY_HOST };
    public static final String[] PROP_PROXYPORT = 
    { "agent.proxyPort", String.valueOf(DEFAULT_PROXY_PORT)};
    private static final String HQ_PASS = "agent.setup.camPword";
    
    public static final String PERSISTED_CONTROL_RESPONSES_DIR = PROP_DATADIR[1] + "/control_responses" ;
    
    // A property provided for testing rollback during agent auto-upgrade.
    // Set the property value to the bundle name that will fail when starting 
    // the agent so that the upgrade will revert to the rollback bundle.
    public static final String[] PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE = 
    { "agent.rollbackAgentBundleUpgrade", ""};    
    
    public static final String[] PROP_ENABLED_CIPHERS =
    	{ "agent.enabledCiphers", String.valueOf(DEFAULT_ENBALED_CIPHERS) };
    
    public static final String PROP_PROPFILE = "agent.propFile";
    
    public static final String DEFAULT_AGENT_PROPFILE_NAME = "agent.properties";
    
    public static final String AGENT_CONF_DIR = PROP_INSTALLHOME[1] + "/conf/";

    public static final String DEFAULT_PROPFILE = AGENT_CONF_DIR + DEFAULT_AGENT_PROPFILE_NAME;

    public static final String ROLLBACK_PROPFILE = "agent.rollbackPropFile";

    public static final String DEFAULT_ROLLBACKPROPFILE = AGENT_CONF_DIR + "rollback.properties";

    public static final String BUNDLE_PROPFILE = AGENT_CONF_DIR + DEFAULT_AGENT_PROPFILE_NAME;

    public static final String DEFAULT_AGENT_PROP_ENC_KEY_FILE_NAME = "agent.scu";

    public static final String DEFAULT_PROP_ENC_KEY_FILE = AGENT_CONF_DIR + DEFAULT_AGENT_PROP_ENC_KEY_FILE_NAME;

    public static final Set<String> ENCRYPTED_PROP_KEYS = new HashSet<String>();

    static {
        ENCRYPTED_PROP_KEYS.add(HQ_PASS);
        ENCRYPTED_PROP_KEYS.add(SSL_KEYSTORE_PASSWORD);
    }
    
    private static final String[][] propertyList = {
        PROP_LISTENPORT,
        PROP_PROXYHOST, 
        PROP_PROXYPORT,
        PROP_STORAGEPROVIDER,
        PROP_STORAGEPROVIDERINFO,
        PROP_INSTALLHOME,
        PROP_BUNDLEHOME,
        PROP_TMPDIR,
        PROP_LOGDIR,
        PROP_DATADIR,
        PROP_KEYSTORE_ACCEPT_UNVERIFIED_CERT,
        PROP_KEYSTORE_PATH,
        PROP_KEYSTORE_PASSWORD,
        PROP_LIB_HANDLERS,
        PROP_LIB_HANDLERS_LIB,
        PROP_PDK_DIR,
        PROP_PDK_LIB_DIR,
        PROP_PDK_PLUGIN_DIR,
        PROP_PDK_WORK_DIR, 
        PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE,
        PROP_ENABLED_CIPHERS
    };

    private int        listenPort;          // Port the agent should listen on
    private String     listenIp;            // IP the agent listens on
    private int        proxyPort;           // Proxy server port
    private String     proxyIp;             // IP for the proxy server
    private int        notifyUpPort;        // The port which the AgentClient defines 
                                            // where the CommandServer can connect to notify
                                            // it of successful startup.
    private String     storageProvider;     // Classname for the provider
    private String     storageProviderInfo;  // Argument to the storage init()
    private Properties bootProps;           // Bootstrap properties
    private String     tokenFile;

	private String[] enabledCiphers;        // Enabled ciphers to be used

    private File logdir;

	private AgentConfig(){
        this.proxyIp = AgentConfig.DEFAULT_PROXY_HOST;
        this.proxyPort = AgentConfig.DEFAULT_PROXY_PORT;
        this.notifyUpPort = AgentConfig.DEFAULT_NOTIFY_UP_PORT;
        this.enabledCiphers = AgentConfig.DEFAULT_ENBALED_CIPHERS.split(",");
    }

    /**
     * Create a new config object with default settings.  
     *
     * @return A newly initialized AgentConfig object
     */

    public static AgentConfig newInstance(){
        try {
            // verify that the agent bundle home has been properly defined
            // before populating the default properties
            checkAgentBundleHome();
            return newInstance(AgentConfig.getDefaultProperties());
        } catch(AgentConfigException exc){
            throw new AgentAssertionException("Default properties should " +
                                              "always be proper");
        }
    }

    // checks for the validity of the agent bundle home system property and
    // throws an
    // appropriate AgentConfigException if not valid
    private static void checkAgentBundleHome() throws AgentConfigException {
        String bundleHome = System.getProperty(PROP_BUNDLEHOME[0]);
        if (bundleHome == null) {
            throw new AgentConfigException(
                    "No value for required system property "
                            + PROP_BUNDLEHOME[0] + " provided!");
        }
        File bundleHomeDir = new File(bundleHome);
        if (!bundleHomeDir.isDirectory()) {
            throw new AgentConfigException("Invalid value "
                    + PROP_BUNDLEHOME[1] + " for required system property "
                    + PROP_BUNDLEHOME[0] + " provided!");
        }
    }

    public static void ensurePropertiesEncryption(String propertiesFileName) throws AgentConfigException {
        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    propertiesFileName, AgentConfig.DEFAULT_PROP_ENC_KEY_FILE, ENCRYPTED_PROP_KEYS);
        } catch (PropertyUtilException exc) {
            throw new AgentConfigException(exc.getMessage());
        }
    }

    private static synchronized boolean loadProps(Properties props, File propFile) throws AgentConfigException {
        Properties tmpProps;
        try {
            tmpProps = PropertyUtil.loadProperties(propFile.getPath());
        } catch (PropertyUtilException exc) {
            throw new AgentConfigException(exc.getMessage());
        }
        if (!propFile.exists()) {
            logger.error(propFile + " does not exist");
            return false;
        }
        if (!propFile.canRead()) {
            logger.error("can't read " + propFile);
            return false;
        }
        try {
            String propEncKey = PropertyEncryptionUtil.getPropertyEncryptionKey(AgentConfig.DEFAULT_PROP_ENC_KEY_FILE);

            for (Enumeration<?> propKeys = tmpProps.propertyNames(); propKeys.hasMoreElements();) {
                String key = (String)propKeys.nextElement();
                String value = tmpProps.getProperty(key);

                // if property is defined in the prop file
                if(value!=null) {
                    // if encrypted, replace with decrypted value
                    if (SecurityUtil.isMarkedEncrypted(value)) {
                        value  = SecurityUtil.decrypt(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM,propEncKey,value);
                        // if not encrypted, although it should have, mark as candidate for encryption in the file
                    }
                    value = value.trim();
                }

                props.put(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }
    
    /**
    * Return an ordered list of property files used to configure the agent.  Only
    * the default agent.properties is required, all other files will not be added
    * if they do not exist.
    *
    * @param propsFile The default agent.properties
    * @return The ordered list of Files to read for agent.properties.
    */
    public static File[] getPropertyFiles(String propsFile) {
    
        List<File> files = new ArrayList<File>();

        files.add(new File(propsFile)); // Default agent.properties
        File bundleFile = new File(BUNDLE_PROPFILE);
        if (bundleFile.exists()) {
            files.add(bundleFile); // Bundle agent.properties
        }

       
        final String home = System.getProperty("user.home");
        File homeAgentProperties = new File(home + File.separator + ".hq" + File.separator + DEFAULT_AGENT_PROPFILE_NAME);
        if (homeAgentProperties.exists()) {
            files.add(homeAgentProperties); // ~/.hq/agent.properties
        }
      
        File deployerProps = new File("deployer.properties");
        if (deployerProps.exists()) {
            files.add(deployerProps);
        }
        
        return files.toArray(new File[files.size()]);
    }
    
    /**
     * Return a Properties object that is the merged result all possible
     * locations for agent.properties.
     * 
     * @param propsFile The default agent.properties.
     * @return The merge Properties object.
     * @see org.hyperic.hq.agent.AgentConfig#getPropertyFiles(String)
     */
    public static Properties getProperties(String propsFile) throws AgentConfigException {
        Properties useProps = new Properties();
        useProps.putAll(AgentConfig.getDefaultProperties());
       
        File[] propFiles = getPropertyFiles(propsFile);
        for (int i=0; i<propFiles.length; i++) {
            if (!loadProps(useProps, propFiles[i])) {
                throw new AgentConfigException("Failed to load: " + propFiles[i]);
            }
        }
        
        PropertyUtil.expandVariables(useProps);
        return useProps;
    }
   
    public static AgentConfig newInstance(String propsFile)
        throws IOException, AgentConfigException {
        // verify that the agent bundle home has been properly defined
        // before populating the default properties
        checkAgentBundleHome();
        
        Properties useProps = getProperties(propsFile);
        
        return AgentConfig.newInstance(useProps);
    }

    /**
     * Create a new config object with settings specified by 
     * a properties object.  
     *
     * @param props Properties to use when setting up the config object
     * 
     * @return A AgentConfig object with settings as setup by
     *          the passed properties
     *
     * @throws AgentConfigException indicating the passed configuration was
     *                              invalid.
     */

    public static AgentConfig newInstance(Properties props)
        throws AgentConfigException 
    {
        AgentConfig res = new AgentConfig();
        
        res.useProperties(props);
        return res;
    }

    /**
     * Get a Properties object with default invocation properties for
     * the Agent.
     *
     * @return a Properties object with all the default parameters that
     *          the Agent will need to execute.
     */

    public static Properties getDefaultProperties(){
        Properties defaultProps = new Properties();

        // Setup default properties
        for (String[] element : AgentConfig.propertyList) {
                defaultProps.setProperty(element[0],
                    element[1]);
        }
        return defaultProps;
    }

    /**
     * Set the configuration based on a properties object.  
     *
     * @param appProps Properties to use to setup the object
     *
     * @throws AgentConfigException indicating the passed configuration was
     *                              invalid.
     */

    public void useProperties(Properties appProps) 
        throws AgentConfigException
    {
        String listenPort, storageProvider, storageProviderInfo;
        String listenIp;

        this.bootProps = appProps;

        listenPort = 
            appProps.getProperty(AgentConfig.PROP_LISTENPORT[0],
                                 AgentConfig.PROP_LISTENPORT[1]);
        try {
            this.setListenPort(Integer.parseInt(listenPort));
        } catch(NumberFormatException exc){
            throw new AgentConfigException(AgentConfig.PROP_LISTENPORT[0]
                                           + " is not an integer");
        }

        listenIp = appProps.getProperty(AgentConfig.PROP_LISTENIP[0],
                                        AgentConfig.PROP_LISTENIP[1]);
        this.setListenIp(listenIp);
        
        String proxyPort = 
            appProps.getProperty(AgentConfig.PROP_PROXYPORT[0], 
                                 AgentConfig.PROP_PROXYPORT[1]);
                
        try {
            int proxyPortInt = Integer.parseInt(proxyPort);
            
            if (proxyPortInt != AgentConfig.DEFAULT_PROXY_PORT) {
                this.setProxyPort(proxyPortInt);
            }
        } catch(NumberFormatException exc){
            throw new AgentConfigException(AgentConfig.PROP_PROXYPORT[0]
                                           + " is not an integer");
        }
        
        String proxyIp = 
            appProps.getProperty(AgentConfig.PROP_PROXYHOST[0], 
                                 AgentConfig.PROP_PROXYPORT[1]);
        
        this.setProxyIp(proxyIp);
        
        String[] enabledCiphers = appProps.getProperty(AgentConfig.PROP_ENABLED_CIPHERS[0], AgentConfig.PROP_ENABLED_CIPHERS[1]).split(",");
        
        this.setEnabledCiphers(enabledCiphers);
        
        storageProvider = 
            appProps.getProperty(AgentConfig.PROP_STORAGEPROVIDER[0],
                                 AgentConfig.PROP_STORAGEPROVIDER[1]);
        this.setStorageProvider(storageProvider);

        storageProviderInfo =
            appProps.getProperty(AgentConfig.PROP_STORAGEPROVIDERINFO[0],
                                AgentConfig.PROP_STORAGEPROVIDERINFO[1]);
        this.setStorageProviderInfo(storageProviderInfo);

        String dataDir = 
            appProps.getProperty(PROP_DATADIR[0],
                                 PROP_DATADIR[1]);

        File dir = new File(dataDir);
        
        boolean succeeded;
        
        try {
            succeeded = FileUtil.makeDirs(dir, 3);
        } catch (InterruptedException e) {
            throw new AgentConfigException("creating data directory was interrupted");
        }
        
        if (!succeeded) {
            String parent = new File(dir.getAbsolutePath())
                                    .getParentFile().getAbsolutePath();
            throw new AgentConfigException
                   ("Error creating data directory: " + dir.getAbsolutePath()
                    + "\nMake sure that the " + parent + " directory is "
                    + "owned by user '" + System.getProperty("user.name")
                    + "' and is not a read-only directory.");            
        }
        
        this.tokenFile = 
            appProps.getProperty("agent.tokenFile",
                                 dataDir + File.separator + "tokendata");

        //XXX the default log/agent.log still gets created even if this
        //changes, but if it is changed logs will be written to the new
        //location
        String logDir = 
            appProps.getProperty(PROP_LOGDIR[0],
                                 PROP_LOGDIR[1]);

        logdir = new File(logDir);
        
        try {
            succeeded = FileUtil.makeDirs(dir, 3);
        } catch (InterruptedException e) {
            throw new AgentConfigException("creating log directory was interrupted");
        }
        
        if (!succeeded) {
            String parent = new File(logdir.getAbsolutePath()).getParentFile().getAbsolutePath();
            throw new AgentConfigException
                   ("Error creating log directory: " + dir.getAbsolutePath()
                    + "\nMake sure that the " + parent + " directory is "
                    + "owned by user '" + System.getProperty("user.name")
                    + "' and is not a read-only directory.");            
        }
    }
    
    public File getLogDir() {
        return logdir;
    }
    
    public String getConfDirName() {
        return AGENT_CONF_DIR;
    }

    /**
     * Sets the port the Agent should listen on.
     *
     * @param port New port to set.  The port should be in the range of
     *             1 to 65535
     *
     * @throws AgentConfigException indicating the port was not within a valid
     *                              range
     */

    public void setListenPort(int port) 
        throws AgentConfigException
    {
        verifyValidPortRange(port);
        this.listenPort = port;
    }

    /**
     * Get the Agent listening port.
     *
     * @return The port the Agent will listen on.
     */

    public int getListenPort(){
        return this.listenPort;
    }

    /**
     * Sets the IP that the agent should listen on.  
     *
     * @param ip The IP to bind to.  If it is '*', then the agent will
     *           listen on all interfaces
     */

    public void setListenIp(String ip){
        this.listenIp = ip;
    }

    /**
     * Get the Agent listening address.
     *
     * @return The address the Agent will listen on.
     */

    public String getListenIp(){
        return this.listenIp;
    }
    
    /**
     * Sets the proxy port.
     * 
     * @param port New port to set.  The port should be in the range of
     *             1 to 65535
     *
     * @throws AgentConfigException indicating the port was not within a valid
     *                              range
     */
    public void setProxyPort(int port) throws AgentConfigException {
        verifyValidPortRange(port);        
        this.proxyPort = port;
    }
    
    /**
     * @return <code>true</code> if a proxy server is configured; 
     *         <code>false</code> otherwise.
     */
    public boolean isProxyServerSet() {
        return (this.getProxyPort() != AgentConfig.DEFAULT_PROXY_PORT) && 
               !AgentConfig.DEFAULT_PROXY_HOST.equals(this.getProxyIp());
    }
    
    /**
     * Get the proxy port.
     * 
     * @return The port or <code>-1</code> if no proxy server is set.
     */
    public int getProxyPort() {
        return this.proxyPort;
    }
    
    /**
     * Set the IP for the proxy server.
     * 
     * @param ip The IP for the proxy server.
     */
    public void setProxyIp(String ip) {
        this.proxyIp = ip;
    }
    
    /**
     * Get the IP for the proxy server.
     * 
     * @return The IP or the empty string if no proxy server is set.
     */
    public String getProxyIp() {
        return this.proxyIp;
    }

    /**
     * Get the listen IP address as an InetAddress object.
     *
     * @return null if the listen IP is for all interfaces, else
     *         an InetAddress referencing a specific IP.
     *
     * @throws UnknownHostException if the listenIP lookup fails.
     */
    public InetAddress getListenIpAsAddr()
        throws UnknownHostException
    {
        if(this.getListenIp().equals(IP_GLOBAL)){
            return null;
        } else {
            return InetAddress.getByName(this.getListenIp());
        }
    }
    
    /**
     * @return The notify up port or -1 if not set.
     */
    public int getNotifyUpPort() {
        return this.notifyUpPort;
    }
    
    /**
     * Sets the port which the AgentClient defines where the CommandServer 
     * can connect to notify it of successful startup.
     *
     * @param port New port to set.  The port should be in the range of
     *             1 to 65535
     *
     * @throws AgentConfigException indicating the port was not within a valid
     *                              range
     */
    public void setNotifyUpPort(int port) throws AgentConfigException {
        verifyValidPortRange(port);
        this.notifyUpPort = port;
    }

    /**
     * Set the classpath of the storage provider.  
     *
     * @param storageProvider Fully qualified classname for a class 
     *                        implementing the AgentStorageProvider interface
     */

    public void setStorageProvider(String storageProvider){
        this.storageProvider = storageProvider;
    }

    /**
     * Gets the storage provider the Agent will use.
     *
     * @return The fully qualified classname the AgentConfig object
     *          was previously configured with.
     */

    public String getStorageProvider(){
        return this.storageProvider;
    }

    /**
     * Sets the info string that the Agent will use to pass to the init()
     * function of the storage provider.  
     *
     * @param info Info string to pass to init()
     */

    public void setStorageProviderInfo(String info){
        this.storageProviderInfo = info;
    }

    /**
     * Get the info string passed to the init() function of the 
     * storage provider.
     *
     * @return The info string previously configured for the config object.
     */

    public String getStorageProviderInfo(){
        return this.storageProviderInfo;
    }

    /**
     * Get the boot properties used when creating the agent configuration.
     *
     * @return a Properties object containing the argument to useProperties,
     *          or, if newInstance was called with a properties object, those
     *          Properties.
     */

    public Properties getBootProperties(){
        return this.bootProps;
    }

    public String getTokenFile() {
        return this.tokenFile;
    }
    
    private void verifyValidPortRange(int port) throws AgentConfigException {
        if((port < 1) || (port > 65535)) {
            throw new AgentConfigException("Invalid port (not in range " +
                                           "1->65535)");
        }        
    }

    public String[] getEnabledCiphers() {
		return enabledCiphers;
	}
    
    public List<String> getEnabledCipherList() {
    	return new ArrayList<String>(Arrays.asList(getEnabledCiphers()));
    }

	public void setEnabledCiphers(String[] enabledCiphers) {
		this.enabledCiphers = enabledCiphers;
	}

}
