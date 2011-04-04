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

package org.hyperic.hq.operation.rabbit.mapping;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class DeclarativeBindingHandler implements BindingHandler {

    private final Routings routings = new Routings();

    private final ChannelTemplate template;

    public DeclarativeBindingHandler(ConnectionFactory connectionFactory) {
        this.template = new ChannelTemplate(connectionFactory);
    }
 
    /**
     * Declares an exchange and an anonymous queue, then binds them together:
     * <ul><li>A durable, non-autodelete exchange of a given type</li>
     * <li>A non-durable, exclusive, autodelete queue with a generated name</li></ul>
     * Returns the queue name generated.
     * @param exchangeName
     * @param routingKey
     * @param exchangeType can be null, which will create a TopicExchange
     * @throws ChannelException
     */
    public String bindExchangeToAnonymousQueue(String exchangeName, String exchangeType, String routingKey, boolean durable) throws ChannelException {
        return declareAndBind(exchangeName, getExchangeType(exchangeType), routingKey, durable, null);
    }

     /**
     * Declares an exchange and a named queue, then binds them together:
     * <ul><li>A durable, non-autodelete exchange of a given type</li>
     * <li>A non-durable, exclusive, autodelete queue with a generated name</li></ul>
     * Returns the queue name generated.
     * @param exchangeName
     * @param routingKey
     * @param exchangeType
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     */
    public void bindExchangeToNamedQueue(String exchangeName, String exchangeType, String routingKey, boolean durable, String queueName) throws ChannelException {
        declareAndBind(exchangeName, routingKey, getExchangeType(exchangeType), durable, queueName);
    }

    /**
     * 
     * @param exchangeName
     * @param routingKey
     * @param exchangeType
     * @param queueName
     * @return
     * @throws ChannelException
     */
    private String declareAndBind(final String exchangeName, final String exchangeType, final String routingKey, final boolean durable, final String queueName) throws ChannelException {
        return this.template.execute(new ChannelCallback<String>() {

            public String doInChannel(Channel channel) throws ChannelException {
                String name = queueName;
                try {
                    channel.exchangeDeclare(exchangeName, exchangeType, durable);
                    if (name == null) {
                       name = channel.queueDeclare().getQueue();
                    }
                    else {
                       channel.queueDeclare(name, durable, false, durable, null);
                    }

                    channel.queueBind(name, exchangeName, routingKey);
                    return name;
                }
                catch (IOException e) {
                    throw new ChannelException("Could not bind queue " + name + " to exchange", e);
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
        return exchangeType != null ? exchangeType : routings.getSharedExchangeType();
    }
}
