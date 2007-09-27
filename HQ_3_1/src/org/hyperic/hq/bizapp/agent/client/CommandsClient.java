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

package org.hyperic.hq.bizapp.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.commands.CreateToken_args;
import org.hyperic.hq.bizapp.agent.commands.CreateToken_result;
import org.hyperic.hq.bizapp.agent.commands.GetServer_args;
import org.hyperic.hq.bizapp.agent.commands.GetServer_result;
import org.hyperic.hq.bizapp.agent.commands.SetServer_args;
import org.hyperic.hq.bizapp.agent.commands.SetServer_result;

public class CommandsClient {
    private AgentConnection agentConn;
    private CommandsAPIInfo  verAPI;

    public CommandsClient(AgentConnection agentConn){
        this.agentConn = agentConn;
        this.verAPI    = new CommandsAPIInfo();
    }


    public CreateToken_result createToken(CreateToken_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        AgentRemoteValue rval;

        rval = this.agentConn.sendCommand(this.verAPI.command_createToken,
                                          this.verAPI.getVersion(), args);
        return new CreateToken_result(rval);
    }        

    public ProviderInfo getProviderInfo()
        throws AgentRemoteException, AgentConnectionException
    {
        GetServer_result res;
        GetServer_args args;
        AgentRemoteValue rval;

        args = new GetServer_args();
        rval = this.agentConn.sendCommand(this.verAPI.command_getServer,
                                          this.verAPI.getVersion(), args);
        res = new GetServer_result(rval);
        return res.getProvider();
    }        

    public void setProviderInfo(ProviderInfo provider)
        throws AgentRemoteException, AgentConnectionException
    {
        SetServer_result res;
        SetServer_args args;
        AgentRemoteValue rval;

        args = new SetServer_args();
        args.setProvider(provider);
        rval = this.agentConn.sendCommand(this.verAPI.command_setServer,
                                          this.verAPI.getVersion(), args);

        // Do this purely for the future, in case we do validation here
        res = new SetServer_result(rval);
    }        
}
