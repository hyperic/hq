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
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Helena Edelson
 */
public class AnnotatedRabbitOperationService implements OperationService, Dispatcher, Endpoint, OperationDiscoverer {

    private OperationMethodInvokingRegistry dispatchers;

    private OperationMethodInvokingRegistry endpoints;

    private RabbitTemplate rabbitTemplate;

    private final Converter<Object, String> converter;

    public AnnotatedRabbitOperationService() {
        this(new SingleConnectionFactory());
    }

    public AnnotatedRabbitOperationService(ConnectionFactory connectionFactory) {
        this(connectionFactory, new JsonMappingConverter());
    }

    /**
     * Creates a new instance that sends messages to a Rabbit broker
     * @param connectionFactory The connectionFactory to use
     * @param converter         The convert used to convert a context to a message
     */
    public AnnotatedRabbitOperationService(ConnectionFactory connectionFactory, Converter<Object, String> converter) {
        this.converter = converter;
        this.rabbitTemplate = new SimpleRabbitTemplate(connectionFactory);
        this.dispatchers = new OperationMethodInvokingRegistry(connectionFactory, converter);
        this.endpoints = new OperationMethodInvokingRegistry(connectionFactory, converter);
    }

    /**
     * @param candidate  The candidate instance which can be a dispatcher or endpoint
     * @param annotation a dispatcher or endpoint
     * @throws OperationDiscoveryException
     */
    public void discover(Object candidate, Class<? extends Annotation> annotation) throws OperationDiscoveryException {
        if (annotation.getClass().equals(OperationDispatcher.class)) {
            this.dispatchers.discover(candidate, OperationDispatcher.class);
        } else {
            this.endpoints.discover(candidate, OperationEndpoint.class);
        }
    }

    /**
     * @param envelope The envelope with meta instructions and data
     * @return
     * @throws OperationFailedException
     */
    public Object perform(Envelope envelope) throws OperationFailedException {
        OperationToRoutingMapping mapping = getOperationToRoutingMapping(envelope.getType(), envelope.getOperationName());

        try {
            if (envelope.getReplyTo() != null) {
                return rabbitTemplate.sendAndReceive(mapping.getExchangeName(), mapping.getRoutingKey(), envelope);
            } else {
                rabbitTemplate.send(mapping.getExchangeName(), mapping.getRoutingKey(), this.converter.write(envelope.getContent()));
                return null;
            }
        } catch (IOException e) {
            throw new OperationFailedException(e.getMessage(), e);
        }
    }

    /**
     * @param operationName the operation name to use
     * @param data          the data to send
     * @return
     */
    public Object dispatch(String operationName, Object data) throws OperationFailedException {
        OperationMethodInvokingRegistry.MethodInvoker invoker = this.dispatchers.map(operationName);
        Envelope envelope = new Envelope(operationName, this.converter.write(data), null, OperationDispatcher.class);

        if (invoker.operationHasReturnType()) {
            String response = (String) perform(envelope);
            return this.converter.read(response, invoker.getReturnType().getClass());
        } else {
            perform(envelope);
            return null;
        }
    }

    /**
     * Handles incoming async messages
     * @param envelope The envelope to handle
     * @throws EnvelopeHandlingException
     */
    public void handle(Envelope envelope) throws EnvelopeHandlingException {
        if (!this.endpoints.operationMappings.containsKey(envelope.getOperationName()))
            throw new OperationNotSupportedException(envelope.getOperationName());

        OperationMethodInvokingRegistry.MethodInvoker invoker = this.endpoints.map(envelope.getOperationName());

        Object response;
        try {
            response = invoker.invoke(envelope.getContent());
            if (response != null) {
                Envelope responseEnvelope = new Envelope(envelope.getOperationName(), this.converter.write(response), "replyto", OperationEndpoint.class);
                perform(responseEnvelope);
            }
        }
        catch (IllegalAccessException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
        catch (InvocationTargetException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
    }

    private OperationToRoutingMapping getOperationToRoutingMapping(Class<? extends Annotation> annotationType, String operationName) {
        return annotationType.getClass().equals(OperationDispatcher.class) ?
                this.dispatchers.routingRegistry.map(operationName) : this.endpoints.routingRegistry.map(operationName);
    }

}
