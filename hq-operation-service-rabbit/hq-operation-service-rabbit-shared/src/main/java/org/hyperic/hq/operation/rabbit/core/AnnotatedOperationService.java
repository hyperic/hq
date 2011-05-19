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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.convert.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationService implements OperationService {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final RoutingRegistry routingRegistry;

    private final RabbitTemplate rabbitTemplate;

    private final Converter<Object, String> converter;

    private final PropertiesConverter propertiesConverter;

    /**
     * Creates a new instance that sends messages to a Rabbit broker
     * @param rabbitTemplate  The rabbitTemplate to use for dispatch
     * @param routingRegistry The routing cache to query for instructions
     * @param converter       the convert to use for byte[] - object conversion
     */
    @Autowired
    public AnnotatedOperationService(RabbitTemplate rabbitTemplate, RoutingRegistry routingRegistry,
                                     Converter<Object, String> converter) {
        this.rabbitTemplate = rabbitTemplate;
        this.routingRegistry = routingRegistry;
        this.converter = converter;
        this.propertiesConverter = new PropertiesConverter(null);
    }

    /**
     * Performs an operation by operation name
     * Delegates handling to the RabbitTemplate for handling.
     * @param operationName the operation name
     * @param data          the data to send
     * @return if the method has a return signature, the value after invocation is returned
     * @throws org.hyperic.hq.operation.OperationFailedException
     *
     */
    public Object perform(String operationName, Object data, Class<? extends Annotation> annotation) throws OperationFailedException {
        try {
            OperationToRoutingMapping mapping = routingRegistry.map(operationName, annotation);

            asynchronousSend(mapping, data);
            return null;
            /*if (mapping.operationReturnsVoid()) {
                asynchronousSend(mapping, data);
                return null;
            } else {
                return synchronousSend(mapping, data);
            }*/

        } catch (ChannelException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Sends a message
     * @param mapping the routing data
     * @param data    the operation data
     */
    private void asynchronousSend(OperationToRoutingMapping mapping, Object data) {
        rabbitTemplate.publish(mapping.getExchange(), mapping.getRoutingKey(), data, null);
    }

    /**
     * Sends a message
     * @param mapping the routing data
     * @param data    the operation data
     * @return returns the Object from the receiver
     */
    private Object synchronousSend(OperationToRoutingMapping mapping, Object data) {
        Object response = rabbitTemplate.publishAndReceive(
                mapping.getQueue(), mapping.getExchange(), mapping.getRoutingKey(), data, mapping.getTransformType(), null);

        rabbitTemplate.close();
        return response;
    } 
}
