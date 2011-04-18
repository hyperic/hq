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
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.api.OperationEndpointRegistry;
import org.hyperic.hq.operation.rabbit.api.RoutingRegistry;
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
public class AnnotatedOperationEndpointRegistry implements OperationEndpointRegistry {

    private final Log logger = LogFactory.getLog(AnnotatedOperationEndpointRegistry.class);

    private final Map<String, RabbitMessageListenerContainer> handlers = new ConcurrentHashMap<String, RabbitMessageListenerContainer>();

    protected final RoutingRegistry routingRegistry;

    protected final Converter<Object, String> converter;

    private final ErrorHandler errorHandler;

    private final ConnectionFactory connectionFactory;


    @Autowired
    public AnnotatedOperationEndpointRegistry(ConnectionFactory connectionFactory, RoutingRegistry routingRegistry,
                                              Converter<Object, String> converter, ErrorHandler errorHandler) {
        this.converter = converter;
        this.errorHandler = errorHandler;
        this.connectionFactory = connectionFactory;
        this.routingRegistry = routingRegistry;
    }

    @PreDestroy
    public void destroy() {
        for (Map.Entry<String, RabbitMessageListenerContainer> entry : this.handlers.entrySet()) {
            RabbitMessageListenerContainer mlc = entry.getValue();
            mlc.destroy();
        }
    }

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * Registers dispatchers and delegates to RoutingRegistry for further work.
     * @param method    the method
     * @param candidate the candidate instance
     */
    public void register(Method method, Object candidate) {
        if (this.handlers.containsKey(method.getName())) return;

        RabbitMessageListenerContainer mlc = new RabbitMessageListenerContainer(
                connectionFactory, candidate, method.getName(), this.errorHandler);
        mlc.afterPropertiesSet();
        mlc.start();
        System.out.println(this + " created a listener for " + method.getName() + " and " + candidate);

        this.handlers.put(method.getName(), mlc);
        this.routingRegistry.register(method);
        logger.info("**registered endpoint bean=" + candidate + " and method=" + method.getName());
    }

}
