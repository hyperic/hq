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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentCommandsAPI;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentDie_args;
import org.hyperic.hq.agent.commands.AgentDie_result;
import org.hyperic.hq.agent.commands.AgentPing_args;
import org.hyperic.hq.agent.commands.AgentPing_result;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.agent.commands.AgentRestart_args;
import org.hyperic.hq.agent.commands.AgentRestart_result;
import org.hyperic.hq.transport.AgentTransport;
import org.hyperic.util.file.FileWriter;
import org.hyperic.util.math.MathUtil;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * The server-side of the commands the Agent supports.  This object 
 * implements the appropriate interface to plugin to the Agent as
 * an AgentServerHandler.  It provides the server-side to what is
 * called from AgentCommandsClient.
 */

public class AgentCommandsServer 
    implements AgentServerHandler
{
    private AgentCommandsAPI verAPI;
    private Log              log;
    private AgentDaemon      agent;
    private AgentCommandsService agentCommandsService;

    public AgentCommandsServer(){
        this.verAPI = new AgentCommandsAPI();
        this.log    = LogFactory.getLog(this.getClass());
        this.agent  = null;

        this.log.info("Agent commands loaded");
    }

    public AgentAPIInfo getAPIInfo(){
        return this.verAPI;
    }

    public String[] getCommandSet(){
        return AgentCommandsAPI.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream inStream,
                                            OutputStream outStream)
        throws AgentRemoteException 
    {
        if(cmd.equals(AgentCommandsAPI.command_ping)){
            new AgentPing_args(args);  // Just parse the args

            agentCommandsService.ping();
            return new AgentPing_result();
        } else if(cmd.equals(AgentCommandsAPI.command_restart)){
            new AgentRestart_args(args);  // Just parse the args
            agentCommandsService.restart();
            return new AgentRestart_result();
        } else if(cmd.equals(AgentCommandsAPI.command_die)){
            new AgentDie_args(args);  // Just parse the args
            agentCommandsService.die();
            return new AgentDie_result();
        } else if(cmd.equals(AgentCommandsAPI.command_receive_file)){
            AgentReceiveFileData_args aa =
                new AgentReceiveFileData_args(args);
            this.log.error("Received receive file command");
            agentCommandsService.agentSendFileData(aa, inStream);
            return new AgentRemoteValue();
        } else {
            throw new AgentAssertionException("Unknown command '" + cmd + "'");
        }
    }

    public void startup(AgentDaemon agent) throws AgentStartException {
        this.agent = agent;
        
        AgentTransport agentTransport;
        
        try {
            agentTransport = agent.getAgentTransport();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get agent transport: "+
                                            e.getMessage());
        }
        
        agentCommandsService = new AgentCommandsService(agent);
        
        if (agentTransport != null) {
            log.info("Registering Agent Commands Service with Agent Transport");
            
            try {
                agentTransport.registerService(AgentCommandsClient.class, agentCommandsService);
            } catch (Exception e) {
                throw new AgentStartException("Failed to register Agent Commands Service.", e);
            }
        }
        
        this.log.info("Agent commands started up");
    }

    public void shutdown(){
        this.log.info("Agent commands shut down");
    }

}
