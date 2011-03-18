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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.amqp.util.MessageConstants;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class DeclarativeBindingDelegate implements BindingDelegate {

    private final ChannelTemplate template;

    public DeclarativeBindingDelegate(ConnectionFactory connectionFactory) {
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
    public String bindExchangeToAnonymousQueue(final String exchangeName, final String routingKey, final String exchangeType) throws ChannelException {
        return declareAndBind(exchangeName, routingKey, exchangeType != null ? exchangeType : MessageConstants.DEFAULT_EXCHANGE_TYPE, null);
    }

     /**
     * Declares an exchange and a named queue, then binds them together:
     * <ul><li>A durable, non-autodelete exchange of a given type</li>
     * <li>A non-durable, exclusive, autodelete queue with a generated name</li></ul>
     * Returns the queue name generated.
     * @param exchangeName
     * @param routingKey
     * @param exchangeType
     * @throws ChannelException
     */
    public void bindExchangeToNamedQueue(final String exchangeName, final String routingKey, final String exchangeType, final String queueName) throws ChannelException {
        declareAndBind(exchangeName, routingKey, exchangeType, queueName);
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
    private String declareAndBind(final String exchangeName, final String routingKey, final String exchangeType, final String queueName) throws ChannelException {
        return this.template.execute(new ChannelCallback<String>() {

            public String doInChannel(Channel channel) throws ChannelException {
                String name = queueName;
                try { 
                    /** durable */
                    channel.exchangeDeclare(exchangeName, exchangeType, true);
                    if (name == null) {
                       name = channel.queueDeclare().getQueue();
                    }
                    else {
                        /** durable, exclusive, auto-delete, args */
                       channel.queueDeclare(name, true, false, false, null);
                    }

                    channel.queueBind(name, exchangeName, (routingKey != null ? routingKey : name));
                    return name;
                }
                catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });
    }


}
