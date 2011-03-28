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

package org.hyperic.hq.amqp;

import org.hyperic.hq.amqp.admin.RabbitAdminTemplate;
import org.hyperic.hq.amqp.configuration.CommonAmqpConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple config for prototype.
 * @author Helena Edelson
 */
@Configuration
public class CommonServerConfiguration extends CommonAmqpConfiguration {

    @Bean
    public RabbitTemplate serverRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setExchange(agentExchange);
        template.setRoutingKey(routingKey);
        return template;
    }
    
    @Bean
    public OperationService operationService() {
        return new AmqpOperationService(serverRabbitTemplate());
    }

    @Bean
    public RabbitAdminTemplate adminTemplate() {
        return new RabbitAdminTemplate();
    }

    @Bean
    public AmqpAgentListenerHandler amqpAgentCommandHandler() {
        return new AmqpAgentListenerHandler(operationService());
    }

    @Bean
    public SimpleMessageListenerContainer pingListener() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitConnectionFactory());
        container.setMessageListener(new MessageListenerAdapter(amqpAgentCommandHandler()));
        container.setQueues(serverQueue());
        return container;
    }
}