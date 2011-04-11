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
public class QueueFactoryBean /*implements FactoryBean<String>*/ {

    private final BindingHandler declarativeBindingHandler;

    private final String queueName;

    private final String exchangeName;

    private final String bindingPattern;

    /**
     * Create a new named {@link com.rabbitmq.client.AMQP.Queue}
     * @param connectionFactory Used to create a Queue
     * @param exchangeName      Exchange name to bind the Queue
     * @param queueName         Name of the queue
     */
    public QueueFactoryBean(ConnectionFactory connectionFactory, String queueName, String exchangeName, String bindingPattern) {
        this.declarativeBindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.bindingPattern = bindingPattern;
    }

    /**
     * Creates a new named {@link com.rabbitmq.client.AMQP.Queue}
     * then binds the queue to an exchange by name. 
     * @return the queue name
     * @throws ChannelException
     */
    public String getObject() throws ChannelException {
        declarativeBindingHandler.declareAndBind(QueueFactoryBean.this.queueName, QueueFactoryBean.this.exchangeName, QueueFactoryBean.this.bindingPattern);
        return QueueFactoryBean.this.queueName;
    }
    
    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

}
