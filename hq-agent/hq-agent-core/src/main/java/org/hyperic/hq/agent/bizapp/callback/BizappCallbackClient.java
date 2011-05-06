/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.agent.bizapp.callback;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.bizapp.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.*;
import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.convert.JsonObjectMappingConverter;
import org.hyperic.hq.operation.rabbit.core.BindingHandler;
import org.hyperic.hq.operation.rabbit.core.DeclarativeBindingHandler;
import org.hyperic.hq.operation.rabbit.core.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.core.SimpleRabbitTemplate;
import org.hyperic.lather.NullLatherValue;
import org.springframework.stereotype.Component;

@Component
public class BizappCallbackClient extends AgentCallbackClient {
    private final Log logger = LogFactory.getLog(this.getClass());

    private AgentConfig config;

    public BizappCallbackClient() { 
    }

    public BizappCallbackClient(ProviderFetcher fetcher, AgentConfig config) {
        super(fetcher);
        setConfig(config);
    }

    public void setConfig(AgentConfig config) {
        this.config = config;
        configureLather();
    }

    private void configureLather() {
        // configure lather proxy settings
        if (config.isProxyServerSet()) {
            logger.info("Setting proxy server: host=" + config.getProxyIp() + "; port=" + config.getProxyPort());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYHOST, config.getProxyIp());
            System.setProperty(AgentConfig.PROP_LATHER_PROXYPORT, String.valueOf(config.getProxyPort()));
        }
    }

    public void bizappPing() throws AgentCallbackClientException {
        ProviderInfo provider = this.getProvider();
        this.invokeLatherCall(provider, CommandInfo.CMD_PING, NullLatherValue.INSTANCE);
    }

    public boolean userIsValid(String user, String pword) throws AgentCallbackClientException {
        UserIsValid_result res;
        ProviderInfo provider;

        provider = this.getProvider();

        res = (UserIsValid_result) this.invokeLatherCall(provider,
                CommandInfo.CMD_USERISVALID,
                new UserIsValid_args(user, pword));
        return res.isValid();
    }

    /**
     * Register an agent with the server.
     * @param oldAgentToken       The old agent token or <code>null</code> if the agent
     *                            has never been registered before.
     * @param user                The user name for connecting the agent to the server.
     * @param pword               The password for connecting the agent to the server.
     * @param authToken           The authorization token.
     * @param agentIP             The agent IP address.
     * @param agentPort           The agent port where the agent commands services are listening.
     * @param version             The version.
     * @param cpuCount            The host platform cpu count.
     * @param isNewTransportAgent <code>true</code> if the agent is using the new transport layer.
     * @param unidirectional      <code>true</code> if the agent is unidirectional.
     * @return The result containing the new agent token.
     */
    public String registerAgent(String oldAgentToken, String user, String pword, String authToken, String agentIP, int agentPort,
                                               String version, int cpuCount, boolean isNewTransportAgent, boolean unidirectional) throws AgentCallbackClientException {

        RegisterAgentRequest registerAgent;
        if (oldAgentToken != null) {
            registerAgent = new RegisterAgentRequest(oldAgentToken, authToken, version, cpuCount, agentIP, agentPort, user, pword, unidirectional);
        } else {
            registerAgent = new RegisterAgentRequest(null, authToken, version, cpuCount, agentIP, agentPort, user, pword, unidirectional);
        }


        /* Finish spring on agent to have this handled by the automated framework */
        ConnectionFactory cf = new ConnectionFactory();
        BindingHandler bindingHandler = new DeclarativeBindingHandler(cf);
        RabbitTemplate rabbitTemplate = new SimpleRabbitTemplate(cf, new JsonObjectMappingConverter(), RegisterAgentResponse.class);

        bindingHandler.declareAndBind("registerAgentRequest", "to.server", "request.*");
        bindingHandler.declareAndBind("testAgent", "to.agent", "response.*");
   
        RegisterAgentResponse response = (RegisterAgentResponse) rabbitTemplate.publishAndReceive("testAgent", "to.server", "request.register", registerAgent, null);

        logger.info("\nagent received=" + response + " with token=" + response.getAgentToken());

        return response.getAgentToken();
    }


    public String updateAgent(String agentToken, String user, String pword,
                              String agentIp, int agentPort,
                              boolean isNewTransportAgent,
                              boolean unidirectional)
            throws AgentCallbackClientException {
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

        res = (UpdateAgent_result) this.invokeLatherCall(provider,
                CommandInfo.CMD_UPDATE_AGENT,
                args);
        return res.getErrMsg();
    }
}
