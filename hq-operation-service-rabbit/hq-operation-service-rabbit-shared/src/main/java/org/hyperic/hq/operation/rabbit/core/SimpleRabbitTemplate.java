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

import com.rabbitmq.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.AbstractOperation;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.api.ChannelCallback;
import org.hyperic.hq.operation.rabbit.api.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.ServerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

/**
 * @author Helena Edelson
 */
@Component
public class SimpleRabbitTemplate implements RabbitTemplate {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private final Converter<Object, String> converter;

    private final ChannelTemplate channelTemplate;

    private final Object monitor = new Object();

    protected Channel channel;

    private final String exchangeName;

    protected String serverQueue;

    protected String agentQueue;

    protected QueueingConsumer queueingConsumer;

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory Used to create a connection to send messages on
     */
    @Autowired
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory) {
        this(connectionFactory, null);
    }

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory           ConnectionFactory used to create a connection
     * @param exchangeName The exchange name to use. if null, uses the AMQP default
     */
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory, String exchangeName) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.converter = new JsonMappingConverter();
        this.exchangeName = exchangeName; 
    }

    /**
     * @param exchangeName the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     */
    public Boolean send(final String exchangeName, final String routingKey, final String data) throws ChannelException {
        final byte[] bytes = data.getBytes(MessageConstants.CHARSET);
 
        synchronized (this.monitor) {
            return this.channelTemplate.execute(new ChannelCallback<Boolean>() {
                public Boolean doInChannel(Channel channel) throws ChannelException {
                    try {
                        channel.basicPublish(exchangeName, routingKey, MessageConstants.DEFAULT_MESSAGE_PROPERTIES, bytes);
                        logger.info("sent " + data + " to " + exchangeName + " with " + routingKey);
                        return true;
                    } catch (IOException e) {
                        throw new ChannelException("Could not send " + data + " to " + exchangeName + " with " + routingKey, e);
                    }
                }
            });
        }
    }

    /**
     * Because autoAck = false call Channel.basicAck to acknowledge receipt
     * @param exchangeName the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     * @return the response object
     * @throws IOException
     */
    public Object sendAndReceive(String exchangeName, String routingKey, String data) throws ChannelException {
        send(exchangeName, routingKey, data);

        AMQP.BasicProperties bp = getBasicProperties(data);
        String correlationId = bp.getCorrelationId();

        synchronized (monitor) {
            while (true) {
                try {
                    GetResponse response = channel.basicGet(agentQueue, false);
                    if (response != null && response.getProps().getCorrelationId().equals(correlationId)) {
                        this.logger.debug("received=" + response);
                        Object received = this.converter.read(new String(response.getBody()), Object.class);
                        this.channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                        return received;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not receive from" + agentQueue, e);
                }
            }
        }
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


    // TODO remove

    public void timedTest(int append) throws IOException, InterruptedException {
        String msg = "test-" + append;
        byte[] bytes = this.converter.write(msg).getBytes(MessageConstants.CHARSET);
        AMQP.BasicProperties bp = getBasicProperties(msg);
        String correlationId = bp.getCorrelationId();

        synchronized (monitor) {
            this.channel.basicPublish(ServerConstants.EXCHANGE_TO_AGENT, "test", bp, bytes);
            while (true) {
                GetResponse response = channel.basicGet(agentQueue, false);
                if (response.getProps().getCorrelationId().equals(correlationId)) {
                    this.logger.debug("received=" + this.converter.read(new String(response.getBody()), Object.class));
                    break;
                }
            }
        }
    }

    /**
     * TODO automate given agent
     */
    public void shutdown() {
        synchronized (this.monitor) {
            try {
                this.channel.close();
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        }
    }
}
