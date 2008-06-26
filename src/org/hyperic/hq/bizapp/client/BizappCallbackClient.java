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
import org.hyperic.hq.agent.AgentConfig;
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
    public BizappCallbackClient(ProviderFetcher fetcher, AgentConfig config){
        super(fetcher);
        
        // configure lather proxy settings
        if (config.isProxyServerSet()) {
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST, 
                               config.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT, 
                               String.valueOf(config.getProxyPort()));
        }
        
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

    /**
     * Register an agent with the server.
     * 
     * @param oldAgentToken The old agent token or <code>null</code> if the agent 
     *                      has never been registered before.
     * @param user The user name for connecting the agent to the server.
     * @param pword The password for connecting the agent to the server.
     * @param authToken The authorization token.
     * @param agentIP The agent IP address.
     * @param agentPort The agent port where the agent commands services are listening.
     * @param version The version.
     * @param cpuCount The host platform cpu count.
     * @param isNewTransportAgent <code>true</code> if the agent is using the new transport layer.
     * @param unidirectional <code>true</code> if the agent is unidirectional.
     * @return The result containing the new agent token.
     */
    public RegisterAgentResult registerAgent(String oldAgentToken, 
                                             String user, String pword, 
                                             String authToken,
                                             String agentIP, int agentPort,
                                             String version,
                                             int cpuCount, 
                                             boolean isNewTransportAgent, 
                                             boolean unidirectional)
        throws AgentCallbackClientException
    {
        RegisterAgent_result res;
        RegisterAgent_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new RegisterAgent_args();
        args.setUser(user);
        args.setPword(pword);
        
        if (oldAgentToken != null) {
            args.setAgentToken(oldAgentToken);    
        }

        args.setAuthToken(authToken);
        args.setAgentIP(agentIP);
        args.setAgentPort(agentPort);
        args.setVersion(version);
        args.setCpuCount(cpuCount);
        
        if (isNewTransportAgent) {
            args.setNewTransportAgent(unidirectional);            
        }

        res = (RegisterAgent_result)this.invokeLatherCall(provider,
                                                CommandInfo.CMD_REGISTER_AGENT,
                                                args);
        return new RegisterAgentResult(res.getResult());
    }

    public String updateAgent(String agentToken, String user, String pword,
                              String agentIp, int agentPort, 
                              boolean isNewTransportAgent, 
                              boolean unidirectional)
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
        
        if (isNewTransportAgent) {
            args.setNewTransportAgent(unidirectional);
        }

        res = (UpdateAgent_result)this.invokeLatherCall(provider,
                                                CommandInfo.CMD_UPDATE_AGENT,
                                                args);
        return res.getErrMsg();
    }
}
