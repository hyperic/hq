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
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationRegistry implements OperationRegistry {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Map<String, RabbitMessageListener> handlers = new ConcurrentHashMap<String, RabbitMessageListener>();

    private final RoutingRegistry routingRegistry;

    private final ErrorHandler errorHandler;

    private final ConnectionFactory connectionFactory;

    @Autowired
    public AnnotatedOperationRegistry(ConnectionFactory connectionFactory,
                                      RoutingRegistry routingRegistry, ErrorHandler errorHandler) {
        this.connectionFactory = connectionFactory;
        this.routingRegistry = routingRegistry;
        this.errorHandler = errorHandler;
    }

    /**
     * Registers an candidate and its annotated operation methods
     * @param method  The method
     * @param candidate The instance to invoke the method on
     * @throws OperationDiscoveryException if an exception occurs
     */
    public void register(Method method, Object candidate) throws OperationDiscoveryException {
        routingRegistry.register(method);

        if (method.isAnnotationPresent(OperationEndpoint.class)) { 
            handlers.put(method.getName(), new RabbitMessageListener(connectionFactory, candidate, method, errorHandler));
        }
    }
 
    @PreDestroy
    public void destroy() throws Exception {
        for (Map.Entry<String, RabbitMessageListener> entry : handlers.entrySet()) {
            entry.getValue().stop();
        }
    }
}
