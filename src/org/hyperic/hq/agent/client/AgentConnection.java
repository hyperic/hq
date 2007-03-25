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

package org.hyperic.hq.agent.client;

import org.hyperic.hq.agent.AgentAPI;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.AgentStreamPair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The object which represents the connection between the client and the
 * Agent.  It holds Agent contact information, and may perform connection 
 * caching.
 */

public class AgentConnection {
    private String   _agentAddress;
    private int      _agentPort;
    private AgentAPI _agentAPI;

    private final Log _log = LogFactory.getLog(AgentConnection.class);

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

    protected Socket getSocket()
        throws AgentConnectionException
    {
        Socket s;

        // Connect to the remote agent
        try {
            s = new Socket(_agentAddress, _agentPort);
        } catch(IOException exc){
            String msg;

            msg = "Error connecting to agent @ ("+ _agentAddress+ ":"+
                _agentPort+"): " + exc.getMessage();
            throw new AgentConnectionException(msg);
        }

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

    public AgentRemoteValue sendCommand(String cmdName, int cmdVersion, 
                                        AgentRemoteValue arg)
        throws AgentRemoteException, AgentConnectionException
    {
        AgentStreamPair sPair;

        // XXX: DEBUG: Print out commands made to agents
        _log.info(_agentAddress + " -> " + cmdName);
        sPair = this.sendCommandHeaders(cmdName, cmdVersion, arg);
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

    public AgentStreamPair sendCommandHeaders(String cmdName, int cmdVersion, 
                                              AgentRemoteValue arg)
        throws AgentConnectionException
    {
        DataOutputStream outputStream;
        AgentStreamPair streamPair;
        Socket s;

        // Get the actual connection
        s = this.getSocket();

        // Send the command
        try {
            streamPair = new SocketStreamPair(s, s.getInputStream(),
                                              s.getOutputStream());
            outputStream = new DataOutputStream(streamPair.getOutputStream());
            outputStream.writeInt(_agentAPI.getVersion());
            outputStream.writeInt(cmdVersion);
            outputStream.writeUTF(cmdName);
            arg.toStream(outputStream);
            outputStream.flush();
        } catch(IOException exc){
            // Close the socket, ignoring errors
            try { s.close(); } catch(IOException ignoreexc){}
            throw new AgentConnectionException("Error sending argument: " +
                                               exc.getMessage());
        }
        
        return streamPair;
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
        SocketStreamPair streamPair;

        streamPair = (SocketStreamPair)inStreamPair;

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
            throw new AgentConnectionException("Error reading result: " +
                                               exc.getMessage());
        } finally {
            try { 
                streamPair.getSocket().close(); 
            } catch(IOException ignoreexc){
            }
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
