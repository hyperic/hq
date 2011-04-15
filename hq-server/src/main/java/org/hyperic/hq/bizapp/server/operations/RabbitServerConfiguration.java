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
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.bizapp.server.operations;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Helena Edelson
 */
@Configuration
public class RabbitServerConfiguration {

    @Autowired
    private RegisterAgentService registerAgentService;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public SimpleMessageListenerContainer registerAgentHandler() {
        ChannelTemplate template = new ChannelTemplate(new ConnectionFactory());
        Channel channel = template.createChannel();
        String requestQueue = null;
        try {
            channel.exchangeDeclare(Constants.TO_SERVER_EXCHANGE, "topic", true, false, null);
            requestQueue = channel.queueDeclare("request", true, false, false, null).getQueue();
            channel.queueBind(requestQueue, Constants.TO_SERVER_EXCHANGE, "request.*");

            channel.exchangeDeclare(Constants.TO_AGENT_EXCHANGE, "topic", true, false, null);
            String responseQueue = channel.queueDeclare("response", true, false, false, null).getQueue();
            channel.queueBind(responseQueue, Constants.TO_AGENT_EXCHANGE, "response.*");


        } catch (Exception e) {
            System.out.println(e.getCause());
        } finally {
            template.releaseResources(channel);
        }

        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer(new SingleConnectionFactory());
        listener.setMessageListener(new MessageListenerAdapter(registerAgentService));
        listener.setQueueName(requestQueue);
        return listener;
    }
}
