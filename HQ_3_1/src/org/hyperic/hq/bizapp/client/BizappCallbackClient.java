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

package org.hyperic.hq.bizapp.client;

import org.hyperic.lather.NullLatherValue;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_args;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_result;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_args;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_result;

public class BizappCallbackClient 
    extends AgentCallbackClient
{
    public BizappCallbackClient(ProviderFetcher fetcher){
        super(fetcher);
    }

    public void bizappPing()
        throws AgentCallbackClientException
    {
        ProviderInfo provider;

        provider = this.getProvider();
        this.invokeLatherCall(provider, CommandInfo.CMD_PING, 
                              NullLatherValue.INSTANCE);
    }

    public boolean userIsValid(String user, String pword)
        throws AgentCallbackClientException
    {
        UserIsValid_result res;
        ProviderInfo provider;

        provider = this.getProvider();

        res = (UserIsValid_result)this.invokeLatherCall(provider, 
                                            CommandInfo.CMD_USERISVALID,
                                            new UserIsValid_args(user, pword));
        return res.isValid();
    }

    public RegisterAgentResult registerAgent(String user, String pword, 
                                             String authToken,
                                             String agentIP, int agentPort,
                                             String version,
                                             int cpuCount)
        throws AgentCallbackClientException
    {
        RegisterAgent_result res;
        RegisterAgent_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new RegisterAgent_args();
        args.setUser(user);
        args.setPword(pword);
        args.setAuthToken(authToken);
        args.setAgentIP(agentIP);
        args.setAgentPort(agentPort);
        args.setVersion(version);
        args.setCpuCount(cpuCount);

        res = (RegisterAgent_result)this.invokeLatherCall(provider,
                                                CommandInfo.CMD_REGISTER_AGENT,
                                                args);
        return new RegisterAgentResult(res.getResult());
    }

    public String updateAgent(String agentToken, String user, String pword,
                              String agentIp, int agentPort)
        throws AgentCallbackClientException
    {
        UpdateAgent_result res;
        UpdateAgent_args args;
        ProviderInfo provider;
        
        provider = this.getProvider();

        args = new UpdateAgent_args();
        args.setUser(user);
        args.setPword(pword);
        args.setAgentIP(agentIp);
        args.setAgentPort(agentPort);
        args.setAgentToken(agentToken);

        res = (UpdateAgent_result)this.invokeLatherCall(provider,
                                                CommandInfo.CMD_UPDATE_AGENT,
                                                args);
        return res.getErrMsg();
    }
}
