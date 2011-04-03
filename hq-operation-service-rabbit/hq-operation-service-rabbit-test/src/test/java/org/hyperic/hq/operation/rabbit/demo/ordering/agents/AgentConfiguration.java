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

package org.hyperic.hq.operation.rabbit.demo.ordering.agents;

import org.hyperic.hq.operation.rabbit.demo.TestConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public class AgentConfiguration extends TestConfiguration {

    @Bean
    public RabbitTemplate agentRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setExchange(serverDirectExchangeName);
        template.setRoutingKey(agentQueueName);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer serverListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentRabbitTemplate())));
        container.setQueues(agentQueue());
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer directListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentRabbitTemplate())));
        container.setQueues(directQueue());
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer fanoutListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentRabbitTemplate())));
        container.setQueues(fanoutQueue());
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer topicListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());  
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentRabbitTemplate())));
        container.setQueues(topicQueue());
        return container;
    }
}

