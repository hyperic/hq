/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
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

package org.hyperic.hq.agent.server;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.agent.AgentLifecycle;
import org.hyperic.hq.agent.AgentMonitorValue;
import org.hyperic.hq.agent.AgentStartupCallback;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.diagnostics.AgentDiagnostics;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorInterface;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.agent.stats.AgentStatsWriter;
import org.hyperic.hq.bizapp.client.PlugininventoryCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginInfo;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.security.SecurityUtil;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * The main daemon which processes requests from clients.  The Agent has
 * the responsibility of being the 'live' entity on remote machines.  
 * Bootstrap configuration is done entirely within this class.
 */

public class AgentDaemon 
    extends AgentMonitorSimple
{
    public static final String NOTIFY_AGENT_UP =
        AgentDaemon.class.getName() + ".agentUp";
    public static final String NOTIFY_AGENT_DOWN =
        AgentDaemon.class.getName() + ".agentDown";
    public static final String NOTIFY_AGENT_FAILED_START = 
        AgentDaemon.class.getName()+ ".agentFailedStart";

    public static final String PROP_CERTDN = "agent.certDN";
    public static final String PROP_HOSTNAME = "agent.hostName";
    
    private static final String AGENT_COMMANDS_SERVER_JAR_NAME = "hq-agent-handler-commands";

    private static AgentDaemon mainInstance;
    private static Object      mainInstanceLock = new Object();

    private final double               startTime;
    private final static Log                  logger = LogFactory.getLog(AgentDaemon.class);
    private final ServerHandlerLoader  handlerLoader;
    private final PluginLoader         handlerClassLoader;
    private CommandDispatcher    dispatcher;
    private AgentStorageProvider storageProvider;
    private CommandListener      listener;
    private AgentTransportLifecycle agentTransportLifecycle;
    private Vector<AgentServerHandler>               serverHandlers;
    private Vector<AgentServerHandler>               startedHandlers = new Vector<AgentServerHandler>();
    private final Hashtable<String, Vector<AgentNotificationHandler>>            notifyHandlers = new Hashtable<String, Vector<AgentNotificationHandler>>();
    private Hashtable<String, AgentMonitorInterface>            monitorClients;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ProductPluginManager ppm;
    private final AgentConfig config;
    private AgentDiagnostics agentDiagnostics;
    
    public static AgentDaemon getMainInstance(){
        synchronized(AgentDaemon.mainInstanceLock){
            return AgentDaemon.mainInstance;
        }
    }

    private AgentDaemon(AgentConfig config){
        // Fields which get re-used are initialized here.
        // See cleanup()/configure() for fields which can
        // be re-configured
        this.handlerClassLoader = PluginLoader.create("ServerHandlerLoader", getClass().getClassLoader());
        this.handlerLoader = new ServerHandlerLoader(this.handlerClassLoader);        
        this.startTime     = System.currentTimeMillis();
        this.config        = config;

        synchronized(AgentDaemon.mainInstanceLock){
            if(AgentDaemon.mainInstance == null){
                AgentDaemon.mainInstance = this;
            }
        }
    }
    
    public CommandDispatcher getCommandDispatcher(){
        return dispatcher;
    }
    
    private static File getAgentCommandsServerJar(String libHandlersDir) 
        throws FileNotFoundException {
        
        File[] jars = new File(libHandlersDir).listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();
                
                return name.startsWith(AGENT_COMMANDS_SERVER_JAR_NAME);
            }
        });

        if ((jars == null) || (jars.length != 1)) {
            throw new FileNotFoundException(AGENT_COMMANDS_SERVER_JAR_NAME+" jar is not optional");
        }
        
        return jars[0];
    }

    private static File[] getOtherCommandsServerJars(String libHandlersDir) {
        File[] jars = new File(libHandlersDir).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String name = file.getName();
                    
                    return name.endsWith(".jar") && !name.startsWith(AGENT_COMMANDS_SERVER_JAR_NAME);
                }
            });

        if (jars == null) {  
            return new File[0];
        }
        return jars;
    }

    /**
     * Create a new AgentDaemon object based on a passed configuration.
     *
     * @param cfg Configuration the new Agent should use.
     *
     * @throws AgentConfigException indicating the passed configuration is 
     *                              invalid.
     */
    public static AgentDaemon newInstance(AgentConfig cfg) throws AgentConfigException {
        AgentDaemon res = new AgentDaemon(cfg);
        try {
            res.configure();
        } catch(AgentRunningException exc){
            throw new AgentAssertionException("New agent should not be running", exc);
        } 
        return res;
    }
    
    /**
     * Retrieve a plugin manager.
     *
     * @param type The type of plugin manager that is wanted
     * @return The plugin manager for the type given
     *
     * @throws AgentRunningException Indicating the agent was not running
     *                               when the request was made.
     *
     * @throws PluginException If the requested manager was not found.
     */
    public PluginManager getPluginManager(String type)
        throws AgentRunningException, PluginException
    {
        if (!this.isRunning()) {
            throw new AgentRunningException("Plugin manager cannot be " +
                                            "retrieved if the Agent is " +
                                            "not running");
        }
  
        return this.ppm.getPluginManager(type);
    }

    /**
     * Retrieve the storage object in use by the Agent.  This routine should
     * be used primarily by server handlers wishing to do any kind of 
     * storage.
     *
     * @return The storage provider object used by the Agent
     *
     * @throws AgentRunningException indicating the Agent was not running 
     *                               when the request was made.
     */
    public AgentStorageProvider getStorageProvider()
        throws AgentRunningException 
    {
        if(!this.isRunning()){
            throw new AgentRunningException("Storage cannot be retrieved if " +
                                            "the Agent is not running");
        }
        return this.storageProvider;
    }

    /**
     * Get the bootstrap configuration that the Agent was initialized with.
     *
     * @return The configuration object used to initialize the Agent.
     */
    public AgentConfig getBootConfig() {
        return this.config;
    }
    
    /**
     * @return The current agent bundle name.
     */
    public String getCurrentAgentBundle() {
        String agentBundleHome = getBootConfig()
                .getBootProperties().getProperty(AgentConfig.PROP_BUNDLEHOME[0]);
          
        File bundleDir = new File(agentBundleHome);
        return bundleDir.getName();
    }
    
    /**
     * Retrieve the agent transport lifecycle.
     * 
     * @throws AgentRunningException indicating the Agent was not running 
     *                               when the request was made.
     */
    public AgentTransportLifecycle getAgentTransportLifecycle() 
        throws AgentRunningException 
    {
        
        if(!this.isRunning()){
            throw new AgentRunningException("Agent Transport Lifecycle cannot be retrieved if " +
                                            "the Agent is not running");
        }
        
        return this.agentTransportLifecycle;
    }

    /**
     * Register an object to be called when a notification of the specified
     * message class occurs;
     *
     * @param handler  Handler to call to process the notification message
     * @param msgClass Message class to register with
     */
    public void registerNotifyHandler(AgentNotificationHandler handler,
                                      String msgClass)
    {
        synchronized(this.notifyHandlers){
            Vector<AgentNotificationHandler> handlers;

            if((handlers = this.notifyHandlers.get(msgClass)) == null){
                handlers = new Vector<AgentNotificationHandler>();
                this.notifyHandlers.put(msgClass, handlers);
            }

            handlers.add(handler);
        }
    }

    /**
     * Send a notification event to all notification handlers which 
     * have registered with the specified message class.
     *
     * @param msgClass Message class that the message belongs to
     * @param message  Message to send
     */
    public void sendNotification(String msgClass, String message){
        synchronized(this.notifyHandlers){
            Vector<AgentNotificationHandler> handlers;

            if((handlers = this.notifyHandlers.get(msgClass)) == null){
                return;
            }

            for (AgentNotificationHandler handler : handlers) {
            	handler.handleNotification(msgClass, message);
            }
        }
    }


    /**
     * Cleanup any internal resources the agent is using.  The Agent must
     * not be running when this function is called.  It may raise an 
     * exception if some of the cleanup fails, however the Agent will be
     * in a pristine state after calling this function.  Cleanup may be
     * called more than one time without re-configuring in between cleanup()
     * calls.
     *
     * @throws AgentRunningException indicating the Agent was running when
     *                               the routine was called.
     */
    private synchronized void cleanup() throws AgentRunningException {
        if(this.isRunning()){
            throw new AgentRunningException("Agent cannot be cleaned up while running");
        }

        // Shutdown the serverhandlers first, in case they need to write
        // something to storage
        if (this.startedHandlers != null){
            for(int i=0; i < this.startedHandlers.size(); i++){
                AgentServerHandler handler = 
                    this.startedHandlers.get(i);
                
                handler.shutdown();
            }
            this.serverHandlers = null;
            this.startedHandlers = null;
        }
        this.dispatcher = null;
        if(this.listener != null){
            this.listener.cleanup();
            this.listener = null;
        }

        if(this.storageProvider != null){
            try {
                this.storageProvider.flush();
            } catch(AgentStorageException exc){
                logger.error("Failed to flush Agent storage", exc);
            } finally {
                this.storageProvider.dispose();
                this.storageProvider = null;
            }
        }

        try {
            this.ppm.shutdown();
        } catch (PluginException e) {
            // Not much we can do
        }
    }

    public static AgentStorageProvider
        createStorageProvider(AgentConfig cfg)
        throws AgentConfigException
    {
        AgentStorageProvider provider;
        Class<?> storageClass;

        try {
            storageClass = Class.forName(cfg.getStorageProvider());
        } catch(ClassNotFoundException exc){
            throw new AgentConfigException("Storage provider not found: " +
                                           exc.getMessage());
        }

        try {
            provider = (AgentStorageProvider)storageClass.newInstance();
            provider.init(cfg.getStorageProviderInfo());
        } catch(IllegalAccessException exc){
            throw new AgentConfigException("Unable to access storage " +
                                           "provider '" + 
                                           cfg.getStorageProvider() + 
                                           "': " + exc.getMessage());
        } catch(InstantiationException exc){
            throw new AgentConfigException("Unable to instantiate storage " +
                                           "provider '" + 
                                           cfg.getStorageProvider() + 
                                           "': " + exc.getMessage());
        } catch(AgentStorageException exc){
            throw new AgentConfigException("Storage provider unable to " +
                                           "initialize: " + exc.getMessage());
        }
        return provider;
    }

    /**
     * Configure the agent to run with new parameters.  This routine will
     * load jars, open sockets, and other resources in preparation for
     * running.
     *
     * @throws AgentRunningException indicating the Agent was running when a
     *                               reconfiguration was attempted.
     * @throws AgentConfigException  indicating the configuration was invalid.
     */
    public void configure()
    throws AgentRunningException, AgentConfigException {
        DefaultConnectionListener defListener;
        if(this.isRunning()){
            throw new AgentRunningException("Agent cannot be configured while"+
                                            " running");
        }

        //add lib/handlers/lib/*.jar classpath for handlers only
        String libHandlersLibDir = 
            config.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS_LIB[0]);
        File handlersLib = new File(libHandlersLibDir);
        if (handlersLib.exists()) {
            this.handlerClassLoader.addURL(handlersLib);
        }

        // Dispatcher
        this.dispatcher = new CommandDispatcher();

        this.storageProvider = AgentDaemon.createStorageProvider(config);

        // Determine the hostname.  This will be stored in the storage provider
        // and checked on each agent invocation.  This determines if this agent
        // installation has been copied from another machine without removing 
        // the data directory.
        // [HHQ-5547] Overriding property is fetched here directly because it's not yet initialized in GenericPlugin
        String platformName = config.getBootProperties().getProperty(ProductPlugin.PROP_PLATFORM_NAME);
        String currentHost = (null != platformName ? platformName :
            GenericPlugin.getPlatformName());

        String storedHost = this.storageProvider.getValue(PROP_HOSTNAME);
        if ((storedHost == null) || (storedHost.length() == 0)) {
            this.storageProvider.setValue(PROP_HOSTNAME, currentHost);
        } else {
            // Validate
            if (!storedHost.equals(currentHost)) {
                String err =
                    "Invalid hostname '" + currentHost + "'. This agent " +
                    "has been configured for '" + storedHost + "'. If " +
                    "this agent has been copied from a different machine " +
                    "please remove the data directory and restart the agent";

                throw new AgentConfigException(err);
            }
        }

        this.listener = new CommandListener(this.dispatcher);
        defListener   = new DefaultConnectionListener(config);
        this.setConnectionListener(defListener);
        
        // set the lather proxy host and port if applicable
        if (config.isProxyServerSet()) {
            logger.info("Setting proxy server: host="+config.getProxyIp()+
                             "; port="+config.getProxyPort()); 
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST, 
                    config.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT, 
                    String.valueOf(config.getProxyPort()));                
        }

        // Server Handlers
        this.serverHandlers = new Vector<AgentServerHandler>();

        // Load server handlers on the fly from lib/*.jar.  Server handler
        // jars  must have a Main-Class that implements the AgentServerHandler
        // interface.
        String libHandlersDir = 
            config.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS[0]);
        
        // The AgentCommandsServer is *NOT* optional. Make sure it is loaded
        File agentCommandsServerJar;
        
        try {
            agentCommandsServerJar = getAgentCommandsServerJar(libHandlersDir);
        } catch (FileNotFoundException e) {
            throw new AgentConfigException(e.getMessage());
        }
        
        loadAgentServerHandlerJars(new File[]{agentCommandsServerJar});
                
        File[] otherCommandsServerJars = getOtherCommandsServerJars(libHandlersDir);
                
        loadAgentServerHandlerJars(otherCommandsServerJars);

        // Make sure the storage provider has a certificate DN.
        // If not, create one
        String certDN = this.storageProvider.getValue(PROP_CERTDN);
        if ( (certDN == null) || (certDN.length() == 0) ) {
            certDN = generateCertDN();
            this.storageProvider.setValue(PROP_CERTDN, certDN);
            try {
                this.storageProvider.flush();
            } catch ( AgentStorageException ase ) {
                throw new AgentConfigException("Error storing certdn in "
                                               + "agent storage: " + ase);
            }
        }
    }

    private void loadAgentServerHandlerJars(File[] libJars)
            throws AgentConfigException {
        AgentServerHandler loadedHandler;
        
        // Save the current context loader, and reset after we load plugin jars
        ClassLoader currentContext = Thread.currentThread().getContextClassLoader();
        
        for (File libJar : libJars) {
            try {
                JarFile jarFile = new JarFile(libJar);
                Manifest manifest = jarFile.getManifest();
                String mainClass = manifest.getMainAttributes().
                    getValue("Main-Class");
                if (mainClass != null) {
                    String jarPath = libJar.getAbsolutePath();
                    loadedHandler = 
                        this.handlerLoader.loadServerHandler(jarPath);
                    this.serverHandlers.add(loadedHandler);
                    this.dispatcher.addServerHandler(loadedHandler);                        
                }
                jarFile.close();
            } catch (Exception e) {
                throw new AgentConfigException("Failed to load " +
                                               "'" + libJar + 
                                               "': " +
                                               e.getMessage());
            }
        }
        
        // Restore the class loader
        Thread.currentThread().setContextClassLoader(currentContext);
    }

    /**
     * Generates a new certificate DN.
     * @return The new DN that was generated.
     */
    private String generateCertDN() {
        return "CAM-AGENT-" + SecurityUtil.generateRandomToken();
    }

    /**
     * MONITOR METHOD:  Get the monitors which are registered with the agent
     */
    public String[] getMonitors() 
        throws AgentMonitorException 
    {
        return this.monitorClients.keySet().toArray(new String[0]);
    }

    /**
     * MONITOR METHOD:  Get the time the agent started
     */
    public double getStartTime()
        throws AgentMonitorException
    {
        return this.startTime;
    }

    /**
     * MONITOR METHOD:  Get the time the agent has been running
     */
    public double getUpTime()
        throws AgentMonitorException
    {
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * MONITOR METHOD:  Get the JVMs total memory
     */
    public double getJVMTotalMemory()
        throws AgentMonitorException
    {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * MONITOR METHOD:  Get the JVMs free memory
     */
    public double getJVMFreeMemory()
        throws AgentMonitorException
    {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * MONITOR METHOD:  Get the # of active threads
     */
    public double getNumActiveThreads()
        throws AgentMonitorException
    {
        return Thread.activeCount();
    }

    public void registerMonitor(String monitorName, 
                                AgentMonitorInterface monitor)
    {
        this.monitorClients.put(monitorName, monitor);
    }

    public AgentMonitorValue[] getMonitorValues(String monitorName, 
                                                String[] monitorKeys)
    {
        AgentMonitorInterface iface;

        iface = this.monitorClients.get(monitorName);
        if(iface == null){
            AgentMonitorValue[] res;
            AgentMonitorValue badVal;

            badVal = new AgentMonitorValue();
            badVal.setErrCode(AgentMonitorValue.ERR_BADMONITOR);

            res = new AgentMonitorValue[monitorKeys.length];
            for(int i=0; i<res.length; i++){
                res[i] = badVal;
            }
            return res;
        }

        return iface.getMonitorValues(monitorKeys);
    }


    /**
     * Determine if the Agent is currently running.
     *
     * @return true if the Agent is running (listening on its port)
     */

    public boolean isRunning(){
        return running.get();
    }

    /**
     * Tell the Agent to close all connections, and die.
     *
     * @throws AgentRunningException indicating the Agent was not running
     *                               when die() was called.
     */

    public void die() throws AgentRunningException {
        if(!running.get()){
            throw new AgentRunningException("Agent is not running");
        }
        listener.die();
        running.set(false);
        storageProvider.dispose();
        agentDiagnostics.die();
    }

    /**
     * A stub API which allows handlers to set the connection listener
     * that the agent uses.
     */
    public void setConnectionListener(AgentConnectionListener newListener)
        throws AgentRunningException
    {
        this.listener.setConnectionListener(newListener);
    }

    private void startPluginManagers() throws AgentStartException {
        try {
            Properties bootProps = this.config.getBootProperties();
            String pluginDir;

            this.ppm = new ProductPluginManager(bootProps);

            this.ppm.init();

            pluginDir = 
                bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);

            Collection<PluginInfo> excludes = new TreeSet<PluginInfo>(new Comparator<PluginInfo>() {
                public int compare(PluginInfo p1, PluginInfo p2) {
                    return p1.name.compareTo(p2.name);
                }
            });
            Set<PluginInfo> plugins = new HashSet<PluginInfo>();
            plugins.addAll(this.ppm.registerPlugins(pluginDir, excludes));
            plugins.addAll(excludes);
            scanLegacyCustomDir("..");
            Collection<PluginInfo> fromDirs = ppm.getAllPluginInfoDirectFromFileSystem(pluginDir);
            plugins = mergeByName(fromDirs, plugins);
            sendPluginStatusToServer(plugins);
            
            logger.info("Product Plugin Manager initalized");
        } catch(Exception e){
        	// ...an unexpected exception has occurred that was not handled
        	// log it and bail...
            logger.error("Error initializing plugins ", e);
            throw new AgentStartException("Unable to initialize plugin " +
                                          "manager: " + e.getMessage());
        }
    }

    private void scanLegacyCustomDir(String startDir) {
        File dir = new File(startDir).getAbsoluteFile();
        String fs = File.separator;
        while (dir != null) {
            File customDir = null;
            try {
                customDir = new File(dir, "hq-plugins");
            } catch (NullPointerException e) {
                logger.warn("cannot scan custom plugin dir " + dir + fs + "hq-plugins, please note the plugins in this " +
                		    "directory will not be supported anymore instead they must to be managed via the " +
                            "HQ Server Plugin Manager UI.");
                continue;
            }
            if (customDir.exists() && customDir.isDirectory()) {
                File[] files = customDir.listFiles();
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith("-plugin.jar") || name.endsWith("-plugin.xml")) {
                        logger.warn("WARNING - custom plugins on the agent are no longer supported.  " +
                                    "Will not load plugin - " + name + ", instead add this plugin " +
                                    "to the HQ Server via the Plugin Manager UI.");
                    }
                }
                return;
            }
            dir = dir.getParentFile();
        }
    }

    /** merge fromDirs into the plugins Collection keyed by pluginInfo.name and return plugins */
    private Set<PluginInfo> mergeByName(Collection<PluginInfo> fromDirs,
                                        Set<PluginInfo> plugins) {
        final Collection<String> pluginFiles = new HashSet<String>();
        for (final PluginInfo info : plugins) {
            pluginFiles.add(info.jar);
        }
        for (final PluginInfo info : fromDirs) {
            if (!pluginFiles.contains(info.jar)) {
                plugins.add(info);
            }
        }
        return plugins;
    }

    private void sendPluginStatusToServer(final Collection<PluginInfo> plugins) {
        // server may be down or Provider may not be setup.  Either way we want to retry until
        // the data is sent
        Thread thread = new Thread("PluginStatusSender") {
            @Override
            public void run() {
                while (true) {
                    try {
                        AgentStorageProvider provider = getStorageProvider();
                        if (provider == null) {
                            logger.debug("trying to send plugin status to the server but " +
                                         "provider has not been setup, will sleep 5 seconds and retry");
                            Thread.sleep(5000);
                            continue;
                        }
                        PlugininventoryCallbackClient client = new PlugininventoryCallbackClient(
                            new StorageProviderFetcher(provider), plugins);
                        logger.info("Sending plugin status to server");
                        client.sendPluginReportToServer();
                        logger.info("Successfully sent plugin status to server");
                        break;
                    } catch (Exception e) {
                        logger.warn("could not send plugin status to server, will retry:  " + e);
                        logger.debug(e,e);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.debug(e,e);
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private void startHandlers()
        throws AgentStartException
    {
        for(int i=0; i<this.serverHandlers.size(); i++){
            AgentServerHandler handler;
            
            handler = this.serverHandlers.get(i);
            try {
                handler.startup(this);
            } catch(AgentStartException exc){
                logger.error("Error starting plugin " + handler, exc);
                throw exc;
            } catch(Exception exc){
                logger.error("Unknown exception", exc);
                throw new AgentStartException("Error starting plugin " +
                                              handler, exc);
            }
            this.startedHandlers.add(handler);
        }
    }
    
    private final void postInitActions() throws AgentStartException { 
        for(AgentServerHandler handler : this.startedHandlers) { 
            handler.postInitActions() ; 
        }//EO while there are more agents 
    }//EOM 

    //these files should have already been deleted on normal
    //jvm shutdown.  however, agent.exe start + CTRL-c on windows
    //leaves them behind.  cleanout at startup.
    private void cleanTmpDir(String tmp) {
        File dir = new File(tmp);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                file.delete();
            } catch (SecurityException e) { }
        }
    }

    private PrintStream newLogStream(String stream, Properties props) {
        Logger logger = Logger.getLogger(stream);
        String logLevel = props.getProperty("agent.logLevel." + stream);
        if (logLevel == null) {
            return null;
        }
        Level level = Level.toLevel(logLevel);
        return new PrintStream(new LoggingOutputStream(logger, level), true);
    }

    private void redirectStreams(Properties props) {
        //redirect System.{out,err} to log4j appender
        PrintStream stream;
        if ((stream = newLogStream("SystemOut", props)) != null) { 
            System.setOut(stream);
        }
        if ((stream = newLogStream("SystemErr", props)) != null) {
            System.setErr(stream);    
        }
    }
    
    /**
     * Start the Agent's listening process.  This routine blocks for the
     * entire execution of the Agent.  
     *
     * @throws AgentStartException indicating the Agent was unable to start,
     *                             or one of the plugins failed to start.
     */

    public void start() 
        throws AgentStartException 
    {
        running.set(true);
        boolean agentStarted = false;
        
        try {
            logger.info("Agent starting up, bundle name="+getCurrentAgentBundle());

            Properties bootProps = this.config.getBootProperties();
            String tmpDir = bootProps.getProperty(AgentConfig.PROP_TMPDIR[0]);
            if (tmpDir != null) {
                try {
                    // update plugins residing in tmp directory prior to cleaning it up
                    List<String> updatedPlugins = AgentUpgradeManager.updatePlugins(bootProps);
                    if (!updatedPlugins.isEmpty()) {
                        logger.info("Successfully updated plugins: " + updatedPlugins);
                    }
                }
                catch (IOException e) {
                    logger.error("Failed to update plugins", e);
                }
                //this should always be the case.
                cleanTmpDir(tmpDir);
                System.setProperty("java.io.tmpdir", tmpDir);
            }

            this.monitorClients = new Hashtable<String, AgentMonitorInterface>();
            this.registerMonitor("agent", this);
            this.registerMonitor("agent.commandListener", this.listener);
            redirectStreams(bootProps);
            
            // Load the agent transport in the server handler classloader.
            // This is necessary because we don't want the jboss remoting 
            // classes in the root agent classloader - this causes conflicts 
            // with the jboss plugins.
            String agentTransportLifecycleClass = 
                "org.hyperic.hq.agent.server.AgentTransportLifecycleImpl";

            try {
            	Class<?> clazz = this.handlerClassLoader.loadClass(agentTransportLifecycleClass);
                Constructor<?> constructor = clazz.getConstructor(
                                    new Class[]{AgentDaemon.class, 
                                                AgentConfig.class, 
                                                AgentStorageProvider.class
                                                });

                this.agentTransportLifecycle = 
                    (AgentTransportLifecycle)constructor.newInstance(
                                    new Object[]{this, 
                                                 getBootConfig(), 
                                                 getStorageProvider()
                                                 });
            } catch (ClassNotFoundException e) {
                throw new AgentStartException(
                        "Cannot find agent transport lifecycle class: "+
                        agentTransportLifecycleClass);
            }
            
            this.startPluginManagers();
            this.startHandlers();

            
            // The started handlers should have already registered with the 
            // agent transport lifecycle
            this.agentTransportLifecycle.startAgentTransport();                
            
            this.listener.setup();

            tryForceAgentFailure();
                        
            logger.info("Agent started successfully");

            this.sendNotification(NOTIFY_AGENT_UP, "we're up, baby!");
            agentStarted = true;
            AgentStatsWriter statsWriter = new AgentStatsWriter(config);
            statsWriter.startWriter();
	    this.postInitActions() ;
            agentDiagnostics = AgentDiagnostics.getInstance();
            agentDiagnostics.setConfig(config);
            agentDiagnostics.start();
            this.listener.listenLoop();
            this.sendNotification(NOTIFY_AGENT_DOWN, "goin' down, baby!");
            statsWriter.stopWriter();
        } catch(AgentStartException exc){
            logger.error(exc.getMessage(), exc);
            throw exc;
        } catch(Exception exc){
            logger.error("Error running agent", exc);
            throw new AgentStartException("Error running agent: " +
                                          exc.getMessage());
        } catch(Throwable exc){
            logger.error("Critical error running agent", exc);
            // We don't flush the storage here, since we may be out of
            // memory, etc -- stuff just isn't in a good state at all.
            if(this.storageProvider != null){
                this.storageProvider.dispose();
                this.storageProvider = null;
            }

            throw new AgentStartException("Critical shutdown");
        } finally {
            if (!agentStarted) {
                logger.debug("Notifying that agent startup failed");
                this.sendNotification(NOTIFY_AGENT_FAILED_START, "agent startup failed!");
            }
            
            if (this.agentTransportLifecycle != null) {
                this.agentTransportLifecycle.stopAgentTransport();
            }
            
            running.set(false);
            
            try {
                cleanup();
            } catch(AgentRunningException e) {
                logger.error(e,e);
            }
        
            if(this.storageProvider != null) {
                this.storageProvider.dispose();                
            }
            
            logger.info("Agent shut down");
            System.exit(0);
        }
    }
    
    private void tryForceAgentFailure() throws AgentStartException {
        String rollbackBundle = getBootConfig().getBootProperties()
                .getProperty(AgentConfig.PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE[0]);
        
        if (getCurrentAgentBundle().equals(rollbackBundle)) {
            throw new AgentStartException(AgentConfig.PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE[0]+ 
                    " property set to force rollback of agent bundle upgrade: "+
                    rollbackBundle);
        }
    }

    public static class RunnableAgent implements Runnable, AgentLifecycle {
        
        private final AgentConfig config;
        private AgentDaemon agent;
        
        public RunnableAgent(AgentConfig config) {
            this.config = config;
        }
        
        public void shutdown() {
            try {
                if (agent != null) {
                    agent.die();
                    agent.cleanup();
                }
            } catch (Throwable e) {
                logger.error(e,e);
            }
        }
                
        public void run() {            
            boolean isConfigured = false;            
            
            try {
                agent = AgentDaemon.newInstance(config);
                isConfigured = true;
            } catch (Throwable e) {
                logger.error("Agent configuration failed: ", e);                                
            } finally {
                if (!isConfigured) {
                    cleanUpOnAgentConfigFailure(config);
                }
            }

            boolean isStarted = false;
            
            if (agent != null) {
                try {
                    agent.start();
                    isStarted = true;
                } catch(AgentStartException e) {
                    logger.error("Agent startup error: ", e);                                
                } catch (Exception e) {
                    logger.error("Agent startup failed: ", e);                
                } finally {
                    if (!isStarted) {
                        cleanUpOnAgentStartFailure();
                    }
                }                
            }
        }
        
        private void cleanUpOnAgentConfigFailure(AgentConfig config) {
            try {
                AgentStartupCallback agentStartupCallback = new AgentStartupCallback(config);
                agentStartupCallback.onAgentStartup(false);
            } catch (Exception e) {
                logger.error("Failed to callback on startup failure.", e);
            }
            
            cleanUpOnAgentStartFailure();
        }
        
        private void cleanUpOnAgentStartFailure() {
            if (!WrapperManager.isControlledByNativeWrapper()) {
                System.exit(-1);
            } else {
                rollbackAndRestartJVM();
            } 
        }
        
        // rollback agent bundle and issue JVM restart if in Java Service Wrapper mode
        private void rollbackAndRestartJVM() {
            logger.info("Attempting to rollback agent bundle");
            boolean success = false;
            try {
                success = AgentUpgradeManager.rollback();
            }
            catch (IOException e) {
                logger.error("Unable to rollback agent bundle", e);
            }
            if (success) {
                logger.info("Rollback of agent bundle was successful");
            } else {
                logger.error("Rollback of agent bundle was not successful");
            }
            
            logger.info("Restarting JVM...");
            AgentUpgradeManager.restartJVM();
        }
        
    }

}
