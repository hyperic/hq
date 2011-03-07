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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.*;
import org.hyperic.hq.agent.commands.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The set of commands a client can call to a remote agent.  This object
 * provides a specific API which wraps the generic functions which 
 * a remote agent implements.
 */

public class LegacyAgentCommandsClientImpl implements AgentCommandsClient {

    private static final Log logger = LogFactory.getLog(LegacyAgentCommandsClientImpl.class);

    private AgentConnection  agentConn;   
    private AgentCommandsAPI verAPI;

    /**
     * Create the object which communicates over a passed connection.
     *
     * @args agentConn the connection to use when making requests to the agent.
     */
    public LegacyAgentCommandsClientImpl(AgentConnection agentConn){
        this.agentConn = agentConn;
        this.verAPI    = new AgentCommandsAPI();
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping()
    throws AgentRemoteException, AgentConnectionException {
        AgentPing_args args = new AgentPing_args();

        long sendTime = System.currentTimeMillis();
        this.agentConn.sendCommand(AgentCommandsAPI.command_ping,
                                   this.verAPI.getVersion(), args, false);
        long duration = System.currentTimeMillis() - sendTime;
        logger.info(".ping()********* returning " + duration);
        return duration;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart()
        throws AgentRemoteException, AgentConnectionException 
    {
        AgentRestart_args args = new AgentRestart_args();

        this.agentConn.sendCommand(AgentCommandsAPI.command_restart,
                                   this.verAPI.getVersion(), args);
    }    

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die()
        throws AgentRemoteException, AgentConnectionException 
    {
        AgentDie_args args = new AgentDie_args();

        this.agentConn.sendCommand(AgentCommandsAPI.command_die,
                                   this.verAPI.getVersion(), args);
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#agentSendFileData(org.hyperic.hq.agent.FileData[], java.io.InputStream[])
     */
    public FileDataResult[] agentSendFileData(FileData[] destFiles,
                                              InputStream[] streams)
        throws AgentRemoteException, AgentConnectionException
    {
        final String cmd = this.verAPI.command_receive_file;
        AgentReceiveFileData_args args;
        AgentStreamPair sPair;

        if(destFiles.length != streams.length){
            throw new IllegalArgumentException("Streams and dest files " +
                                               "arrays must be the same " +
                                               "length");
        }

        args = new AgentReceiveFileData_args();

        for(int i=0; i < destFiles.length; i++){
            args.addFileData(destFiles[i]);
        }
        
        sPair = this.agentConn.sendCommandHeaders(cmd, 
                                                  this.verAPI.getVersion(), 
                                                  args);
        try {
            FileStreamMultiplexer muxer = new FileStreamMultiplexer();
            FileDataResult[] rs = muxer.sendData(sPair.getOutputStream(), destFiles, streams);
            
            // this is necessary so that remote exceptions are propagated 
            // back to the client
            this.agentConn.getCommandResult(sPair);
            
            return rs;
        } catch(IOException exc){
            throw new AgentRemoteException("IO Exception while sending " +
                                           "file data: " + exc.getMessage());
        } finally {
            // make sure the socket is closed - may not be closed if sendData() 
            // throws an exception
           try {
               sPair.close();
           } catch (IOException e) {
            // swallow
           } 
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public Map upgrade(String tarFile, String destination)
    throws AgentRemoteException, AgentConnectionException {
        // set the arguments to the command
        AgentUpgrade_args args = new AgentUpgrade_args(tarFile, destination);
        AgentRemoteValue cmdRes = this.agentConn.sendCommand(AgentCommandsAPI.command_upgrade, this.verAPI.getVersion(), args);
        AgentUpgrade_result upgradeResult = new AgentUpgrade_result(cmdRes);
        Map result = new HashMap();

        result.put(AgentUpgrade_result.VERSION, upgradeResult.getAgentVersion());
        result.put(AgentUpgrade_result.BUNDLE_NAME, upgradeResult.getAgentBundleName());

        return result;
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle() throws AgentRemoteException, AgentConnectionException {
        
        AgentBundle_args args = new AgentBundle_args();
        
        AgentRemoteValue cmdRes = 
            this.agentConn.sendCommand(AgentCommandsAPI.command_getCurrentAgentBundle, 
                                       this.verAPI.getVersion(), 
                                       args);
        
        return new AgentBundle_result(cmdRes).getCurrentAgentBundle();
    }

    public Map<String, Boolean> agentRemoveFile(Collection<String> files)
    throws AgentRemoteException, AgentConnectionException {
        AgentRemoveFileData_args args = new AgentRemoveFileData_args(files);
        AgentRemoteValue res = 
            agentConn.sendCommand(AgentCommandsAPI.command_remove_file, verAPI.getVersion(), args);
        return new FileRemoval_result(res).getResult();
    }    

}