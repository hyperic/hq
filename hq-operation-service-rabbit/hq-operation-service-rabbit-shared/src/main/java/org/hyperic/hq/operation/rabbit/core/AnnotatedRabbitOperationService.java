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

import com.rabbitmq.client.AMQP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.AbstractOperation;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.api.Envelope;
import org.hyperic.hq.operation.rabbit.api.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.api.RoutingRegistry;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedRabbitOperationService implements OperationService {

    private final Log logger = LogFactory.getLog(AnnotatedRabbitOperationService.class);

    private final RoutingRegistry routingRegistry;

    private final RabbitTemplate rabbitTemplate;

    private final Converter<Object, String> converter;

    /**
     * Creates a new instance that sends messages to a Rabbit broker
     * @param rabbitTemplate  The rabbitTemplate to use for dispatch
     * @param routingRegistry The routing cache to query for instructions
     * @param converter the convert to use for byte[] - object conversion
     */
    @Autowired
    public AnnotatedRabbitOperationService(RabbitTemplate rabbitTemplate, RoutingRegistry routingRegistry, Converter<Object, String> converter) {
        this.rabbitTemplate = rabbitTemplate;
        this.routingRegistry = routingRegistry;
        this.converter = converter;
    }

    /**
     *  TODO test Envelope envelope = createEnvelope(operationName, data);
     * Performs an operation by operation name
     * Delegates handling to the RabbitTemplate for handling.
     * @param operationName the operation name
     * @param data the data to send
     * @return if the method has a return signature, the value after invocation is returned
     * @throws org.hyperic.hq.operation.OperationFailedException 
     */
    public Object perform(String operationName, Object data) throws OperationFailedException {
        OperationToRoutingMapping mapping = this.routingRegistry.map(operationName);
 
        try {
            if (mapping.operationRequiresResponse()) {
                return synchronousSend(mapping, data);
            } else {
                asynchronousSend(mapping, data);
                return null;
            }
        } catch (ChannelException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Creates the wrapper for the data to send by converting the data payload to
     * a JSON string, and setting an auto-generated correlation ID to assert
     * request-response operations match. Using JSON so that if in the future,
     * Hyperic wishes to implement a RESTful OperationService, this functionality
     * is compatible. And it was faster than serialization in benchmark testing.
     *
     * @param operationName the operation name
     * @param data the data to transform to a JSON string
     * @return org.hyperic.hq.operation.Envelope
     */
    private Envelope createEnvelope(String operationName, Object data) {
        AMQP.BasicProperties bp = MessageConstants.getBasicProperties(data);
        return new Envelope(operationName, this.converter.write(data), bp.getCorrelationId());
    }

    /**
     * Sends a message  
     * @param mapping the routing data
     * @param data the operation data
     */
    private void asynchronousSend(OperationToRoutingMapping mapping, Object data) {
        this.rabbitTemplate.send(mapping.getExchangeName(), mapping.getRoutingKey(), data, getBasicProperties(data)); 
    }

    /**
     * Sends a message
     * </p>
     * Most if not all of Hyperic's current API is synchronous.
     * Most of those need to be migrated to async to improve performance,
     * and leverage the new messaging architecture.
     * @param mapping the routing data
     * @param data the operation data
     * @return returns the Object from the receiver
     */
    private Object synchronousSend(OperationToRoutingMapping mapping, Object data) {
        return this.rabbitTemplate.sendAndReceive(
                mapping.getQueueName(), mapping.getExchangeName(), mapping.getRoutingKey(), this.converter.write(data), getBasicProperties(data));
    }

    /**
     * Creates the default message properties and sets a correlationId
     * @param data the object to pull context from
     * @return BasicProperties with a correlationid
     */
    protected AMQP.BasicProperties getBasicProperties(Object data) {
        AMQP.BasicProperties bp = MessageConstants.DEFAULT_MESSAGE_PROPERTIES;

        if (data.getClass().isAssignableFrom(AbstractOperation.class)) {
            bp.setCorrelationId(((AbstractOperation) data).getOperationName());
        } else {
            bp.setCorrelationId(new Random().toString());
        }
        return bp;
    }
}
