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

package org.hyperic.hq.operation.rabbit.handler;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Helena Edelson
 */
public class MessageListenerFactoryBean implements FactoryBean<SimpleMessageListenerContainer> {

    private final ConnectionFactory connectionFactory;

    private final Object handler;

    private final Queue[] queues;

    public MessageListenerFactoryBean(ConnectionFactory connectionFactory, Object handler, Queue... queues) {
        this.connectionFactory = connectionFactory;
        this.handler = handler;
        this.queues = queues;
    }
 
    public SimpleMessageListenerContainer getObject() throws Exception {
        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer();
        listener.setConnectionFactory(connectionFactory);
        listener.setMessageListener(new MessageListenerAdapter(handler));
        listener.setQueues(queues);
        return null;
    }

    public Class<?> getObjectType() {
        return SimpleMessageListenerContainer.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
