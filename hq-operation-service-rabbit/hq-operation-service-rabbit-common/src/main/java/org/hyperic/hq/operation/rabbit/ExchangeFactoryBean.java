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
package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.core.BindingHandler;
import org.hyperic.hq.operation.rabbit.core.DeclarativeBindingHandler;

/**
 * @author Helena Edelson
 */
public class ExchangeFactoryBean /*implements FactoryBean<String>*/ {

    private final BindingHandler declarativeBindingHandler;

    private final String queueName;

    private final String exchangeName;

    private final String routingKey;

    /**
     * Creates a new {@link com.rabbitmq.client.AMQP.Exchange}
     * @param connectionFactory Used to get a connection to create the queue with
     * @param exchangeName      The name of the exchange
     */
    public ExchangeFactoryBean(ConnectionFactory connectionFactory, String queueName, String exchangeName, String routingKey) {
        this.declarativeBindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public String getObject() throws ChannelException {
        declarativeBindingHandler.declareAndBind(ExchangeFactoryBean.this.queueName, ExchangeFactoryBean.this.exchangeName, ExchangeFactoryBean.this.routingKey);
        return ExchangeFactoryBean.this.exchangeName;
    }

    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
