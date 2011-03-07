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

package org.hyperic.hq.amqp.prototype.ordering.agents;

import org.hyperic.hq.amqp.configuration.CommonAmqpConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public abstract class CommonAgentConfiguration extends CommonAmqpConfiguration {
    
   @Bean
    public RabbitTemplate agentTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setMessageConverter(new JsonMessageConverter());
        template.setExchange(agentToServerExchange().getName());
        template.setRoutingKey(agentToServerQueue().getName());
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer directListener() throws InterruptedException {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentTemplate())));
        container.setQueues(directQueue()); 
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer fanoutListener() throws Exception {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentTemplate())));
        container.setQueues(fanoutQueue());
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer topicListener() throws Exception {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(agentTemplate())));
        container.setQueues(topicQueue());
        return container;
    }
}

