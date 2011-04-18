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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.OperationNotSupportedException;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.api.BindingHandler;
import org.hyperic.hq.operation.rabbit.api.RoutingRegistry;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent does not have Spring
 * @author Helena Edelson
 */
@Component
public class OperationToRoutingKeyRegistry implements RoutingRegistry {

    private final Log logger = LogFactory.getLog(OperationToRoutingKeyRegistry.class);

    private final Map<String, OperationToRoutingMapping> operationToRoutingMappings = new ConcurrentHashMap<String, OperationToRoutingMapping>();

    private final BindingHandler bindingHandler;

    private static final String SERVER_ROUTING_KEY_PREFIX = "hq.server.";
     
    public static final String AGENT_ROUTING_KEY_PREFIX = "hq.agent.";

    private final String serverId;

    @Autowired
    public OperationToRoutingKeyRegistry(ConnectionFactory connectionFactory) {
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.serverId = getDefaultServerId();
    }

    /**
     * Delegates to the BindingHandler to declare the exchange and queue
     * then bind the them with the binding pattern.
     * Registers the operation by routing mappings
     * @param method the operation to extract and register
     */
    public void register(Method method) throws ChannelException {
        if (supports(method.getName())) return;

        Operation operation = method.getAnnotation(Operation.class);

        if (isValid(operation)) {
            String queueName = bindingHandler.declareAndBind(method.getName(), operation);
            Assert.isTrue(queueName.equalsIgnoreCase(method.getName()));

            OperationToRoutingMapping map = new OperationToRoutingMapping(operation.exchange(), operation.routingKey(), method.getName(), operationHasReturnType(method));
            logger.info("added new routing map=" + map);
            this.operationToRoutingMappings.put(method.getName(), map);
        }
    }

    private boolean operationHasReturnType(Method method) {
        return !void.class.equals(method.getReturnType());
    }

    /* TODO - there is a jira ticket in the backlog */ 
    private boolean isAuthenticatedUser(ConnectionFactory connectionFactory) {
        return !connectionFactory.getUsername().equalsIgnoreCase(MessageConstants.GUEST_USER) && !connectionFactory.getPassword().equalsIgnoreCase(MessageConstants.GUEST_PASS);
    }

    /**
     * @param operationName The operation's name
     * @return org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping
     */
    public OperationToRoutingMapping map(String operationName) {
        if (!supports(operationName)) throw new OperationNotSupportedException(operationName);
        return operationToRoutingMappings.get(operationName);
    }

    private boolean supports(String operationName) {
        return operationToRoutingMappings.containsKey(operationName);
    }

    private boolean isValid(Operation operation) {
        return operation.routingKey() != null && operation.exchange() != null && operation.binding() != null;
    }

    /*
   id = 1302212470776-5028906219606536735-6762208433280624914
   hq-agents.agent-{id}.operations.config.registration.request
   new StringBuilder(SERVER_ROUTING_KEY_PREFIX + this.serverId).append(OPERATION_PREFIX).append(operation).toString());
   new StringBuilder(AGENT_ROUTING_KEY_PREFIX).append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
   */

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
}
