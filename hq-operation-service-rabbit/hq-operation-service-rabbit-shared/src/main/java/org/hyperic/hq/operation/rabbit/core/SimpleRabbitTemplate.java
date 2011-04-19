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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.api.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Helena Edelson
 */
@Component
public class SimpleRabbitTemplate implements RabbitTemplate {

    protected final Log logger = LogFactory.getLog(this.getClass());

    protected final Converter<Object, String> converter;

    protected final ChannelTemplate channelTemplate;

    protected final Channel channel;

    protected final Object monitor = new Object();

    protected final AtomicBoolean read = new AtomicBoolean(true);

    protected final AtomicLong timeout = new AtomicLong(6000);

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory ConnectionFactory used to create a connection
     * @param converter         the converter to use
     */
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory, Converter<Object, String> converter) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.channel = channelTemplate.createChannel();
        this.converter = converter;
    }

    /**
     * Sends a message
     * @param exchange the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     * @param props        AMQP properties containing a correlation id
     * @throws ChannelException
     */
    public void send(String exchange, String routingKey, Object data, AMQP.BasicProperties props) throws ChannelException {
        publish(exchange, routingKey, data, props);
    }

    /**
     * TODO in progress
     * @param responseQueueName    the name of the queue to consume the response from
     * @param exchange the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     * @param props        AMQP properties containing a correlation id
     * @return the object returned
     * @throws ChannelException
     */
    public Object sendAndReceive(final String responseQueueName, String exchange, String routingKey, Object data, AMQP.BasicProperties props, Class<?> responseType) throws ChannelException {
        publish(exchange, routingKey, data, props);

        try {
            synchronized (monitor) {
                return consume(responseQueueName, props, responseType);
            }
        } catch (IOException e) {
            throw new ChannelException("Unable to complete consuming from channel " + channel + " with queue " + responseQueueName, e);
        }
    }

    /**
     * Publishes a message to the broker
     * @param exchange the exchange name to use
     * @param routingKey the routing key to use
     * @param data the data to send
     * @param props amqp properties
     */
    private void publish(final String exchange, final String routingKey, final Object data, AMQP.BasicProperties props) {
        final byte[] bytes = converter.write(data).getBytes(MessageConstants.CHARSET);

        try {
            synchronized (monitor) {
                channel.basicPublish(exchange, routingKey, props, bytes);
            }
            logger.debug("sent " + data + " to " + exchange + " with " + routingKey);
        } catch (IOException e) {
            throw new ChannelException("Could not send " + data + " to " + exchange + " with " + routingKey, e);
        }
    }

    /**
     * Consumes from the given queue and loops until the message with a matching
     * correlationId is received.
     * @param queueName the queue name to consume from
     * @param props basic properties
     * @param responseType
     * @return the converted data to the given Class<?> responseType
     * @throws IOException
     */
    private Object consume(String queueName, AMQP.BasicProperties props, Class<?> responseType) throws IOException {
        QueueingConsumer consumer = new QueueingConsumer(channel);

        QueueingConsumer.Delivery delivery = null;

        while (read.get()) {
            try {
                channel.basicConsume(queueName, false, consumer);
                delivery = consumer.nextDelivery();

                if (props.getCorrelationId().equalsIgnoreCase(delivery.getProperties().getCorrelationId())) {
                    logger.debug("received message with " + delivery.getProperties().getCorrelationId());
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    read.set(false);
                    return converter.read(new String(delivery.getBody(), MessageConstants.CHARSET), responseType.getClass());
                }
            }
            catch (Exception e) {
                if (delivery != null) channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
                logger.error("Unable to handle message", e);
            }

        }
        return null;   // if timeout exceeded
    }

    @PreDestroy
    void close() {
        synchronized (monitor) {
            read.set(false);
            channelTemplate.releaseResources(channel);
        }
    }
}
