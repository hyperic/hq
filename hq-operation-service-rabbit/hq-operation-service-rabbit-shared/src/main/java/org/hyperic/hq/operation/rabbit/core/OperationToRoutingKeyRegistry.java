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
package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.OperationNotSupportedException;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hyperic.hq.operation.rabbit.util.BindingPatternConstants.OPERATION_PREFIX;

/**
 * Agent does not have Spring
 * @author Helena Edelson
 */
@Component
public class OperationToRoutingKeyRegistry implements RoutingRegistry {

    private final Map<String, OperationToRoutingMapping> operationToRoutingKeyMappings = new ConcurrentHashMap<String, OperationToRoutingMapping>();

    private final Map<String, SimpleMessageListenerContainer> handlerMappings = new ConcurrentHashMap<String, SimpleMessageListenerContainer>();

    private final BindingHandler bindingHandler;

    private final String serverId;

    @Autowired
    public OperationToRoutingKeyRegistry(ConnectionFactory connectionFactory) {  
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.serverId = getDefaultServerId();
    }

    /**
     * Registers operation to routing key and exchange mappings
     * @param operation the operation to extract and register
     */
    public void register(Operation operation) throws ChannelException {
        if (!this.operationToRoutingKeyMappings.containsKey(operation.operationName())) {
            String queueName = this.bindingHandler.declareAndBind(operation);

            this.operationToRoutingKeyMappings.put(operation.operationName(),
                    new OperationToRoutingMapping(operation.exchangeName(), operation.value(), queueName, null));
        }
    }

    /*public void registerHandler() { 
        MessageListenerAdapter mla = new MessageListenerAdapter();
        mla.setDefaultListenerMethod("registerAgentRequest");
        mla.setDelegate(registerAgentService);
        mla.setResponseExchange(Constants.TO_SERVER_AUTHENTICATED_EXCHANGE);
        mla.setResponseRoutingKey(Constants.OPERATION_NAME_AGENT_REGISTER_RESPONSE);

        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer(new SingleConnectionFactory());
        listener.setMessageListener(mla);
        listener.setQueueName("hq-agents.config");
    }*/
 
    /**
     * Automatically handled by spring
     * @return
     */
    public List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();
        for (String operation : Constants.SERVER_OPERATIONS) {
            keys.add(new StringBuilder(Constants.SERVER_ROUTING_KEY_PREFIX + this.serverId).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    /** 
     * @param operationName The operation's name
     * @return
     */
    public OperationToRoutingMapping map(String operationName) {
        if (!this.operationToRoutingKeyMappings.containsKey(operationName)) throw new OperationNotSupportedException(operationName);
        return this.operationToRoutingKeyMappings.get(operationName);
    }

    public SimpleMessageListenerContainer mapListener(String operationName) {
        if (!this.handlerMappings.containsKey(operationName)) throw new OperationNotSupportedException(operationName);
        return this.handlerMappings.get(operationName);
    }
 
    /**
     * agentToken = 1302212470776-5028906219606536735-6762208433280624914
     * hq-agents.agent-{agentToken}.operations.config.registration.request
     * @param agentToken
     * @return
     */
    public List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();
        for (String operation : Constants.AGENT_OPERATIONS) {
            keys.add(new StringBuilder(Constants.AGENT_ROUTING_KEY_PREFIX)
                    .append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    /**
     * Returns the IP address as a String. If an error occurs getting
     * the host IP, a random UUID as String is used.
     * @return the IP address string in textual presentation
     */
    private static String getDefaultServerId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return UUID.randomUUID().toString();
        }
    }

    public Map<String, SimpleMessageListenerContainer> getHandlerMappings() {
        return handlerMappings;
    }

    private boolean validArguments(String operationName, String exchangeName, String value) {
        return operationName == null || exchangeName == null || value == null;
    }

}
