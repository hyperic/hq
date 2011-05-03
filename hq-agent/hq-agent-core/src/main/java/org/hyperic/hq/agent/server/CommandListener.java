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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorIncalculableException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The object used to read serialized object data from the network and
 * send it to the CommandDispatcher.
 */

public class CommandListener
        extends AgentMonitorSimple {
    private static final int DIE_FREQUENCY = 1000;  // Time between death chex

    private CommandDispatcher dispatcher;
    private Log logger = LogFactory.getLog(CommandListener.class);

    private AtomicBoolean running = new AtomicBoolean(false);

    private volatile AgentConnectionListener listener;

    private int stat_numRequestsServed = 0;
    private int stat_numConnFailures = 0;
    private long stat_maxRequestTime = -1;
    private long stat_minRequestTime = -1;
    private long stat_totRequestTime = 0;

    /**
     * Setup a listener on a specified port, with a dispatcher containing
     * known methods which can be invoked.
     * @param dispatcher Object to call when a command comes across the wire
     */

    CommandListener(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    void setConnectionListener(AgentConnectionListener listener) throws AgentRunningException {
        if (running.get())  throw new AgentRunningException("Cannot replace listener while running");
        this.listener = listener;
    }

    /**
     * Shut down the command listener.
     * @throws AgentRunningException indicating the listener was not listening when 'die' was requested.
     */
    void die() throws AgentRunningException {
        if (!running.get()) throw new AgentRunningException("CommandListener not listening");
    }

    /**
     * Cleanup any resources in use by the object.  Notably, if the object
     * is constructed, but the listen is never called, this will need to be invoked.
     */
    void cleanup() {
        listener.cleanup();
    }

    void setup() throws AgentStartException {
        listener.setup(CommandListener.DIE_FREQUENCY);
    }

    /**
     * The main loop which blocks, waiting for connections.  Connections are
     * handled in a synchronous manner -- one connection is not processed
     * until the previous one is finished.
     */
    void listen() {
        long lastRequestTime;

        lastRequestTime = 0;
        //while(continuable){

        AgentServerConnection conn = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        Object dispatchResult = null;
        AgentCommand cmd;

        try {
            // Statistics block
            if (lastRequestTime != 0) {
                gatherLastRequestTimeStatistics(lastRequestTime);
            }

            lastRequestTime = 0;
            try {
                conn = this.listener.getNewConnection();
            } catch (InterruptedIOException exc) {
                System.out.println(this + " **********exc");
                // Timeout occurred
                    running.set(false);
                    listener.cleanup();
                    return;
                 
            } catch (AgentConnectionException exc) {
                System.out.println(this + " **********exc");
                this.stat_numConnFailures++;
                this.logger.error("Failed handling new connection", exc);
                //continue;
            }

            lastRequestTime = System.currentTimeMillis();
            this.stat_numRequestsServed++;

            try {
                inputStream = conn.getInputStream();
                outputStream = conn.getOutputStream();
            } catch (AgentConnectionException exc) {
                System.out.println(this + " **********exc");
                this.stat_numConnFailures++;
                this.logger.error("Failed to get connection r/w handles", exc);
                //continue;
            }

            cmd = null;
            try {
                cmd = conn.readCommand();
                this.logger.debug("Dispatching request for '" +
                        cmd.getCommand() + "'");

                dispatchResult =
                        this.dispatcher.processRequest(cmd, inputStream,
                                outputStream);
            } catch (AgentConnectionException exc) {
                System.out.println(this + " **********exc");
                this.stat_numConnFailures++;
                this.logger.error("Failed to read method/args from client",
                        exc);
                dispatchResult = exc;
            } catch (AgentRemoteException exc) {
                System.out.println(this + " **********exc");
                // This is a fine result, raised via the AgentServerHandler
                // interface
                dispatchResult = exc;
            } catch (EOFException exc) {
                System.out.println(this + " **********exc");
                logger.debug(exc, exc);
                //continue;
            } catch (Exception exc) {
                System.out.println(this + " **********exc");
                // Catch-all so we don't blow up the program when a plugin
                // fails.
                dispatchResult = exc;
            }

            try {
                if (dispatchResult instanceof AgentRemoteException) {
                    this.logger.warn("Error invoking method",
                            (Exception) dispatchResult);
                    conn.sendErrorResponse(((AgentRemoteException) dispatchResult)
                            .getMessage());
                } else if (dispatchResult instanceof Exception) {
                    this.logger.warn("Error invoking method",
                            (Exception) dispatchResult);
                    conn.sendErrorResponse(((Exception) dispatchResult).
                            toString());
                } else if (dispatchResult == null) {
                    this.logger.debug("Method '" + cmd + "' returned null ");
                    conn.sendSuccessResponse(new AgentRemoteValue());
                } else {
                    this.logger.debug("Method '" + cmd + "' returned an " +
                            "object result");
                    conn.sendSuccessResponse((AgentRemoteValue) dispatchResult);
                }
            } catch (AgentConnectionException excIO) {
                System.out.println(this + " **********exc");
                // Close the socket
                this.logger.error("Error writing result to client", excIO);
            } finally {
                try {
                    outputStream.flush();
                } catch (IOException ignExc) {
                    System.out.println(this + " **********exc");
                }
                try {
                    outputStream.close();
                } catch (IOException ignExc) {
                    System.out.println(this + " **********exc");
                }
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void gatherLastRequestTimeStatistics(long lastRequestTime) {
        long requestTime = System.currentTimeMillis() - lastRequestTime;

        this.stat_totRequestTime += requestTime;
        if (requestTime > this.stat_maxRequestTime ||
                this.stat_maxRequestTime == -1) {
            this.stat_maxRequestTime = requestTime;
        }

        if (requestTime < this.stat_minRequestTime ||
                this.stat_minRequestTime == -1) {
            this.stat_minRequestTime = requestTime;
        }
    }

    /**
     * MONITOR METHOD:  Gets the number of requests served
     */
    public double getNumRequestsServed()
            throws AgentMonitorException {
        return this.stat_numRequestsServed;
    }

    /**
     * MONITOR METHOD:  Gets the number of connection failures
     */
    public double getNumConnFailures()
            throws AgentMonitorException {
        return this.stat_numConnFailures;
    }

    /**
     * MONITOR METHOD:  Gets the total number of milliseconds spent
     * in requests
     */
    public double getTotalRequestTime()
            throws AgentMonitorException {
        return this.stat_totRequestTime;
    }

    /**
     * MONITOR METHOD:  Gets the longest request time in milliseconds
     */
    public double getMaxRequestTime()
            throws AgentMonitorException {
        if (this.stat_maxRequestTime == -1) {
            throw new AgentMonitorIncalculableException("No requests yet");
        }
        return this.stat_maxRequestTime;
    }

    /**
     * MONITOR METHOD:  Gets the shortest request time in milliseconds
     */
    public double getMinRequestTime()
            throws AgentMonitorException {
        if (this.stat_minRequestTime == -1) {
            throw new AgentMonitorIncalculableException("No requests yet");
        }
        return this.stat_minRequestTime;
    }
}
