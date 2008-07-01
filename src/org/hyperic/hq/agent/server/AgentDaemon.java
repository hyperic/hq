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

package org.hyperic.hq.agent.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.agent.AgentMonitorValue;
import org.hyperic.hq.agent.AgentStartupCallback;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorInterface;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginExistsException;
import org.hyperic.hq.product.PluginManager;
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

    private static AgentDaemon mainInstance;
    private static Object      mainInstanceLock = new Object();

    private double               startTime;
    private final static Log                  logger = LogFactory.getLog(AgentDaemon.class);
    private ServerHandlerLoader  handlerLoader;
    private PluginLoader         handlerClassLoader;
    private CommandDispatcher    dispatcher;
    private AgentStorageProvider storageProvider;
    private CommandListener      listener;
    private AgentTransportLifecycle agentTransportLifecycle;
    private Vector               serverHandlers;
    private Vector               startedHandlers = new Vector();
    private AgentConfig          bootConfig;
    private Hashtable            notifyHandlers = new Hashtable();
    private Hashtable            monitorClients;
    private volatile boolean     running;         // Are we running?

    private ProductPluginManager ppm;
    
    public static AgentDaemon getMainInstance(){
        synchronized(AgentDaemon.mainInstanceLock){
            return AgentDaemon.mainInstance;
        }
    }

    private AgentDaemon(){
        // Fields which get re-used are initialized here.
        // See cleanup()/configure() for fields which can
        // be re-configured
        this.handlerClassLoader =
            PluginLoader.create("ServerHandlerLoader",
                                getClass().getClassLoader());
        this.handlerLoader = new ServerHandlerLoader(this.handlerClassLoader);        
        this.running       = false;
        this.startTime     = System.currentTimeMillis();

        synchronized(AgentDaemon.mainInstanceLock){
            if(AgentDaemon.mainInstance == null){
                AgentDaemon.mainInstance = this;
            }
        }
    }

    private static File[] getLibJars(String libHandlersDir) {
        File[] jars = new File(libHandlersDir).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String name = file.getName();
                    
                    return name.endsWith(".jar");
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
    public static AgentDaemon newInstance(AgentConfig cfg)
        throws AgentConfigException 
    {
        AgentDaemon res = new AgentDaemon();

        try {
            res.configure(cfg);
        } catch(AgentRunningException exc){
            throw new AgentAssertionException("New agent should not be " +
                                              "running");
        } 
        return res;
    }
    
    /**
     * Retreive a plugin manager.
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
     * Retreive the storage object in use by the Agent.  This routine should
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
    public AgentConfig getBootConfig()
    {
        return this.bootConfig;
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
     * Register an object to be called when a notifiation of the specified
     * message class occurs;
     *
     * @param handler  Handler to call to process the notification message
     * @param msgClass Message class to register with
     */
    public void registerNotifyHandler(AgentNotificationHandler handler,
                                      String msgClass)
    {
        synchronized(this.notifyHandlers){
            Vector handlers;

            if((handlers = (Vector)this.notifyHandlers.get(msgClass)) == null){
                handlers = new Vector();
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
            Vector handlers;

            if((handlers = (Vector)this.notifyHandlers.get(msgClass)) == null){
                return;
            }

            for(Iterator i=handlers.iterator(); i.hasNext(); ){
                AgentNotificationHandler handler;

                handler = (AgentNotificationHandler)i.next();
                handler.handleNotification(msgClass, message);
            }
        }
    }


    /**
     * Cleanup any internal resources the agent is using.  The Agent must
     * not be running when this function is called.  It may raise an 
     * exception if some of the cleanup fails, however the Agent will be
     * in a pristine state after calling this function.  Cleanup may be
     * called more than one time without re-configuring inbetween cleanup()
     * calls.
     *
     * @throws AgentRunningException indicating the Agent was running when
     *                               the routine was called.
     */
    private void cleanup()
        throws AgentRunningException 
    {
        if(this.isRunning()){
            throw new AgentRunningException("Agent cannot be cleaned up " +
                                            "while running");
        }

        // Shutdown the serverhandlers first, in case they need to write
        // something to storage
        if(this.startedHandlers != null){
            for(int i=0; i < this.startedHandlers.size(); i++){
                AgentServerHandler handler = 
                    (AgentServerHandler)this.startedHandlers.get(i);
                
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
        Class storageClass;

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
     * @param cfg Configuration to use to configure the Agent
     *
     * @throws AgentRunningException indicating the Agent was running when a
     *                               reconfiguration was attempted.
     * @throws AgentConfigException  indicating the configuration was invalid.
     */

    public void configure(AgentConfig cfg)
        throws AgentRunningException, AgentConfigException
    {
        DefaultConnectionListener defListener;
        AgentServerHandler loadedHandler;

        if(this.isRunning()){
            throw new AgentRunningException("Agent cannot be configured while"+
                                            " running");
        }

        //add lib/handlers/lib/*.jar classpath for handlers only
        String libHandlersLibDir = 
            cfg.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS_LIB[0]);
        File handlersLib = new File(libHandlersLibDir);
        if (handlersLib.exists()) {
            this.handlerClassLoader.addURL(handlersLib);
        }

        // Dispatcher
        this.dispatcher = new CommandDispatcher();

        this.storageProvider = AgentDaemon.createStorageProvider(cfg);

        // Determine the hostname.  This will be stored in the storage provider
        // and checked on each agent invocation.  This determines if this agent
        // installation has been copied from another machine without removing 
        // the data directory.
        String currentHost =
            GenericPlugin.getPlatformName();

        String storedHost = this.storageProvider.getValue(PROP_HOSTNAME);
        if (storedHost == null || storedHost.length() == 0) {
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
        defListener   = new DefaultConnectionListener(cfg);
        this.setConnectionListener(defListener);
        
        // set the lather proxy host and port if applicable
        if (cfg.isProxyServerSet()) {
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST, 
                    cfg.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT, 
                    String.valueOf(cfg.getProxyPort()));                
        }

        // Server Handlers
        this.serverHandlers = new Vector();

        // Always add in the AgentCommandsServer
        loadedHandler = new AgentCommandsServer();
        this.serverHandlers.add(loadedHandler);
        this.dispatcher.addServerHandler(loadedHandler);

        // Load server handlers on the fly from lib/*.jar.  Server handler
        // jars  must have a Main-Class that implements the AgentServerHandler
        // interface.
        String libHandlersDir = 
            cfg.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS[0]);
        File[] libJars = getLibJars(libHandlersDir);
        for (int i=0; i<libJars.length; i++) {
            try {
                JarFile jarFile = new JarFile(libJars[i]);
                Manifest manifest = jarFile.getManifest();
                String mainClass = manifest.getMainAttributes().
                    getValue("Main-Class");
                if (mainClass != null) {
                    String jarPath = libJars[i].getAbsolutePath();
                    loadedHandler = 
                        this.handlerLoader.loadServerHandler(jarPath);
                    this.serverHandlers.add(loadedHandler);
                    this.dispatcher.addServerHandler(loadedHandler);                        
                }
            } catch (Exception e) {
                throw new AgentConfigException("Failed to load " +
                                               "'" + libJars[i] + 
                                               "': " +
                                               e.getMessage());
            }
        }

        // Make sure the storage provider has a certificate DN.
        // If not, create one
        String certDN = this.storageProvider.getValue(PROP_CERTDN);
        if ( certDN == null || certDN.length() == 0 ) {
            certDN = generateCertDN();
            this.storageProvider.setValue(PROP_CERTDN, certDN);
            try {
                this.storageProvider.flush();
            } catch ( AgentStorageException ase ) {
                throw new AgentConfigException("Error storing certdn in "
                                               + "agent storage: " + ase);
            }
        }

        this.bootConfig = cfg;
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
        return (String[])this.monitorClients.keySet().toArray(new String[0]);
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

        iface = (AgentMonitorInterface)this.monitorClients.get(monitorName);
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
        return this.running;
    }

    /**
     * Tell the Agent to close all connections, and die.
     *
     * @throws AgentRunningException indicating the Agent was not running
     *                               when die() was called.
     */

    public void die() 
        throws AgentRunningException 
    {
        if(!this.running){
            throw new AgentRunningException("Agent is not running");
        }
        this.listener.die();
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

    private void startPluginManagers()
        throws AgentStartException
    {
        try {
            Properties bootProps = this.bootConfig.getBootProperties();
            String pluginDir;

            this.ppm = new ProductPluginManager(bootProps);
            this.ppm.init();

            pluginDir = 
                bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);

            this.ppm.registerPlugins(pluginDir);
            //check .. and higher for hq-plugins
            this.ppm.registerCustomPlugins("..");
            
            logger.info("Product Plugin Manager initalized");
        } catch(PluginExistsException exc){
            logger.error("Plugin initialize > 1 time", exc);
            throw new AgentStartException("Unable to initialize plugin " +
                                          "manager: " + exc.getMessage());
        } catch(PluginException e){
            logger.error("Error initializing plugins ", e);
            throw new AgentStartException("Unable to initialize plugin " +
                                          "manager: " + e.getMessage());
        }
    }

    private void startHandlers()
        throws AgentStartException
    {
        for(int i=0; i<this.serverHandlers.size(); i++){
            AgentServerHandler handler;
            
            handler = (AgentServerHandler) this.serverHandlers.get(i);
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
        for (int i=0; i<files.length; i++) {
            try {
                files[i].delete();
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
        
        this.running = true;
        boolean agentStarted = false;
        
        try {
            logger.info("Agent starting up, bundle name="+getCurrentAgentBundle());

            Properties bootProps = this.bootConfig.getBootProperties();
            String tmpDir = bootProps.getProperty(AgentConfig.PROP_TMPDIR[0]);
            if (tmpDir != null) {
                //this should always be the case.
                cleanTmpDir(tmpDir);
                System.setProperty("java.io.tmpdir", tmpDir);
            }

            this.monitorClients = new Hashtable();
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
                Class clazz = this.handlerClassLoader.loadClass(agentTransportLifecycleClass);
                Constructor constructor = clazz.getConstructor(
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
            this.listener.listenLoop();
            this.sendNotification(NOTIFY_AGENT_DOWN, "goin' down, baby!");
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
            
            this.running = false;
            
            try {this.cleanup();} catch(AgentRunningException exc){}
        
            if(this.storageProvider != null) {
                this.storageProvider.dispose();                
            }
            
            logger.info("Agent shut down");
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

    public static class RunnableAgent implements Runnable {
        
        private final boolean inNewProcess;
        
        public RunnableAgent() {
            this(true);
        }
        
        public RunnableAgent(boolean inNewProcess) {
            this.inNewProcess = inNewProcess;
        }
        
        public void run() {            
            String propFile =
                System.getProperty(AgentConfig.PROP_PROPFILE,
                                   AgentConfig.DEFAULT_PROPFILE);
                        

            boolean isConfigured = false;
            
            AgentDaemon agent = null;
            
            try {
                AgentConfig cfg = AgentConfig.newInstance(propFile);
                // Re-configue with the merged agent configuration.  This will
                // allow logging configuration to come from the user's
                // .hq/agent.properties.
                PropertyConfigurator.configure(cfg.getBootProperties());
                
                agent = AgentDaemon.newInstance(cfg);
                isConfigured = true;
            } catch (AgentConfigException e) {
                logger.error("Agent configuration error: ", e);    
            } catch (Exception e) {
                logger.error("Agent configuration failed: ", e);                                
            } finally {
                if (!isConfigured) {
                    cleanUpOnAgentConfigFailure();
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
        
        private void cleanUpOnAgentConfigFailure() {
            try {
                AgentStartupCallback agentStartupCallback = new AgentStartupCallback();
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
    
    public static void main(String[] args) {
        // Setup basic logging facility -- if we need to override it, we can.
        // only need this in process mode, since AgentClient already configured
        // logging system in thread mode
        BasicConfigurator.configure();
        
        new RunnableAgent().run();
    }
}
