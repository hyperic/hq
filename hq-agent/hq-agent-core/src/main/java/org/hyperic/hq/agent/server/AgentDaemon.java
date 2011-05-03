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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hyperic.hq.agent.*;
import org.hyperic.hq.agent.server.monitor.AgentMonitorInterface;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.bizapp.client.PlugininventoryCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.product.*;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.security.SecurityUtil;
import org.tanukisoftware.wrapper.WrapperManager;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The main daemon which processes requests from clients.  The Agent has
 * the responsibility of being the 'live' entity on remote machines.
 * Bootstrap configuration is done entirely within this class.
 */

public class AgentDaemon extends AgentMonitorSimple {

    public static final String[] BASE_PATHS = {"org.hyperic.hq.bizapp.client", "org.hyperic.hq.operation"};

    public static final String NOTIFY_AGENT_UP = AgentDaemon.class.getName() + ".agentUp";

    public static final String NOTIFY_AGENT_DOWN = AgentDaemon.class.getName() + ".agentDown";

    public static final String NOTIFY_AGENT_FAILED_START = AgentDaemon.class.getName() + ".agentFailedStart";

    public static final String PROP_CERTDN = "agent.certDN";

    public static final String PROP_HOSTNAME = "agent.hostName";

    private static final String AGENT_COMMANDS_SERVER_JAR_NAME = "hq-agent-handler-commands";

    private static AgentDaemon mainInstance;

    private static final Object monitor = new Object();

    private double startTime;

    private final static Log logger = LogFactory.getLog(AgentDaemon.class);

    private ServerHandlerLoader handlerLoader;

    private PluginLoader handlerClassLoader;

    private RemotingCommandDispatcher dispatcher;

    private AgentStorageProvider storageProvider;

    private CommandListener listener;

    private AgentTransportLifecycle agentTransportLifecycle;

    private List<AgentServerHandler> serverHandlers;

    private List<AgentServerHandler> startedHandlers = new ArrayList<AgentServerHandler>();

    private AgentConfig bootConfig;

    private final Map<String, List<AgentNotificationHandler>> notifyHandlers = new ConcurrentHashMap<String, List<AgentNotificationHandler>>();

    private Map<CharSequence, AgentMonitorInterface> monitorClients;

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private static final AtomicBoolean started = new AtomicBoolean(false);

    private ProductPluginManager ppm;


    public static AgentDaemon getMainInstance() {
        synchronized (AgentDaemon.monitor) {
            return AgentDaemon.mainInstance;
        }
    }

    public AgentDaemon() {
        // Fields which get re-used are initialized here.
        // See cleanup()/configure() for fields which can
        // be re-configured
        this.handlerClassLoader = PluginLoader.create("ServerHandlerLoader", getClass().getClassLoader());
        this.handlerLoader = new ServerHandlerLoader(this.handlerClassLoader);
        this.startTime = System.currentTimeMillis();

        synchronized (monitor) {
            if (AgentDaemon.mainInstance == null) {
                AgentDaemon.mainInstance = this;
            }
        }
    }

    public static boolean isRunning() {
        return running.get();
    }

    public static boolean isStarted() {
        return started.get();
    }

    public RemotingCommandDispatcher getCommandDispatcher() {
        return dispatcher;
    }

    private static File getAgentCommandsServerJar(String libHandlersDir) throws FileNotFoundException {
        File[] jars = new File(libHandlersDir).listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();

                return name.startsWith(AGENT_COMMANDS_SERVER_JAR_NAME);
            }
        });

        if (jars == null || jars.length != 1) {
            throw new FileNotFoundException(AGENT_COMMANDS_SERVER_JAR_NAME + " jar is not optional");
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

        if (jars == null) return new File[0];
        return jars;
    }

    /**
     * Create a new AgentDaemon object based on a passed configuration.
     * @param cfg Configuration the new Agent should use.
     * @return new AgentDaemon instance
     * @throws AgentConfigException indicating the passed configuration is invalid.
     */
    public static AgentDaemon newInstance(AgentConfig cfg) throws AgentConfigException {
        AgentDaemon res = new AgentDaemon();

        try {
            res.configure(cfg);
            running.set(true);
        } catch (AgentRunningException exc) {
            throw new AgentAssertionException("New agent should not be running");
        }
        return res;
    }

    /**
     * Retreive a plugin manager.
     * @param type The type of plugin manager that is wanted
     * @return The plugin manager for the type given
     * @throws AgentRunningException Indicating the agent was not running
     *                               when the request was made.
     * @throws PluginException       If the requested manager was not found.
     */
    public PluginManager getPluginManager(String type) throws AgentRunningException, PluginException {
        if (!running.get())
            throw new AgentRunningException("Plugin manager cannot be retrieved if the Agent is not running");

        return this.ppm.getPluginManager(type);
    }

    /**
     * Retrieve the storage object in use by the Agent.  This routine should
     * be used primarily by server handlers wishing to do any kind of storage.
     * @return The storage provider object used by the Agent
     * @throws AgentRunningException indicating the Agent was not running when the request was made.
     */
    public AgentStorageProvider getStorageProvider() throws AgentRunningException {
        if (!running.get()) throw new AgentRunningException("Storage cannot be retrieved if the Agent is not running");

        return this.storageProvider;
    }

    /**
     * Get the bootstrap configuration that the Agent was initialized with.
     * @return The configuration object used to initialize the Agent.
     */
    public AgentConfig getBootConfig() {
        return this.bootConfig;
    }

    /**
     * @return The current agent bundle name.
     */
    public String getCurrentAgentBundle() {
        String agentBundleHome = getBootConfig().getBootProperties().getProperty(AgentConfig.PROP_BUNDLEHOME[0]);
        File bundleDir = new File(agentBundleHome);
        return bundleDir.getName();
    }

    /**
     * Retrieve the agent transport lifecycle.
     * @return AgentTransportLifecycle
     * @throws AgentRunningException indicating the Agent was not running when the request was made.
     */
    public AgentTransportLifecycle getAgentTransportLifecycle() throws AgentRunningException {
        if (!running.get())
            throw new AgentRunningException("Agent Transport Lifecycle cannot be retrieved if the Agent is not running");

        return this.agentTransportLifecycle;
    }

    /**
     * Register an object to be called when a notifiation of the specified
     * message class occurs;
     * @param handler  Handler to call to process the notification message
     * @param msgClass Message class to register with
     */
    public void registerNotifyHandler(AgentNotificationHandler handler, String msgClass) {
        List<AgentNotificationHandler> handlers = notifyHandlers.get(msgClass);

        if (handlers == null) {
            handlers = new ArrayList<AgentNotificationHandler>();
            notifyHandlers.put(msgClass, handlers);
        }
        handlers.add(handler);
    }

    /**
     * Send a notification event to all notification handlers which
     * have registered with the specified message class.
     * @param msgClass Message class that the message belongs to
     * @param message  Message to send
     */
    public void sendNotification(String msgClass, String message) {
        if (notifyHandlers.get(msgClass) == null) return;

        List<AgentNotificationHandler> handlers = notifyHandlers.get(msgClass);
        for (AgentNotificationHandler objHandler : handlers) {
            objHandler.handleNotification(msgClass, message);
        }
    }

    /**
     * Cleanup any internal resources the agent is using.  The Agent must
     * not be running when this function is called.  It may raise an
     * exception if some of the cleanup fails, however the Agent will be
     * in a pristine state after calling this function.  Cleanup may be
     * called more than one time without re-configuring inbetween cleanup()
     * calls.
     * @throws AgentRunningException indicating the Agent was running when
     *                               the routine was called.
     */
    private void cleanup() throws AgentRunningException {
        if (running.get()) throw new AgentRunningException("Agent cannot be cleaned up while running");

        // Shutdown the serverhandlers first, in case they need to write
        // something to storage
        if (this.startedHandlers != null) {
            for (AgentServerHandler handler : this.startedHandlers) {
                handler.shutdown();
            }
            this.serverHandlers = null;
            this.startedHandlers = null;
        }
        this.dispatcher = null;
        if (this.listener != null) {
            this.listener.cleanup();
            this.listener = null;
        }

        if (this.storageProvider != null) {
            try {
                this.storageProvider.flush();
            } catch (AgentStorageException exc) {
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

    public static AgentStorageProvider createStorageProvider(AgentConfig cfg) throws AgentConfigException {
        AgentStorageProvider provider;
        Class storageClass;

        try {
            storageClass = Class.forName(cfg.getStorageProvider());
        } catch (ClassNotFoundException exc) {
            throw new AgentConfigException("Storage provider not found: " +
                    exc.getMessage());
        }

        try {
            provider = (AgentStorageProvider) storageClass.newInstance();
            provider.init(cfg.getStorageProviderInfo());
        } catch (IllegalAccessException exc) {
            throw new AgentConfigException("Unable to access storage " +
                    "provider '" +
                    cfg.getStorageProvider() +
                    "': " + exc.getMessage());
        } catch (InstantiationException exc) {
            throw new AgentConfigException("Unable to instantiate storage " +
                    "provider '" +
                    cfg.getStorageProvider() +
                    "': " + exc.getMessage());
        } catch (AgentStorageException exc) {
            throw new AgentConfigException("Storage provider unable to " +
                    "initialize: " + exc.getMessage());
        }
        return provider;
    }

    /**
     * Configure the agent to run with new parameters. This routine will load jars, open sockets,
     * and other resources in preparation for running.
     * @param cfg Configuration to use to configure the Agent
     * @throws AgentRunningException indicating the Agent was running when a reconfiguration was attempted.
     * @throws AgentConfigException  indicating the configuration was invalid.
     */
    public void configure(AgentConfig cfg) throws AgentRunningException, AgentConfigException {
        DefaultConnectionListener defListener;
        if (running.get()) throw new AgentRunningException("Agent cannot be configured while running");

        //add lib/handlers/lib/*.jar classpath for handlers only
        String libHandlersLibDir = cfg.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS_LIB[0]);
        File handlersLib = new File(libHandlersLibDir);
        if (handlersLib.exists()) {
            this.handlerClassLoader.addURL(handlersLib);
        }

        this.dispatcher = null; //new CommandDispatcher();

        this.storageProvider = AgentDaemon.createStorageProvider(cfg);

        // Determine the hostname.  This will be stored in the storage provider
        // and checked on each agent invocation.  This determines if this agent
        // installation has been copied from another machine without removing 
        // the data directory.
        String currentHost = GenericPlugin.getPlatformName();

        String storedHost = this.storageProvider.getValue(PROP_HOSTNAME);
        if (storedHost == null || storedHost.length() == 0) {
            this.storageProvider.setValue(PROP_HOSTNAME, currentHost);
        } else {
            // Validate
            if (!storedHost.equals(currentHost)) {
                String err = "Invalid hostname '" + currentHost + "'. This agent has been configured for '"
                        + storedHost + "'. If this agent has been copied from a different machine please remove " +
                        "the data directory and restart the agent";
                throw new AgentConfigException(err);
            }
        }

        this.listener = new CommandListener(this.dispatcher);
        defListener = new DefaultConnectionListener(cfg);
        this.setConnectionListener(defListener);

        // set the lather proxy host and port if applicable
        if (cfg.isProxyServerSet()) {
            logger.info("Setting proxy server: host=" + cfg.getProxyIp() + " port=" + cfg.getProxyPort());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST, cfg.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT, String.valueOf(cfg.getProxyPort()));
        }

        this.serverHandlers = new ArrayList<AgentServerHandler>();

        // Load server handlers on the fly from lib/*.jar.  Server handler
        // jars  must have a Main-Class that implements the AgentServerHandler
        // interface.
        String libHandlersDir = cfg.getBootProperties().getProperty(AgentConfig.PROP_LIB_HANDLERS[0]);

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

        if (certDN == null || certDN.length() == 0) {
            certDN = generateCertDN();

            this.storageProvider.setValue(PROP_CERTDN, certDN);
            try {
                this.storageProvider.flush();
            } catch (AgentStorageException ase) {
                throw new AgentConfigException("Error storing certdn in agent storage: " + ase);
            }
        }

        this.bootConfig = cfg;
    }

    private void loadAgentServerHandlerJars(File[] libJars) throws AgentConfigException {
        AgentServerHandler loadedHandler;

        // Save the current context loader, and reset after we load plugin jars
        ClassLoader currentContext = Thread.currentThread().getContextClassLoader();

        for (File libJar : libJars) {
            try {
                JarFile jarFile = new JarFile(libJar);
                Manifest manifest = jarFile.getManifest();
                String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                if (mainClass != null) {
                    String jarPath = libJar.getAbsolutePath();
                    loadedHandler = this.handlerLoader.loadServerHandler(jarPath);
                    this.serverHandlers.add(loadedHandler);
                    //this.dispatcher.addServerHandler(loadedHandler);
                }
            } catch (Exception e) {
                throw new AgentConfigException("Failed to load " + "'" + libJar + "': " + e.getMessage());
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

    public void registerMonitor(String monitorName, AgentMonitorInterface monitor) {
        this.monitorClients.put(monitorName, monitor);
    }

    public AgentMonitorValue[] getMonitorValues(String monitorName, String[] monitorKeys) {
        AgentMonitorInterface iface = this.monitorClients.get(monitorName);
        if (iface == null) {
            AgentMonitorValue[] res;
            AgentMonitorValue badVal;

            badVal = new AgentMonitorValue();
            badVal.setErrCode(AgentMonitorValue.ERR_BADMONITOR);

            res = new AgentMonitorValue[monitorKeys.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = badVal;
            }
            return res;
        }

        return iface.getMonitorValues(monitorKeys);
    }

    /**
     * Tell the Agent to close all connections, and die.
     * @throws AgentRunningException indicating the Agent was not running when die() was called.
     */
    public void die() throws AgentRunningException {
        if (!running.get()) {
            throw new AgentRunningException("Agent is not running");
        }
        this.listener.die();
    }

    /**
     * A stub API which allows handlers to set the connection listener
     * that the agent uses.
     */
    public void setConnectionListener(AgentConnectionListener newListener) throws AgentRunningException {
        logger.info("Set connection listener to " + newListener);
        this.listener.setConnectionListener(newListener);
    }

    private void startPluginManagers() throws AgentStartException {
        try {
            Properties bootProps = this.bootConfig.getBootProperties();

            this.ppm = new ProductPluginManager(bootProps);

            this.ppm.init();

            String pluginDir = bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);

            Collection<PluginInfo> plugins = new ArrayList<PluginInfo>();
            plugins.addAll(this.ppm.registerPlugins(pluginDir));
            //check .. and higher for hq-plugins
            plugins.addAll(this.ppm.registerCustomPlugins(".."));
            sendPluginStatusToServer(plugins);

            logger.info("Product Plugin Manager initalized");
        } catch (Exception e) {
            // ...an unexpected exception has occurred that was not handled
            logger.error("Error initializing plugins ", e);
            throw new AgentStartException("Unable to initialize plugin  manager: " + e.getMessage());
        }
    }

    private void sendPluginStatusToServer(final Collection<PluginInfo> plugins) {
        // server may be down or Provider may not be setup.  Either way we want to retry until
        // the data is sent
        new Thread("PluginStatusSender") {
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
                        logger.debug(e, e);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.debug(e, e);
                    }
                }
            }
        }.start();
    }

    private void startHandlers() throws AgentStartException {
        for (AgentServerHandler serverHandler : this.serverHandlers) {
            AgentServerHandler handler = serverHandler;
            /*try {
                handler.startup(this);
            } catch (AgentStartException exc) {
                logger.error("Error starting plugin " + handler, exc);
                throw exc;
            } catch (Exception exc) {
                logger.error("Unknown exception", exc);
                throw new AgentStartException("Error starting plugin " + handler, exc);
            }*/
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
        for (int i = 0; i < files.length; i++) {
            try {
                files[i].delete();
            } catch (SecurityException e) {
            }
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
     * @throws AgentStartException indicating the Agent was unable to start,
     *                             or one of the plugins failed to start.
     */
    public void start() throws AgentStartException {
        // running.set(true);

        try {
            Properties bootProps = this.bootConfig.getBootProperties();
            String tmpDir = bootProps.getProperty(AgentConfig.PROP_TMPDIR[0]);
            if (tmpDir != null) {
                try {
                    // update plugins residing in tmp directory prior to cleaning it up
                    List updatedPlugins = AgentUpgradeManager.updatePlugins(bootProps);
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

            this.monitorClients = new ConcurrentHashMap<CharSequence, AgentMonitorInterface>();
            this.registerMonitor("agent", this);
            this.registerMonitor("agent.commandListener", this.listener);
            redirectStreams(bootProps);

            // Load the agent transport in the server handler classloader.
            // This is necessary because we don't want the jboss remoting 
            // classes in the root agent classloader - this causes conflicts 
            // with the jboss plugins.
            String agentTransportLifecycleClass = "org.hyperic.hq.agent.server.AgentTransportLifecycleImpl";

            try {
                Class type = this.handlerClassLoader.loadClass(agentTransportLifecycleClass);
                Constructor constructor = type.getConstructor(new Class[]{AgentDaemon.class, AgentConfig.class, AgentStorageProvider.class});

                this.agentTransportLifecycle = (AgentTransportLifecycle) constructor.newInstance(this, getBootConfig(), getStorageProvider());

            } catch (ClassNotFoundException e) {
                throw new AgentStartException("Cannot find agent transport lifecycle class: " + agentTransportLifecycleClass);
            }

            this.startPluginManagers();
            this.startHandlers();

            // The started handlers should have already registered with the 
            // agent transport lifecycle
            this.agentTransportLifecycle.startAgentTransport();

            this.listener.setup();

            tryForceAgentFailure();

            started.set(true);
            
            logger.info("Agent started successfully");

            this.sendNotification(NOTIFY_AGENT_UP, "we're up");

            this.listener.listen();

            this.sendNotification(NOTIFY_AGENT_DOWN, "going down");

        } catch (AgentStartException exc) {
            logger.error(exc.getMessage(), exc);
            throw exc;
        } catch (Exception exc) {
            logger.error("Error running agent", exc);
            throw new AgentStartException("Error running agent: " + exc.getMessage());
        } catch (Throwable exc) {
            logger.error("Critical error running agent", exc);
            // We don't flush the storage here, since we may be out of
            // memory, etc -- stuff just isn't in a good state at all.
            if (this.storageProvider != null) {
                this.storageProvider.dispose();
                this.storageProvider = null;
            }

            throw new AgentStartException("Critical shutdown");
        } finally {
             
            if (!started.get()) {
                logger.debug("Notifying that agent startup failed");
                this.sendNotification(NOTIFY_AGENT_FAILED_START, "agent startup failed!");
            }

            if (this.agentTransportLifecycle != null) {
                this.agentTransportLifecycle.stopAgentTransport();
            }

            running.set(false);

            try {
                this.cleanup();
            } catch (AgentRunningException exc) {
            }

            if (this.storageProvider != null) {
                this.storageProvider.dispose();
            }

            logger.info("Agent shut down");
        }
    }

    private void tryForceAgentFailure() throws AgentStartException {
        String rollbackBundle = getBootConfig().getBootProperties()
                .getProperty(AgentConfig.PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE[0]);

        if (getCurrentAgentBundle().equals(rollbackBundle)) {
            throw new AgentStartException(AgentConfig.PROP_ROLLBACK_AGENT_BUNDLE_UPGRADE[0] +
                    " property set to force rollback of agent bundle upgrade: " +
                    rollbackBundle);
        }
    }

    public static class RunnableAgent implements Runnable {

        private final AgentConfig config;

        public RunnableAgent(AgentConfig config) {
            this.config = config;
        }

        public void run() {
            boolean isConfigured = false;
            AgentDaemon agent = null;

            try {
                agent = AgentDaemon.newInstance(config);
                isConfigured = true;
            } catch (AgentConfigException e) {
                logger.error("Agent configuration error: ", e);
            } catch (Exception e) {
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

                } catch (AgentStartException e) {
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
