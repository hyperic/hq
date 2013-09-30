/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.agent.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPI;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.AgentStreamPair;
import org.hyperic.util.timer.StopWatch;

/**
 * The object which represents the connection between the client and the
 * Agent.  It holds Agent contact information, and may perform connection 
 * caching.
 */

public class AgentConnection {
    private static final Log log = LogFactory.getLog(AgentConnection.class);
    private static final int MAX_RETRIES = 5;
    private static final long SLEEP_TIME = 3000;
    private static final int SOCKET_TIMEOUT = 60000;
    private String   _agentAddress;
    private int      _agentPort;
    private AgentAPI _agentAPI;

    /**
     * Create a connection to an Agent with the specified address and
     * port.
     *
     * @param agentAddress IP address of the Agent
     * @param agentPort    Port the Agent is listening on
     */

    public AgentConnection(String agentAddress, int agentPort) {
        _agentAddress = agentAddress;
        _agentPort    = agentPort;
        _agentAPI     = new AgentAPI();
    }

    public String getAgentAddress() {
        return _agentAddress;
    }

    public int getAgentPort() {
        return _agentPort;
    }

    protected Socket getSocket() throws IOException {
        Socket s;
        // Connect to the remote agent
        try {
            s = new Socket(_agentAddress, _agentPort);
        } catch(IOException exc){
            String msg = "Error connecting to agent @ ("+ _agentAddress+ 
                         ":"+ _agentPort+"): " + exc.getMessage();
            IOException toThrow = new IOException(msg);
            // call initCause instead of constructor to be java 1.5 compat
            toThrow.initCause(exc);
            throw toThrow;
        }
        s.setSoTimeout(SOCKET_TIMEOUT);
        return s;
    }

    /**
     * Send a command to the remote Agent.  This routine blocks, while sending
     * the data to the agent and waiting for the result.
     *
     * @param cmdName    Name of the remote method to execute
     * @param cmdVersion API version number belonging to the command
     * @param arg        Argument to send to the remote method
     *
     * @return an AgentRemoteValue object, as returned from the Agent.
     *
     * @throws AgentRemoteException indicating an error invoking the method.
     * @throws AgentConnectionException indicating a failure to connect to, or
     *                                  communicate with the agent.
     */
    public AgentRemoteValue sendCommand(String cmdName, int cmdVersion, AgentRemoteValue arg)
    throws AgentRemoteException, AgentConnectionException {
        if (log.isDebugEnabled()) log.debug(_agentAddress + ":" + _agentPort + " -> " + cmdName);
        AgentStreamPair sPair = this.sendCommandHeaders(cmdName, cmdVersion, arg);
        return this.getCommandResult(sPair);
    }

    /**
     * Send a command to the remote Agent.  This routine blocks, while sending
     * the data to the agent and waiting for the result.
     *
     * @param cmdName    Name of the remote method to execute
     * @param cmdVersion API version number belonging to the command
     * @param arg        Argument to send to the remote method
     * @param withRetires Tells the api to retry the command if an IOException is thrown during
     *                    its attempt to communicate to the agent
     *
     * @return an AgentRemoteValue object, as returned from the Agent.
     *
     * @throws AgentRemoteException indicating an error invoking the method.
     * @throws AgentConnectionException indicating a failure to connect to, or
     *                                  communicate with the agent.
     */
    public AgentRemoteValue sendCommand(String cmdName, int cmdVersion, AgentRemoteValue arg,
                                        boolean withRetries)
    throws AgentRemoteException, AgentConnectionException {
        if (log.isDebugEnabled()) log.debug(_agentAddress + ":" + _agentPort + " -> " + cmdName);
        AgentStreamPair sPair = this.sendCommandHeaders(cmdName, cmdVersion, arg, withRetries);
        return this.getCommandResult(sPair);
    }

    /**
     * Send the command to the agent, not waiting for it to process the
     * result.  This call must be paired with a single 'getCommandResult'
     * which is passed the stream pair which is returned from this
     * routine.  By calling sendCommandHeaders and getCommandResult
     * seperately, callers can use the stream pair to perform special
     * communication with the remote handlers, not supported by
     * the agent transportation framework (such as streamed file transfer).
     *
     * @param cmdName    Name of the remote method to execute
     * @param cmdVersion API version number belonging to the command
     * @param arg        Argument to send to the remote method
     *
     * @return an AgentRemoteValue object, as returned from the Agent.
     *
     * @throws AgentConnectionException indicating a failure to connect to, or
     *                                  communicate with the agent.
     */
    public AgentStreamPair sendCommandHeaders(String cmdName, int cmdVersion, AgentRemoteValue arg)
    throws AgentConnectionException {
        return sendCommandHeaders(cmdName, cmdVersion, arg, true);
    }

    private AgentStreamPair sendCommandHeaders(String cmdName, int cmdVersion, AgentRemoteValue arg,
                                               boolean withRetries)
    throws AgentConnectionException {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        try {
            if (debug) watch.markTimeBegin("cmdName=" + cmdName);
            if (withRetries) {
                return sendCommandHeadersWithRetries(cmdName, cmdVersion, arg, MAX_RETRIES);
            } else {
                return sendCommandHeadersWithRetries(cmdName, cmdVersion, arg, 1);
            }
        } catch(IOException exc){
            throw new AgentConnectionException(
                "Error sending argument: " + exc.getMessage() + ", cmd=" + cmdName, exc);
        } finally {
            if (debug) watch.markTimeEnd("cmdName=" + cmdName);
            if (debug) log.debug(watch);
        }
    }
        
    private AgentStreamPair sendCommandHeadersWithRetries(String cmdName, int cmdVersion,
                                                          AgentRemoteValue arg, int maxRetries)
    throws IOException {
        IOException ex = null;
        AgentStreamPair streamPair = null;
        Socket s = null;
        int tries = 0;
        while (tries++ < maxRetries) {
            try {
                s = getSocket();
                streamPair = new SocketStreamPair(s, s.getInputStream(), s.getOutputStream());
                DataOutputStream outputStream = new DataOutputStream(streamPair.getOutputStream());
                outputStream.writeInt(_agentAPI.getVersion());
                outputStream.writeInt(cmdVersion);
                outputStream.writeUTF(cmdName);
                arg.toStream(outputStream);
                outputStream.flush();
                return streamPair;
            } catch (IOException e) {
                ex = e;
                close(s);
            }
            if (tries >= maxRetries) {
                break;
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                log.debug(e,e);
            }
        }
        if (ex != null) {
            IOException toThrow =
                new IOException(ex.getMessage()+ ", retried " + MAX_RETRIES + " times");
            // call initCause instead of constructor to be java 1.5 compat
            toThrow.initCause(ex);
            throw toThrow;
        }
        return streamPair;
    }
    
    private void close(Socket s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch(IOException ex) {
            log.debug(ex, ex);
        }
    }

    /**
     * Get the result of command execution from the remote command handler.
     *
     * @param inStreamPair The pair which was returned from the associated
     *                     sendCommandHeaders invocation.
     *
     * @return an AgentRemoteValue object, as returned from the Agent.
     *
     * @throws AgentRemoteException indicating an error invoking the method.
     * @throws AgentConnectionException indicating a failure to communicate
     *                                  with the agent.
     */

    public AgentRemoteValue getCommandResult(AgentStreamPair inStreamPair)
        throws AgentRemoteException, AgentConnectionException
    {
        SocketStreamPair streamPair = (SocketStreamPair)inStreamPair;
        // Get the response
        try {
            DataInputStream inputStream;
            int isException;

            inputStream = new DataInputStream(streamPair.getInputStream());
            isException = inputStream.readInt();
            if(isException == 1){
                String exceptionMsg = inputStream.readUTF();
                
                throw new AgentRemoteException(exceptionMsg);
            } else {
                AgentRemoteValue result;

                result = AgentRemoteValue.fromStream(inputStream);
                return result;
            }
        } catch(EOFException exc){
            throw new AgentConnectionException("EOF received from Agent");
        } catch(IOException exc){
            throw new AgentConnectionException("Error reading result: " + exc.getMessage(), exc);
        } finally {
            close(streamPair);
        }
    }
    
    private void close(SocketStreamPair streamPair) {
        if (streamPair == null) {
            return;
        }
        try { 
            streamPair.close(); 
        } catch(IOException e){
            log.debug(e,e);
        }
    }

    public void closeSocket() {
        try {
            getSocket().close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }

    public boolean equals(Object o) {
        return o instanceof AgentConnection &&
            ((AgentConnection)o).getAgentAddress().equals(getAgentAddress()) &&
            ((AgentConnection)o).getAgentPort() == getAgentPort();
    }

    public int hashCode() {
        return getAgentAddress().hashCode() + getAgentPort();
    }

}
