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
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.util.Message;
import org.hyperic.hq.operation.rabbit.util.OperationRouting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Helena Edelson
 */
public class AnnotatedOperationEndpointDiscoverer extends AbstractMethodInvokingRegistry implements Endpoint, OperationDiscoverer {

    public AnnotatedOperationEndpointDiscoverer() {
        super(new SingleConnectionFactory());
    }

    public AnnotatedOperationEndpointDiscoverer(ConnectionFactory connectionFactory, Converter<Object, String> converter, RoutingRegistry routingRegistry) {
        super(connectionFactory, converter, routingRegistry);
    }

    /**
     * Passes the candidate instance along with the marker annotation
     * @param candidate The candidate instance which can be a dispatcher or endpoint
     */
    public void discover(Object candidate) {
        super.discover(candidate, OperationEndpoint.class);
    }

    @Override
    public boolean validArguments(String operationName, String exchangeName, String value) {
        return operationName == null || exchangeName == null || value == null;
    }

    /**
     * Handles incoming async messages
     * @param envelope The envelope to handle
     * @throws EnvelopeHandlingException
     */
    public void handle(Envelope envelope) throws EnvelopeHandlingException {
        if (!this.operationMappings.containsKey(envelope.getOperationName()))
            throw new OperationNotSupportedException(envelope.getOperationName());

        MethodInvoker invoker = this.operationMappings.get(envelope.getOperationName());

        try {
            Object object = invoker.invoke(envelope.getContext());
            String json = this.converter.write(object);

            /* TODO what check will we use to know to reply */
            if (envelope.getReplyTo() != null) {
                Envelope response = new Message(envelope.getOperationId(), envelope.getOperationName(), json, envelope.getReplyTo());
                OperationRouting routing = this.routingRegistry.map(envelope.getOperationName());
                this.rabbitTemplate.send(routing.getExchangeName(), routing.getRoutingKey(), json);
            }
        }
        catch (IllegalAccessException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
        catch (InvocationTargetException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
        catch (IOException e) {
            throw new EnvelopeHandlingException("Exception sending response to operation", e);
        }
    }
}
