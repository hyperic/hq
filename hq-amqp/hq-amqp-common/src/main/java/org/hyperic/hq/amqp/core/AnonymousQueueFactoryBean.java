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

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Helena Edelson
 */
public class AnonymousQueueFactoryBean extends AmqpComponentBean implements FactoryBean<String> {

    /**
     * Creates a new anonymous {@link com.rabbitmq.client.AMQP.Queue}
     * @param connectionFactory Used to get a connection to create the queue with
     * @param exchangeName The name of the exchange to link the queue with
     */
    public AnonymousQueueFactoryBean(ConnectionFactory connectionFactory, String exchangeName, String exchangeType, String routingKey, boolean durable) {
        super(connectionFactory, exchangeName, exchangeType, routingKey, durable);
    }

    /**
     * Creates a new {@link com.rabbitmq.client.AMQP.Queue} with an
     * auto-generated name.
     * @return
     * @throws ChannelException
     */
    public String getObject() throws ChannelException {
        return declarativeBindingDelegate.bindExchangeToAnonymousQueue(AnonymousQueueFactoryBean.this.exchangeName,
                AnonymousQueueFactoryBean.this.exchangeType, AnonymousQueueFactoryBean.this.routingKey, AnonymousQueueFactoryBean.this.durable);
    } 
}
