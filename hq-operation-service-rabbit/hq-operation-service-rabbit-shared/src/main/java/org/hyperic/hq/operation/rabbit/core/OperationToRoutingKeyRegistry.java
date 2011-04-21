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
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
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
    /* TODO */
    private static final String SERVER_PREFIX = "hq.server.";
    public static final String AGENT_PREFIX = "hq.agent.";

    private final String serverId;

    @Autowired
    public OperationToRoutingKeyRegistry(ConnectionFactory connectionFactory) {
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.serverId = getDefaultServerId();
    }

    /**
     * Delegates to the BindingHandler then registers the operation by routing mappings
     * @param method the operation to extract and register
     */
    public void register(Method method) {
        if (method.isAnnotationPresent(OperationDispatcher.class)) {
            OperationDispatcher dispatcher = method.getAnnotation(OperationDispatcher.class);
            if (!supports(method.getName(), dispatcher.getClass())) {
                register(method, extractQueueName(method), dispatcher.exchange(), dispatcher.routingKey(), dispatcher.binding(), dispatcher.getClass());
            }
        }
        if (method.isAnnotationPresent(OperationEndpoint.class)) {
            OperationEndpoint endpoint = method.getAnnotation(OperationEndpoint.class);
            if (!supports(method.getName(), endpoint.getClass())) {
                register(method, extractQueueName(method), endpoint.exchange(), endpoint.routingKey(), endpoint.binding(), endpoint.getClass());
            }
        }
    }

    /**
     * Delegates to the BindingHandler then registers the operation by routing mappings
     * @param method     the operation to extract and register
     * @param queue      the queue name - if this is set, override the default method name as queue name
     * @param exchange   the exchange name to declare an exchange
     * @param routingKey the routing key for sending
     * @param binding    the pattern to bind a queue to exchange
     * @param annotation the annotation type
     */
    private void register(Method method, String queue, String exchange, String routingKey, String binding, Class<? extends Annotation> annotation) {
        bindingHandler.declareAndBind(queue, exchange, binding);

        operationToRoutingMappings.put(method.getName(), new OperationToRoutingMapping(
            queue, exchange, routingKey, method.getReturnType(), annotation));

        logger.debug("registered method + " + method.getName() + " on bean type=" + method.getDeclaringClass());
    }
 
    public String extractQueueName(Method method) {
        if (method.isAnnotationPresent(OperationDispatcher.class)) {
            OperationDispatcher dispatcher = method.getAnnotation(OperationDispatcher.class);
            return hasValue(dispatcher.queue()) ? dispatcher.queue() : method.getName();

        }
        else if (method.isAnnotationPresent(OperationEndpoint.class)) {
            OperationEndpoint endpoint = method.getAnnotation(OperationEndpoint.class);
            return hasValue(endpoint.queue()) ? endpoint.queue() : method.getName();
        }
        return method.getName();
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
 
    private String createKey(String operationName, Class<? extends Annotation> annotation) {
        return operationName + annotation.getSimpleName();
    }

    private boolean hasValue(String entry) {
        return entry != null && entry.length() > 0;
    }

    /* The following is related to a JIRA ticket in the backlog - work not started */
    private boolean isAuthenticatedUser(ConnectionFactory connectionFactory) {
        return !connectionFactory.getUsername().equalsIgnoreCase(MessageConstants.GUEST_USER)
                && !connectionFactory.getPassword().equalsIgnoreCase(MessageConstants.GUEST_PASS);
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
            return Thread.currentThread().getName(); // TODO
        }
    }
}
