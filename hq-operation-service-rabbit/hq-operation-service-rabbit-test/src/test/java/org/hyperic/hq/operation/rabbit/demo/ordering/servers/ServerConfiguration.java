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

package org.hyperic.hq.operation.rabbit.demo.ordering.servers;

import org.hyperic.hq.operation.rabbit.admin.RabbitAdminTemplate;
import org.hyperic.hq.operation.rabbit.demo.TestConfiguration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public class ServerConfiguration extends TestConfiguration {

    private int messagesToSend = 5000;

    @Bean
    public Server server() {  
        return new Server(messagesToSend, serverRabbitTemplate(), agentListener());
    }

    @Bean
    public RabbitAdminTemplate adminTemplate() {
        return new RabbitAdminTemplate();
    }

    @Bean
    public RabbitTemplate serverRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(agentExchangeName);
        template.setRoutingKey(agentQueueName);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer agentListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new SimpleServerResponseHandler()));
        container.setQueues(agentQueue());
        return container;
    }

    @Bean
    public Binding direct() {
        Binding direct = BindingBuilder.from(directQueue()).to(agentExchange()).with(directQueue().getName());
        amqpAdmin().declareBinding(direct);
        return direct;
    }

    @Bean
    public Binding fanout() {
        Binding fanout = BindingBuilder.from(fanoutQueue()).to(fanoutExchange());
        amqpAdmin().declareBinding(fanout);
        return fanout;
    }

    @Bean
    public Binding topic() {
        Binding topic = BindingBuilder.from(topicQueue()).to(topicExchange()).with(topicQueue().getName());
        amqpAdmin().declareBinding(topic);
        return topic;
    }
}
