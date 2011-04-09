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
import org.hyperic.hq.operation.*;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.util.OperationRouting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Detects and maps operations and operation dispatchers to the messaging system
 * while keeping them independent of each other. If candidates are valid, registers them.
 * @author Helena Edelson
 */
public class AnnotatedOperationDispatcherDiscoverer extends AbstractMethodInvokingRegistry implements Dispatcher, OperationDiscoverer {

    public AnnotatedOperationDispatcherDiscoverer() {
        super(new SingleConnectionFactory());
    }

    public AnnotatedOperationDispatcherDiscoverer(ConnectionFactory connectionFactory, Converter<Object, String> converter, RoutingRegistry routingRegistry) {
        super(connectionFactory, converter, routingRegistry);
    }

    /**
     * Passes the candidate instance along with the marker annotation
     * @param candidate The candidate instance which can be a dispatcher or endpoint
     */
    public void discover(Object candidate) {
        super.discover(candidate, OperationDispatcher.class);
    }

    /**
     * Hand-off point from Hyperic to Rabbit
     * @param envelope
     */
    public void dispatch(Envelope envelope) {
        if (!this.operationMappings.containsKey(envelope.getOperationName()))
            throw new OperationNotSupportedException(envelope.getOperationName());

        MethodInvoker methodInvoker = this.operationMappings.get(envelope.getOperationName());

        OperationRouting routing = this.routingRegistry.map(envelope.getOperationName());

        Object data = null;

        try {
            data = methodInvoker.invoke(envelope.getContext());
        } catch (IllegalAccessException e) {
            //logger.error("", e);
        } catch (InvocationTargetException e) {
            //logger.error("", e);
        }

        try {

            this.rabbitTemplate.send(routing.getExchangeName(), routing.getRoutingKey(), data);
        } catch (IOException e) {
            //logger.error("", e);
        }
    }

    @Override
    public boolean validArguments(String operationName, String exchangeName, String value) {
        return operationName == null || exchangeName == null || value == null;
    }
}
