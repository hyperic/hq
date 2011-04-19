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
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.api.BindingHandler;
import org.hyperic.hq.operation.rabbit.api.RoutingRegistry;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
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

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Map<String, OperationToRoutingMapping> operationToRoutingMappings = new ConcurrentHashMap<String, OperationToRoutingMapping>();

    private final BindingHandler bindingHandler;

    private static final String SERVER_PREFIX = "hq.server.";

    public static final String AGENT_PREFIX = "hq.agent.";

    private final String serverId;

    @Autowired
    public OperationToRoutingKeyRegistry(ConnectionFactory connectionFactory) {
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.serverId = getDefaultServerId();
    }

    /**
     * Delegates to the BindingHandler and registers the operation by routing mappings
     * @param method   the operation to extract and register
     * @param endpoint the @OperationEndpoint
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     *
     */
    public void register(Method method, OperationEndpoint endpoint) throws ChannelException {
        if (supports(method.getName(), endpoint.getClass())) return;
        register(method, endpoint.exchange(), endpoint.routingKey(), endpoint.binding(), endpoint.getClass());
    }

    /**
     * Delegates to the BindingHandler and registers the operation by routing mappings
     * @param method     the operation to extract and register
     * @param dispatcher the @OperationDispatcher
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     *
     */
    public void register(Method method, OperationDispatcher dispatcher) throws ChannelException {
        if (supports(method.getName(), dispatcher.getClass())) return;
        register(method, dispatcher.exchange(), dispatcher.routingKey(), dispatcher.binding(), dispatcher.getClass());
    }


    private void register(Method method, String exchange, String routingKey, String binding, Class<? extends Annotation> annotation) {
        bindingHandler.declareAndBind(method.getName(), exchange, binding);

        operationToRoutingMappings.put(method.getName(),
                new OperationToRoutingMapping(method.getName(), exchange, routingKey, method.getReturnType(), annotation));
    }


    /**
     * @param operationName The operation's name
     * @return org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping
     */
    public OperationToRoutingMapping map(String operationName, Class<? extends Annotation> annotation) {
        if (!supports(operationName, annotation)) throw new OperationNotSupportedException(operationName);
        return operationToRoutingMappings.get(createKey(operationName, annotation));
    }

    public boolean supports(String operationName, Class<? extends Annotation> annotation) {
        return operationToRoutingMappings.containsKey(createKey(operationName, annotation));
    }

    /**
     * Create the routing registry key name
     */
    private String createKey(String operationName, Class<? extends Annotation> annotation) {
        return operationName + annotation.getSimpleName();
    }


    /* JIRA ticket in the backlog */

    private boolean isAuthenticatedUser(ConnectionFactory connectionFactory) {
        return !connectionFactory.getUsername().equalsIgnoreCase(MessageConstants.GUEST_USER)
                && !connectionFactory.getPassword().equalsIgnoreCase(MessageConstants.GUEST_PASS);
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
