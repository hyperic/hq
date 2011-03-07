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

package org.hyperic.hq.amqp.prototype.distribution.agents;

import org.hyperic.hq.amqp.configuration.CommonAmqpConfiguration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public class AgentConfig extends CommonAmqpConfiguration {
 
    @Bean
    public Queue topicQueue() {
        return amqpAdmin().declareQueue();
    }

    @Bean
    public TopicExchange topicExchange() {
        TopicExchange e = new TopicExchange("rockets.exchanges.topic", true, false);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(topicQueue()).to(e).with(topicQueue().getName()));
        return e;
    }

    @Bean
    public AmqpTemplate topicTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(topicExchange().getName());
        template.setRoutingKey(topicQueue().getName());
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer topicListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(topicTemplate())));
        //container.setConcurrentConsumers(this.concurrentConsumers);
        container.setQueues(topicQueue());
        return container;
    }

     @Bean
    public Queue fanoutQueue() {
        return amqpAdmin().declareQueue();
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        FanoutExchange e = new FanoutExchange("rockets.exchanges.fanout", true, false);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(fanoutQueue()).to(e));
        return e;
    }

    @Bean
    public AmqpTemplate fanoutTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(fanoutExchange().getName());
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer fanoutListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(fanoutTemplate())));
        //container.setConcurrentConsumers(this.concurrentConsumers);
        container.setQueues(fanoutQueue());
        return container;
    }

    @Bean
    public Queue directQueue() {
        return amqpAdmin().declareQueue();
    }

    @Bean
    public DirectExchange serverToAgentDirectExchange() {
        DirectExchange e = new DirectExchange("rockets.exchanges.direct", true, false);
        amqpAdmin().declareExchange(e);
        amqpAdmin().declareBinding(BindingBuilder.from(directQueue()).to(e).with(directQueue().getName()));
        return e;
    }

    @Bean
    public AmqpTemplate directTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(serverToAgentDirectExchange().getName());
        template.setRoutingKey(directQueue().getName());
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer directListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(new Agent(directTemplate())));
        //container.setConcurrentConsumers(this.concurrentConsumers);
        container.setQueues(directQueue());
        return container;
    }
}
