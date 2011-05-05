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

package org.hyperic.hq.agent.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.bizapp.callback.AgentCallbackClientException;
import org.hyperic.hq.agent.bizapp.callback.BizappCallbackClient;
import org.hyperic.hq.agent.bizapp.callback.StaticProviderFetcher;
import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.core.BindingHandler;
import org.hyperic.hq.operation.rabbit.core.RabbitTemplate;

/**
 * @author Helena Edelson
 */
//@OperationService
//@Service
public class AnnotatedOperationServiceCallback implements OperationServiceCallback {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final RabbitTemplate rabbitTemplate;

    private final BindingHandler bindingHandler;
     
    private BizappCallbackClient bizappCallback;

    private StaticProviderFetcher providerFetcher;

    private AgentConfig agentConfig;

    //@Autowired
    public AnnotatedOperationServiceCallback(BindingHandler bindingHandler, RabbitTemplate rabbitTemplate) {
        this.bindingHandler = bindingHandler; //new DeclarativeBindingHandler(connectionFactory);
        this.rabbitTemplate = rabbitTemplate; //new SimpleRabbitTemplate(connectionFactory, RegisterAgentResponse.class);
    }
  
    public boolean userIsValid(String username, String password) throws AgentCallbackClientException {
        return bizappCallback.userIsValid(username, password);
    }

    /**
     * @see org.hyperic.hq.agent.bizapp.callback.BizappCallbackClient
     */
    //@OperationDispatcher(exchange = "to.server", routingKey = "request.register", binding = "request.*", queue = "registerAgentRequest")
    //@OperationEndpoint(exchange = "to.agent", routingKey = "response.register", binding = "response.*", queue = "agent")
    public RegisterAgentResponse registerAgent(String oldAgentToken, String user, String pass, String authToken, String address, int port,
                                               String version, int cpuCount, boolean isNewTransportAgent, boolean unidirectional) throws AgentCallbackClientException {

        RegisterAgentRequest registerAgent = oldAgentToken != null
            ? new RegisterAgentRequest(oldAgentToken, authToken, version, cpuCount, address, port, user, pass, unidirectional)
                : new RegisterAgentRequest(null, authToken, version, cpuCount, address, port, user, pass, unidirectional);

        bindingHandler.declareAndBind("registerAgentRequest", "to.server", "request.*");
        bindingHandler.declareAndBind("testAgent", "to.agent", "response.*");

        RegisterAgentResponse response = (RegisterAgentResponse) rabbitTemplate.publishAndReceive("testAgent", "to.server", "request.register", registerAgent, null);

        logger.info("\nagent received=" + response + " with token=" + response.getAgentToken());

        return response;
    }

    /**
     * @see org.hyperic.hq.agent.bizapp.callback.BizappCallbackClient
     */
    //@OperationDispatcher(exchange = "to.server", routingKey = "request.register", binding = "request.*", queue = "registerAgentRequest")
    public String updateAgent(String token, String user, String pass, String address, int port, boolean isNewTransport, boolean unidirectional) throws AgentCallbackClientException {
       return bizappCallback.updateAgent(token, user, pass, address, port, isNewTransport, unidirectional);
    }
}
