/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.agent.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.*;
import org.hyperic.hq.agent.server.monitor.AgentMonitorInterface;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginInfo;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.tanukisoftware.wrapper.WrapperManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Helena Edelson
 */
@Component
public class AgentLifecycleService implements AgentService, SmartLifecycle {

    private static final Log logger = LogFactory.getLog(AgentLifecycleService.class);

    //private PluginLoader handlerClassLoader;

    private CommandDispatcher dispatcher;

    private AgentStorageProvider storageProvider;

    private CommandListener listener;

    private AgentTransportLifecycle agentTransportLifecycle;

    private List<AgentServerHandler> serverHandlers = new ArrayList<AgentServerHandler>();

    private List<AgentServerHandler> startedHandlers = new ArrayList<AgentServerHandler>();

    private AgentConfig bootConfig;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean continuable = new AtomicBoolean(false);

    private ProductPluginManager productPluginManager;

    private AgentManager agentManager;

    private volatile ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    /**
     * Create a new AbstractAgentService object based on a passed configuration
     * @param agentManager the agent manager
     * @param dispatcher   the command dispatcher
     * @throws org.hyperic.hq.agent.AgentConfigException
     *          indicating the passed configuration is invalid.
     */
    @Autowired
    public AgentLifecycleService(AgentManager agentManager, CommandDispatcher dispatcher, List<AgentServerHandler> serverHandlers)
            throws AgentConfigException, AgentRunningException {

        this.serverHandlers.addAll(serverHandlers);
        this.agentManager = agentManager;
        this.dispatcher = dispatcher;
        this.listener = new CommandListener(dispatcher);
    }

    public void start(final AgentConfig config) {
        Thread agent = new Thread(new RunnableAgent(config), "AgentThread");
        agent.start();

        //taskExecutor.execute(new RunnableAgent(config));
    }

    public boolean isRunning() {
        return started.get();
    }

    public boolean isAutoStartup() {
        return false;
    }

    public void stop(Runnable callback) {
        stop();
    }

    public int getPhase() {
        return 0;
    }

    /**
     * Handle for agents to call dispatcher.processRequest()
     * @return org.hyperic.hq.agent.server.CommandDispatcher
     */
    public CommandDispatcher getDispatcher() {
        return dispatcher; // != null ? dispatcher : new CommandDispatcher();
    }

    /**
     * Start the Agent's listening process.
     * This routine blocks for the entire execution of the Agent.
     * @throws AgentStartException indicating the Agent was unable to start,
     *                             or one of the plugins failed to start.
     */
    public void start() {
        if (!running.get()) return;

        try {
            Properties bootProps = bootConfig.getBootProperties();

            agentManager.doInitialCleanup(bootProps);

            registerMonitor("agent", agentManager);
            registerMonitor("agent.commandListener", listener);

            agentManager.redirectStreams(bootProps);

            /*TODO once jboss remoting is ripped out, remove. */
            this.agentTransportLifecycle = new AgentTransportLifecycleImpl(this);

            startPluginManagers();

            agentManager.startHandlers(this, serverHandlers, startedHandlers);

            dispatcher.addServerHandlers(startedHandlers);

            agentTransportLifecycle.startAgentTransport();

            listener.setup();

            agentManager.tryForceAgentFailure(bootConfig);

            agentManager.sendNotification(getNotifyAgentUp(), "we're up");

        } catch (Throwable t) {
            System.out.println("*******Critical error starting agent: " + t);
            logger.error("Error starting the agent", t);
            agentManager.sendNotification(getNotifyAgentDown(), "going down");

            /* We don't flush the storage here, since we may be out of memory */
            if (storageProvider != null) {
                storageProvider.dispose();
                storageProvider = null;
            }
            started.set(false);
        }
    }


    /**
     * Cleanup any internal resources the agent is using.  The Agent must not be running
     * when this function is called.  It may raise an exception if some of the cleanup
     * fails, however the Agent will be in a pristine state after calling this function.
     * Cleanup may be called more than one time without re-configuring in between cleanup() calls.
     * <p/>
     * Shutdown the serverhandlers first, in case they need to write something to storage
     * @throws AgentRunningException indicating the Agent was running when the routine was called.
     */
    public void stop() {
        if (isRunning()) {
            logger.info("Agent cannot be cleaned up while running");
            return;
        }

        if (startedHandlers != null) {
            for (AgentServerHandler handler : this.startedHandlers) {
                handler.shutdown();
            }
            serverHandlers = null;
            startedHandlers = null;
        }

        dispatcher = null;
        if (listener != null) {
            listener.cleanup();
            listener = null;
        }

        if (storageProvider != null) {
            try {
                storageProvider.flush();
            } catch (AgentStorageException exc) {
                logger.error("Failed to flush Agent storage", exc);
            } finally {
                storageProvider.dispose();
                storageProvider = null;
            }
        }

        try {
            productPluginManager.shutdown();
        } catch (PluginException e) {
            // Not much we can do
        }
    }

    /**
     * Tell the Agent to close all connections, and die.
     * @throws AgentRunningException indicating the Agent was not running when die() was called.
     */
    public void die() throws AgentRunningException {
        if (!isRunning()) throw new AgentRunningException("Agent is not running");
        listener.die();
    }

    public class RunnableAgent implements Runnable {

        private final AgentConfig config;

        private final CountDownLatch start;

        public RunnableAgent(AgentConfig config) {
            this.config = config;
            this.start = new CountDownLatch(1);
        }

        public void run() {

            try {
                System.out.println("- Initializing the Agent");
                initialize(config);
                continuable.set(true);

                try {
                    running.set(true);
                    start(); 
                    continuable.set(true);

                    System.out.println("- Agent started");


                    if (continuable.get() && running.get()) {
                        System.out.println("AgentLifecycleService - Agent Listener started");

                        //start.countDown();
                        try {
                            started.set(true);
                            while(!started.get()) {
                                
                            }
                            listener.listen();
                        } catch (Throwable t) {
                            listener.die(); 
                            System.out.println("AgentLifecycleService - A problem occurred with the listener. Stopping the Agent");
                            throw t;
                        }
                    }

                    running.set(false);
                    // Set runtime start.getCount();
                    agentManager.sendNotification(getNotifyAgentDown(), "going down");

                } catch (Throwable t) {
                    logger.error("Error running agent", t);
                    if (!started.get()) {
                        agentManager.sendNotification(getNotifyAgentFailedStart(), "agent startup failed!");
                    }
                    throw new AgentStartException("Error running agent: " + t.getMessage());
                }

            } catch (Throwable t) {  
                logger.error("Agent thread interrupted, processing stopped: ", t);
                Thread.currentThread().interrupt();

                if (t.getCause() instanceof AgentConfigException) {
                    logger.error("Agent configuration error: ", t.getCause());
                }
                else if (t.getCause() instanceof AgentStartException) {
                    logger.error("Agent startup failed: ", t.getCause());
                }
            } finally {
                /*continuable.set(false); running.set(false); started.set(false);*/

                if (!continuable.get()) {
                    cleanUpOnAgentConfigFailure(bootConfig);
                } else if (!started.get()) {
                    cleanUpOnAgentStartFailure();
                }
            }
        }

        /**
         * Configure the agent to run with new parameters. This routine will load jars, open sockets,
         * and other resources in preparation for running.
         * @param config Configuration to use to configure the Agent
         * @throws org.hyperic.hq.agent.server.AgentRunningException
         *                              indicating the Agent was running when a reconfiguration was attempted.
         * @throws AgentConfigException indicating the configuration was invalid.
         */
        private void initialize(AgentConfig config) throws AgentRunningException, AgentConfigException {
            if (running.get()) throw new AgentRunningException("Agent cannot be configured while running");

            storageProvider = agentManager.createStorageProvider(config);
            agentManager.addProviderCertificate(storageProvider);   
            listener.setConnectionListener(new DefaultConnectionListener(config));
            agentManager.setProxy(config); // remove when remoting is removed
            bootConfig = config;
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
     * Retreive a plugin manager
     * @param type The type of plugin manager that is wanted
     * @return The plugin manager for the type given
     * @throws org.hyperic.hq.agent.server.AgentRunningException
     *          indicating the agent was not running
     * @throws org.hyperic.hq.product.PluginException
     *          if the requested manager was not found
     */
    public PluginManager getPluginManager(String type) throws AgentRunningException, PluginException {
        if (!running.get())
            throw new AgentRunningException("Plugin manager cannot be retrieved if the Agent is not running");

        return productPluginManager.getPluginManager(type);
    }

    private void startPluginManagers() throws AgentStartException {
        try {
            Properties bootProps = bootConfig.getBootProperties();
            productPluginManager = new ProductPluginManager(bootProps);
            productPluginManager.init();

            String pluginDir = bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);

            Collection<PluginInfo> plugins = new ArrayList<PluginInfo>();
            plugins.addAll(productPluginManager.registerPlugins(pluginDir));

            plugins.addAll(productPluginManager.registerCustomPlugins(".."));
            agentManager.sendPluginStatusToServer(plugins, storageProvider);

            logger.info("Product Plugin Manager initalized");
        } catch (Exception e) {
            logger.error("Error initializing plugins ", e);
            throw new AgentStartException("Unable to initialize plugin  manager: " + e.getMessage());
        }
    }


    /**
     * @return The current agent bundle name.
     */
    public String getCurrentAgentBundle() {
        return agentManager.getCurrentAgentBundle(bootConfig);
    }

    /**
     * Retrieve the storage object in use by the Agent.  This routine should
     * be used primarily by server handlers wishing to do any kind of storage.
     * @return The storage provider object used by the Agent
     * @throws AgentRunningException indicating the Agent was not running when the request was made.
     */
    public AgentStorageProvider getStorageProvider() throws AgentRunningException {
        if (!running.get()) throw new AgentRunningException("Storage cannot be retrieved if the Agent is not running");
        return storageProvider;
    }

    /**
     * A stub API which allows handlers to set the connection listener that the agent uses.
     */
    public void setConnectionListener(AgentConnectionListener connectionListener) throws AgentRunningException {
        logger.info("Set connection listener to " + listener);
        listener.setConnectionListener(connectionListener);
    }

    public void registerMonitor(String monitorName, AgentMonitorInterface monitor) {
        agentManager.registerMonitor(monitorName, monitor);
    }

    public void registerNotifyHandler(AgentNotificationHandler handler, String msgClass) {
        agentManager.registerNotifyHandler(handler, msgClass);
    }

    public AgentMonitorValue[] getMonitorValues(String monitorName, String[] monitorKeys) {
        return agentManager.getMonitorValues(monitorName, monitorKeys);
    }

    public AgentMonitorValue[] getMonitorValues(String[] monitorKeys) {
        return getMonitorValues(null, monitorKeys);
    }

    /**
     * Get the bootstrap configuration that the Agent was initialized with.
     * @return The configuration object used to initialize the Agent.
     */
    public AgentConfig getBootConfig() {
        return bootConfig;
    }

    public void sendNotification(String msgClass, String message) {
        agentManager.sendNotification(msgClass, message);
    }

    public String getNotifyAgentUp() {
        return "AgentService.agentUp";
    }

    public String getNotifyAgentDown() {
        return "AgentService.agentDown";
    }

    public String getNotifyAgentFailedStart() {
        return "AgentService.agentFailedStart";
    }

    public String getCertDn() {
        return agentManager.getCertDn();
    }


}
