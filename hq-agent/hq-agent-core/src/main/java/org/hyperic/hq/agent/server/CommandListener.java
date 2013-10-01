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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentCommandsAPI;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;


/**
 * The object used to read serialized object data from the network and
 * send it to the CommandDispatcher.
 */
public class CommandListener extends AgentMonitorSimple {
    private static final int POLL_FREQUENCY = 1000;
    private static final String GENERIC_POOL = "generic";
    private static final String[] THREAD_POOLS = new String[] {
        AgentCommandsAPI.command_ping, AgentCommandsAPI.command_receive_file, GENERIC_POOL};
    private final Map<String, ExecutorService> threadPools = new HashMap<String, ExecutorService>();
    private final CommandDispatcher dispatcher; // Dispatcher which handles commands
    private final Log log = LogFactory.getLog(CommandListener.class);
    /** Shutdown the listenLoop thread and all thread pools */
    private AtomicBoolean shutdown = new AtomicBoolean(true);
    private AtomicReference<AgentConnectionListener> listener = new AtomicReference<AgentConnectionListener>();

    /**
     * Setup a listener on a specified port, with a dispatcher containing
     * known methods which can be invoked.
     *
     * @param dispatcher Object to call when a command comes across the wire
     */
    CommandListener(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        setupThreadPools();
    }

    private void setupThreadPools() {
        for (final String cmdName : THREAD_POOLS) {
            final String poolName = cmdName.replace(AgentCommandsAPI.commandPrefix, "");
            final ExecutorService pool = Executors.newFixedThreadPool(1, new ThreadFactory() {
                private final AtomicLong num = new AtomicLong(0);
                public Thread newThread(Runnable r) {
                    Thread rtn = new Thread(r, "commandlistener-" + poolName + "-" + num.getAndIncrement());
                    rtn.setDaemon(true);
                    return rtn;
                }
            });
            threadPools.put(cmdName, pool);
        }
    }

    void setConnectionListener(final AgentConnectionListener listener) throws AgentRunningException {
        if (!shutdown.get()){
            throw new AgentRunningException("Cannot replace listener while running");
        }
        this.listener.set(listener);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (listener != null) {
                        listener.cleanup();
                    }
                } catch (Throwable e) {
                    log.error("could not close socket connection: " + e,e);
                }
            }
        });
    }

    /**
     * Shut down the command listener.
     *
     * @throws AgentRunningException indicating the listener was not listening
     *                               when 'die' was requested.
     */
    void die() throws AgentRunningException {
        shutdown.set(true);
        for (final Entry<String, ExecutorService> entry : threadPools.entrySet()) {
            final ExecutorService pool = entry.getValue();
            pool.shutdownNow();
            log.info("Shut down executor service for CommandListener " + entry.getKey());
        }
    }

    /**
     * Cleanup any resources in use by the object.  Notably, if the object
     * is constructed, but the listenLoop is never called, this will need to
     * be invoked.
     */

    void cleanup() {
        listener.get().cleanup();
    }

    void setup() throws AgentStartException {
        listener.get().setup(CommandListener.POLL_FREQUENCY);
    }

    /**
     * The main loop which blocks, waiting for connections.  Connections are
     * handled in a synchronous manner -- one connection is not processed
     * until the previous one is finished.
     */
    void listenLoop() {
    	boolean logDebug = log.isDebugEnabled();
        shutdown.set(false);
        while (!shutdown.get()) {
            try {
                try {
                    final AgentServerConnection conn = listener.get().getNewConnection();
                    if (logDebug) log.debug("Opened new connection");
                    final AgentCommand cmd = conn.readCommand();
                    final ExecutorService pool = getPool(cmd);
                    if (logDebug) log.debug("Dispatching command " + cmd.getCommand() + " to pool: " + pool);
                    pool.execute(new AgentDispatchTask(conn, cmd));
                    if (logDebug) log.debug("Done dispatching command " + cmd.getCommand() + " to pool " + pool);
                } catch (EOFException e) {
                    log.debug(e, e);
                } catch (InterruptedIOException e){
                    if (shutdown.get()) {
                        listener.get().cleanup();
                        return;
                    }
                    continue;
                } catch (AgentConnectionException e){
                    if (!shutdown.get()) {
                        log.error("Failed handling new connection: " + e, e);
                    }
                    continue;
                }
            } catch (Throwable t) {
                // only log to error if the agent isn't shutting down
                if (!shutdown.get()) {
                    log.error(t,t);
                } else {
                    log.debug(t,t);
                }
            }
        }
    }
    
    private ExecutorService getPool(AgentCommand cmd) {
        final ExecutorService pool = threadPools.get(cmd.getCommand());
        return (pool == null) ? threadPools.get(GENERIC_POOL) : pool;
    }

    private class AgentDispatchTask implements Runnable {
        private AgentServerConnection conn;
        private AgentCommand cmd;
        public AgentDispatchTask(AgentServerConnection conn, AgentCommand cmd) {
            this.conn = conn;
            this.cmd = cmd;
        }
        public void run() {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = conn.getInputStream();
                outputStream = conn.getOutputStream();
                handleConn(conn, cmd, inputStream, outputStream);
            } catch (Throwable t) {
                log.error(t,t);
            } finally {
                try {outputStream.flush();} catch(IOException ignExc) {}
                try {outputStream.close();} catch(IOException ignExc) {}
                try {inputStream.close();} catch(IOException ignExc) {}
                conn.close();
            }
        }
    }

    private void handleConn(AgentServerConnection conn, AgentCommand cmd, InputStream inputStream, OutputStream outputStream) {
        Object dispatchResult;
        try {
            log.debug("Dispatching request for '" + cmd.getCommand() + "'");
            dispatchResult = dispatcher.processRequest(cmd, inputStream, outputStream);
        } catch (AgentRemoteException e) {
            // This is a fine result, raised via the AgentServerHandler interface
            dispatchResult = e;
        }
        try {
            if (dispatchResult instanceof AgentRemoteException){
                log.warn("Error invoking method",
                                 (Exception)dispatchResult);
                String message = ((AgentRemoteException)dispatchResult).getMessage();
                if (message == null){
                    message = "Problem occurred without an error message, see stacktrace for more information.";
                }
                conn.sendErrorResponse(message);
            } else if(dispatchResult instanceof Exception){
                log.warn("Error invoking method", (Exception)dispatchResult);
                conn.sendErrorResponse(((Exception)dispatchResult).
                                       toString());
            } else if(dispatchResult == null){
                log.debug("Method '" + cmd + "' returned null ");
                conn.sendSuccessResponse(new AgentRemoteValue());
            } else {
                log.debug("Method '" + cmd + "' returned an object result");
                conn.sendSuccessResponse((AgentRemoteValue)dispatchResult);
            }
        } catch(AgentConnectionException e) {
            log.error("Error writing result to client: " + e, e);
        }
    }

}
