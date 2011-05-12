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
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.operation.rabbit.core;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.convert.JsonMessageConverter;
import org.hyperic.hq.operation.rabbit.convert.JsonObjectMappingConverter;
import org.hyperic.hq.operation.rabbit.convert.MessageConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author Helena Edelson
 */
public class InvokingConsumerHandler implements MessageHandler {

    private final Log logger = LogFactory.getLog(this.getClass());
 
    /**
     * The operation name
     */
    private final Method method;

    private final Object endpoint;

    private final RabbitTemplate rabbitTemplate;

    private final String responseRoutingKey;

    private final String responseExchange;

    private MethodInvoker methodInvoker;

    public InvokingConsumerHandler(ConnectionFactory connectionFactory, Object endpoint, Method method) {
        this.rabbitTemplate = new SimpleRabbitTemplate(connectionFactory, new JsonObjectMappingConverter(), method.getReturnType());
        this.methodInvoker = new MethodInvoker(method, endpoint);
        this.method = method;
        this.endpoint = endpoint;
        this.responseExchange = method.getAnnotation(OperationEndpoint.class).responseExchange();
        this.responseRoutingKey = method.getAnnotation(OperationEndpoint.class).responseRoutingKey();
    }
 
    /**
     * Delegates conversion of the request data and invokes the method on the endpoint.
     * If the method invoked is not a void return, publishes the response.
     * @param delivery the Delivery
     * @param channel  the channel to use
     */
    public void handle(QueueingConsumer.Delivery delivery, Channel channel) throws Exception {
        if (delivery.getBody().length <= 0) return;

        if (!delivery.getEnvelope().getExchange().equalsIgnoreCase(responseExchange)
                && !delivery.getEnvelope().getRoutingKey().equalsIgnoreCase(responseRoutingKey)) {

            //channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            Object response = invoke(delivery);

            publishResponse(channel, response);
        }
    }

    private Object invoke(QueueingConsumer.Delivery delivery) {     
        try {
            return methodInvoker.invoke(delivery);
        }
        catch (IllegalAccessException e) {
            throw new ConsumerHandlingException("Exception invoking operation handler method", e);
        }
        catch (InvocationTargetException e) {
            throw new ConsumerHandlingException("Exception invoking operation handler method", e);
        }
    }

    /**
     * If the method invoked in the endpoint has a return that is not null,
     * assemble and publish the response.
     * @param response the result of invoking the handler method
     * @param channel  the channel to use
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     *          if an error occurs while publishing the response
     */
    protected void publishResponse(Channel channel, Object response) throws ChannelException {

        if (response != null && responseExchange != null && responseRoutingKey != null) {
            logger.debug(channel + " publishing response to " + responseExchange + " with " + responseRoutingKey);

            if (channel == null || !channel.isOpen()) {
                rabbitTemplate.publish(channel, responseExchange, responseRoutingKey, response, null);
            } else {
                rabbitTemplate.publish(responseExchange, responseRoutingKey, response, null);
            }
        }
    }

    private static final class MethodInvoker {

        private final Method method;

        private final Object instance;

        private final MessageConverter converter;

        MethodInvoker(Method handlerMethod, Object instance) {
            this.method = handlerMethod;
            this.instance = instance;
            this.converter = new JsonMessageConverter(handlerMethod);
        }

        Object invoke(QueueingConsumer.Delivery delivery) throws IllegalAccessException, InvocationTargetException {
            return method.invoke(instance, converter.extractRequest(delivery));
        }
    }


    @Override
    public String toString() {
        return new StringBuilder("endpoint=").append(endpoint).append(" method=").append(method)
                .append(" method argument=").append(method.getParameterTypes()[0]).append(" method return=")
                .append(method.getReturnType()).append(" response exchange=").append(responseExchange)
                .append(" response routing key=").append(responseRoutingKey).toString();
    }
}
