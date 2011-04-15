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
import org.hyperic.hq.operation.*;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Helena Edelson
 */
@Component("operationService")
public class AnnotatedRabbitOperationService implements OperationService, OperationDiscoverer {

    private final Log logger = LogFactory.getLog(RabbitMessageListenerContainer.class);
 
    private OperationMethodInvokingRegistry operationMethodInvokingRegistry;
 
    private RabbitTemplate rabbitTemplate;

    private Converter<Object, String> converter;

    /**
     * Creates a new instance that sends messages to a Rabbit broker
     * @param connectionFactory The connectionFactory to use
     * @param routingRegistry
     * @param converter The convert used to convert a context to a message
     */
    @Autowired
    public AnnotatedRabbitOperationService(ConnectionFactory connectionFactory, RoutingRegistry routingRegistry, Converter<Object, String> converter) {
        this.converter = converter != null ? converter : new JsonMappingConverter();
        this.rabbitTemplate = new SimpleRabbitTemplate(connectionFactory);
        this.operationMethodInvokingRegistry = new OperationMethodInvokingRegistry(routingRegistry, converter);
    }
  
    /**
     * @param candidate  The candidate instance which can be a dispatcher or endpoint
     * @param annotation a dispatcher or endpoint
     * @throws OperationDiscoveryException
     */
    public void discover(Object candidate, Class<? extends Annotation> annotation) throws OperationDiscoveryException {
        this.operationMethodInvokingRegistry.discover(candidate, annotation);
    }

    /**
     * @param envelope The envelope with meta instructions and data
     * @return
     * @throws OperationFailedException
     */
    public Object perform(Envelope envelope) throws OperationFailedException {
        OperationToRoutingMapping mapping = this.operationMethodInvokingRegistry.routingRegistry.map(envelope.getOperationName());

        try {
            return mapping.operationRequiresResponse() ?
                    this.rabbitTemplate.sendAndReceive(mapping.getExchangeName(), mapping.getRoutingKey(), envelope)
                        : this.rabbitTemplate.send(mapping.getExchangeName(), mapping.getRoutingKey(), envelope);

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
        OperationMethodInvokingRegistry.MethodInvoker invoker = this.operationMethodInvokingRegistry.map(operationName);
        Envelope envelope = new Envelope(operationName, this.converter.write(data));

        if (invoker.operationHasReturnType()) {
            return perform(envelope); 
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
        if (!this.operationMethodInvokingRegistry.operationMappings.containsKey(envelope.getOperationName()))
            throw new OperationNotSupportedException(envelope.getOperationName());

        OperationMethodInvokingRegistry.MethodInvoker invoker = this.operationMethodInvokingRegistry.map(envelope.getOperationName());
 
        try {
            Object response = invoker.invoke(envelope.getContent());
            if (response != null) {
                Envelope responseEnvelope = new Envelope(envelope.getOperationName(), this.converter.write(response));
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

    public OperationMethodInvokingRegistry getMappings() {
        return this.operationMethodInvokingRegistry;
    }
}
