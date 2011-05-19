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
import org.hyperic.hq.operation.OperationRegistrationException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationRegistry implements OperationRegistry {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Map<String, RabbitMessageListener> endpoints = new ConcurrentHashMap<String, RabbitMessageListener>();

    private final Map<String, Object> dispatchers = new ConcurrentHashMap<String, Object>();

    private final RoutingRegistry registry;

    private final ErrorHandler errorHandler;

    private final ConnectionFactory connectionFactory;

    @Autowired
    public AnnotatedOperationRegistry(ConnectionFactory connectionFactory, RoutingRegistry registry,
                                      ErrorHandler errorHandler) {
        this.connectionFactory = connectionFactory;
        this.registry = registry;
        this.errorHandler = errorHandler;
    }


    @PreDestroy
    public void destroy() throws Exception {
        for (Map.Entry<String, RabbitMessageListener> entry : endpoints.entrySet()) {
            entry.getValue().stop();
        }
    }

    /**
     * Registers an candidate and its annotated operation methods.
     * Re-validate because initial validation is done in load-time by the discoverer,
     * however we want to support runtime registration in which case validation
     * must exist here.
     * @param method    The method
     * @param candidate The instance to invoke the method on
     * @throws OperationRegistrationException if the operation is already registered
     *                                        which will alert the developer to change the candidate method name to a unique one
     */
    public void register(Method method, Object candidate) throws OperationRegistrationException {

        if (method.isAnnotationPresent(OperationEndpoint.class)) {
            validateRoutingsSupported(method, method.getAnnotation(OperationEndpoint.class).getClass());
            validateParameterTypes(method, candidate);
            validateSupported(method.getName(), endpoints);
            /*if (!method.getAnnotation(OperationEndpoint.class).responseBinding().isEmpty())
                validateReturnType(method, candidate);  */
            registry.register(method);
            endpoints.put(method.getName(), new RabbitMessageListener(connectionFactory, candidate, method, errorHandler));
            logger.info(String.format("Registered '%s' on '%s'", method.getAnnotation(OperationEndpoint.class), candidate.getClass()));
        }
        else if (method.isAnnotationPresent(OperationDispatcher.class)) {
            validateRoutingsSupported(method, method.getAnnotation(OperationDispatcher.class).getClass());
            validateReturnType(method, candidate);
            validateSupported(method.getName(), dispatchers);
            registry.register(method);
            dispatchers.put(method.getName(), candidate);
            logger.info(String.format("Registered '%s' on '%s'",method.getAnnotation(OperationDispatcher.class), candidate.getClass()));
        }
    }

    private void validateSupported(String operation, Map mappings) {
        if (mappings.containsKey(operation)) throw new OperationRegistrationException(operation);
    }

    private void validateRoutingsSupported(Method method, Class<? extends Annotation> annotation) throws OperationRegistrationException {
        if ((registry.supports(method.getName(), annotation)))
            throw new OperationRegistrationException(
                    String.format("Illegal method name: '%s' already supports an '%s' operation.", method, annotation.getSimpleName()));
    }


    private void validateParameterTypes(Method method, Object candidate) throws OperationRegistrationException {
        if (method.getParameterTypes().length != 1) throw new OperationRegistrationException(
                String.format("Illegal operation: method '%s' on '%s' must have one parameter.", method, candidate));
    }


    private void validateReturnType(Method method, Object candidate) throws OperationRegistrationException {
        if (void.class.equals(method.getReturnType())) throw new OperationRegistrationException(
                String.format("Illegal operation: method '%s' on '%s' must have a non-void return type.", method, candidate));
    }


}
