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
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.core.RabbitErrorHandler;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
@Configuration
public class RabbitServerConfiguration {

    @Autowired
    private RegisterAgentService registerAgentService;

    @Autowired
    private RabbitErrorHandler rabbitErrorHandler;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public SimpleMessageListenerContainer registerAgentHandler() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(registerAgentService);
        adapter.setDefaultListenerMethod("registerAgentRequest");

        final SimpleMessageListenerContainer mlc = new SimpleMessageListenerContainer(new SingleConnectionFactory());
        mlc.setMessageListener(adapter);
        mlc.setErrorHandler(rabbitErrorHandler);
        
        new ChannelTemplate(new ConnectionFactory()).execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare(Constants.TO_SERVER_EXCHANGE, "topic", true, false, null);
                    String requestQueue = channel.queueDeclare("registerAgentRequest", true, false, false, null).getQueue();
                    channel.queueBind(requestQueue, Constants.TO_SERVER_EXCHANGE, "request.*");

                    channel.exchangeDeclare(Constants.TO_AGENT_EXCHANGE, "topic", true, false, null);
                    String responseQueue = channel.queueDeclare("registerAgent", true, false, false, null).getQueue();
                    channel.queueBind(responseQueue, Constants.TO_AGENT_EXCHANGE, "response.*");

                    mlc.setQueueName(requestQueue);
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });

        return mlc;
    }
}
