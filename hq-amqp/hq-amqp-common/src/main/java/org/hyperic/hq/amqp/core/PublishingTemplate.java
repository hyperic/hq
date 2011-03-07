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

package org.hyperic.hq.amqp.core;

import java.io.IOException;

import static com.rabbitmq.client.AMQP.BasicProperties; 
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

import com.rabbitmq.client.MessageProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.amqp.convert.Converter;
import org.hyperic.hq.amqp.convert.JsonMappingConverter;

import static org.hyperic.hq.amqp.util.MessageConstants.CHARSET;
import static org.hyperic.hq.amqp.util.MessageConstants.MESSAGE_PROPERTIES;

/**
 * The one thing done synchronously is sending which is encapsulated here.
 * Channel lifecycle and handling is delegated to the ChannelTemplate.
 * @author Helena Edelson
 */
public class PublishingTemplate implements AmqpOperations {

    private final Log logger = LogFactory.getLog(getClass());

    private static final String DEFAULT_EXCHANGE = ""; 

    private static final String DEFAULT_ROUTING_KEY = "";

    private final Converter converter;

    private final ChannelTemplate channelTemplate;

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory Used to create a connection to send messages on
     * @throws ChannelException If a {@link Channel} cannot be opened
     */
    public PublishingTemplate(ConnectionFactory connectionFactory)  throws ChannelException {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.converter = new JsonMappingConverter();
        //possibly set QOS and a default reply timeout.
    }

    /**
     * Send the given message to the specified exchange with provided routing key.
     * @param exchangeName The exchange to send messages to
     * @param routingKey   the routing key
     * @param message      the Message to send
     * @throws ChannelException
     */
    public void send(final String exchangeName, final String routingKey, final Object message, final BasicProperties props) throws ChannelException {
        this.channelTemplate.execute(new ChannelCallback<String>() {
            //@Override
            public String doInChannel(Channel channel) throws IOException {
                doSend(channel, exchangeName, routingKey, message, props);
                return null;
            }
        });
    }

    /**
     * Assign exchangeName and routingKey values if they are null.
     * Convert the Object message to byte[].
     * Publishes the message as byte[] to the specified exchange with provided routing key
     * with {@link com.rabbitmq.client.AMQP.BasicProperties}.
     * @param channel The Channel to use from the ChannelTemplate
     * @param exchangeName The exchange to send messages to
     * @param routingKey   the routing key
     * @param message      the Object message to send
     * @param props        AMQP.BasicProperties
     * @throws java.io.IOException
     */
    private void doSend(Channel channel, String exchangeName, String routingKey, final Object message, BasicProperties props) throws IOException {
        exchangeName = exchangeName != null ? exchangeName : DEFAULT_EXCHANGE;
        routingKey = routingKey != null ? routingKey : DEFAULT_ROUTING_KEY;

        props = props != null ? props : MESSAGE_PROPERTIES;
        this.logger.debug("Sending message '" + message + "' to exchange '" + exchangeName + "' with routingKey '" + routingKey + "'");

        byte[] bytes = converter.from(message).getBytes(CHARSET);
        channel.basicPublish(exchangeName, routingKey, props, bytes);
    }

}
