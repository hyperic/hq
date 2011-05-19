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
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent does not have Spring
 * @author Helena Edelson
 */
@Component
public class OperationToRoutingRegistry implements RoutingRegistry {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Map<String, OperationToRoutingMapping> operationToRoutingMappings = new ConcurrentHashMap<String, OperationToRoutingMapping>();

    private final BindingHandler bindingHandler;


    @Autowired
    public OperationToRoutingRegistry(ConnectionFactory connectionFactory) {
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
    }

    @PostConstruct
    public void initialize() {
        bindingHandler.declareExchange(Routing.EXCHANGE_REQUEST);
        bindingHandler.declareExchange(Routing.EXCHANGE_RESPONSE);
        bindingHandler.declareExchange(Routing.EXCHANGE_REQUEST_SECURE);
        bindingHandler.declareExchange(Routing.EXCHANGE_RESPONSE_SECURE);
        bindingHandler.declareExchange(Routing.EXCHANGE_ERRORS);
    }

    /**
     * Delegates to the BindingHandler then registers the operation by routing mappings
     * NOTE: this is still in prototype phase
     * @param method the operation to extract and register
     */
    public void register(Method method) {
        if (method.isAnnotationPresent(OperationDispatcher.class)) {
            registerDispatcher(method);
        } else if (method.isAnnotationPresent(OperationEndpoint.class)) {
            registerEndpoint(method);
        }
    }

    private void registerEndpoint(Method method) {
        OperationEndpoint endpoint = method.getAnnotation(OperationEndpoint.class);

        if (!endpoint.request().isEmpty()) {
            if (!endpoint.request().equals(Routing.EXCHANGE_REQUEST)) {
                bindingHandler.declareExchange(endpoint.request());
            }

            if (!endpoint.requestBinding().isEmpty()) {
                bindingHandler.declareAndBind(method.getName(), endpoint.request(), endpoint.requestBinding());
                addMapping(method, createMapping(method.getName(), endpoint.request(),
                        endpoint.routing(), method.getReturnType(), OperationEndpoint.class));
            }
        }

        if (!endpoint.response().isEmpty()) {
            if (!endpoint.response().equals(Routing.EXCHANGE_RESPONSE)) {
                bindingHandler.declareExchange(endpoint.response());
            }

            if (!endpoint.responseBinding().isEmpty()) {
                if (void.class.equals(method.getReturnType())) {
                    bindingHandler.declareAndBind(method.getName(), endpoint.response(), endpoint.responseBinding());
                    addMapping(method, createMapping(method.getName(),
                            endpoint.response(), endpoint.routing(), method.getParameterTypes()[0], OperationEndpoint.class));
                } else {
                    addMapping(method, createMapping(method.getName(),
                            endpoint.response(), endpoint.routing(), method.getReturnType(), OperationEndpoint.class));
                }
            }
        }

        if (!endpoint.source().isEmpty() && !endpoint.destination().isEmpty()) {
            //next decoupling phase: bindingHandler.declareExchangesAndBind(destination, source, binding);
        }
    }

    private void registerDispatcher(Method method) {
        OperationDispatcher dispatcher = method.getAnnotation(OperationDispatcher.class);
        if (!dispatcher.request().isEmpty() && !dispatcher.request().equals(Routing.EXCHANGE_REQUEST)) {
            bindingHandler.declareExchange(dispatcher.request());
        }

        addMapping(method, createMapping(method.getName(), dispatcher.request(), dispatcher.routing(), method.getReturnType(), OperationDispatcher.class));
    }

    private void addMapping(Method method, OperationToRoutingMapping mapping) {
        operationToRoutingMappings.put(method.getName(), mapping);
        logger.debug(String.format("Registered mapping [%s] for bean %s. Current total of %s routing mappings",
                mapping, method.getDeclaringClass().getClass().getName(), operationToRoutingMappings.size()));
    }

    private OperationToRoutingMapping createMapping(String queue, String exchange, String routing, Class methodReturnType, Class<? extends Annotation> type) {
        return new OperationToRoutingMapping(queue, exchange, routing, methodReturnType, type);
    }


    /**
     * @param operationName The operation name
     * @return org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping
     */
    public OperationToRoutingMapping map(String operationName, Class<? extends Annotation> annotation) {
        if (!supports(operationName, annotation)) throw new OperationNotSupportedException(operationName);
        return operationToRoutingMappings.get(operationName); //createKey(operationName, annotation)
    }

    public boolean supports(String operationName, Class<? extends Annotation> annotation) {
        return operationToRoutingMappings.containsKey(operationName);//createKey(operationName, annotation)
    }

    private String createKey(String operationName, Class<? extends Annotation> annotation) {
        return operationName + annotation.getSimpleName();
    }


    /* The following is related to a JIRA ticket in the backlog - work not started */

    private boolean isAuthenticatedUser(ConnectionFactory connectionFactory) {
        return !connectionFactory.getUsername().equalsIgnoreCase(MessageConstants.GUEST_USER)
                && !connectionFactory.getPassword().equalsIgnoreCase(MessageConstants.GUEST_PASS);
    }
}
