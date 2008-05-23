/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;

/**
 * A callback class that notifies the starting entity (most likely the agent 
 * client) as to the startup status of the agent (success/failure).
 */
public class AgentStartupCallback {
    
    private final Log _log = LogFactory.getLog(AgentStartupCallback.class);  
    
    private final Socket _startupSock;
    
    /**
     * Creates an instance.
     *
     * @throws AgentConfigException  if the "camUpPort" is not specified.
     * @throws IOException if the callback fails to establish a connection 
     *                      to the starting entity.
     */
    public AgentStartupCallback() throws AgentConfigException, IOException {
        String startupPort = System.getProperty(CommandsAPIInfo.PROP_UP_PORT);
        
        if(startupPort == null){
            throw new AgentConfigException("Failure to find startup " +
                                           "reporting port in sys properties");
        } else {
            int sPort = Integer.parseInt(startupPort);
            
            try {
                _startupSock = new Socket("127.0.0.1", sPort);
            } catch(IOException exc){
                _log.error("Failed to connect to startup port ("+sPort+")", exc);
                
                throw new IOException("Failed to connect to startup " +
                                      "port (" + sPort + "): " +
                                      exc.getMessage());
            }
        }        
    }
    
    /**
     * Notify the starting entity.
     * 
     * @param succeeded <code>true</code> if the agent startup succeeded; 
     *                  <code>false</code> if the agent startup failed.
     */
    public void onAgentStartup(boolean succeeded) {
        if (succeeded) {        
            writeStartupState(1);
        } else {
            writeStartupState(0);
        }
    }
    
    /**
     * Clean up resources just in case!
     */
    protected void finalize() throws Throwable {
        try {
            _startupSock.close();
        } catch (Exception e) {
        }
    }
    
    private void writeStartupState(int state) {
        try {
            DataOutputStream dOs = 
                new DataOutputStream(_startupSock.getOutputStream());
            dOs.writeInt(state);
            dOs.flush();
        } catch(IOException exc){
            _log.error("Error writing startup state to startup port: "+
                           exc.getMessage());
        } finally {
            try {_startupSock.close();} catch(IOException iexc){}
        }        
    }

}
