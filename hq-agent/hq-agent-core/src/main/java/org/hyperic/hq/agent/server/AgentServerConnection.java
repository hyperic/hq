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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteValue;


/**
 * This class represents an incoming connection to the agent.
 * AgentConnectionListener objects return these when incoming
 * connections are received.
 */
public abstract class AgentServerConnection {
    /**
     * Get a stream which can be used to read data from the connection.
     */
    public abstract InputStream getInputStream()
       throws AgentConnectionException;

    /**
     * Get a stream which can be used to write data to the connection.
     */
    public abstract OutputStream getOutputStream()
       throws AgentConnectionException;

    /**
     * Close the connection.
     */
    public abstract void close();
    
    /**
     * Read the command invocation information from the client
     */
    public AgentCommand readCommand()
        throws AgentConnectionException, EOFException
    {
        DataInputStream dIs;
        AgentRemoteValue cmdArg;
        String cmd;
        int cmdVersion, agentVersion;
        try {
            dIs          = new DataInputStream(getInputStream());
            agentVersion = dIs.readInt();
            cmdVersion   = dIs.readInt();
            cmd          = dIs.readUTF();
            cmdArg       = AgentRemoteValue.fromStream(dIs);
        } catch(EOFException exc){
            throw exc;
        } catch(IOException exc){
            throw new AgentConnectionException("Unable to read command: " + exc.getMessage(), exc);
        }
        return new AgentCommand(agentVersion, cmdVersion, cmd, cmdArg);
    }

    public void sendErrorResponse(String msg)
        throws AgentConnectionException
    {
        DataOutputStream dOs;

        try {
            dOs = new DataOutputStream(this.getOutputStream());

            dOs.writeInt(1);
            dOs.writeUTF(msg);
        } catch(IOException exc){
            throw new AgentConnectionException("Unable to send error: " +
                                               exc.getMessage());
        }
    }


    public void sendSuccessResponse(AgentRemoteValue res)
        throws AgentConnectionException
    {
        DataOutputStream dOs;

        try {
            dOs = new DataOutputStream(this.getOutputStream());
            
            dOs.writeInt(0);
            res.toStream(dOs);
        } catch(IOException exc){
            throw new AgentConnectionException("Unable to send response: " +
                                               exc.getMessage(), exc);
        }
    }
}
