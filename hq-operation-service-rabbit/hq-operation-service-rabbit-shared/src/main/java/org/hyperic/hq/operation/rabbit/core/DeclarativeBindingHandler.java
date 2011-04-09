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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class DeclarativeBindingHandler implements BindingHandler {

    private final ChannelTemplate channelTemplate;

    public DeclarativeBindingHandler(ConnectionFactory connectionFactory) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
    }

    /**
     * Declare and bind components
     * Queues declared are durable, exclusive, non-auto-delete
     * @param operation the operaton meta-data
     */
    public void declareAndBind(final Operation operation) throws ChannelException {
        Channel channel = this.channelTemplate.createChannel();
        String exchange = getExchangeType(operation.exchangeName());
        try {
            channel.exchangeDeclare(exchange, Constants.SHARED_EXCHANGE_TYPE, true, false, null);
            String queue = channel.queueDeclare(operation.operationName(), true, true, false, null).getQueue();
            channel.queueBind(queue, operation.exchangeName(), operation.value());
        } catch (IOException e) {
            throw new ChannelException(e.getMessage());
        } finally {
            this.channelTemplate.releaseResources(channel);
        }
        //declareAndBind(operation.operationName(), operation.exchangeName(), operation.value());
    }

    public void declareAndBind(final String operationName, final String exchangeName, final String bindingPattern) throws ChannelException {
        this.channelTemplate.execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    String exchange = getExchangeType(exchangeName);
                    channel.exchangeDeclare(exchange, Constants.SHARED_EXCHANGE_TYPE, true, false, null);
                    String queue = channel.queueDeclare(operationName, true, true, false, null).getQueue();
                    channel.queueBind(queue, exchange, bindingPattern);
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });
    }

    /**
     * If the exchangeType is null, returns the default, "topic"
     * @param exchangeType direct, fanout, topic, header. Can be null.xxx
     * @return String exchange type
     */
    private String getExchangeType(String exchangeType) {
        return exchangeType != null ? exchangeType : Constants.SHARED_EXCHANGE_TYPE;
    }
}
