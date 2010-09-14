/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
 *
 */
package org.hyperic.hq.plugin.rabbitmq.configure;


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.erlang.core.ErlangTemplate;

/**
 * RabbitConfig configures Spring AMQP RabbitMQ objects
 *
 * @author Helena Edelson
 */
public class RabbitConfiguration {

    @Value("${hyperic.rabbit.routingkey}")
    private String hypericRoutingKeyName;

    @Value("${hyperic.rabbit.exchange}")
    private String exchangeName;

    @Value("${hyperic.rabbit.request.queue}")
    private String requestQueueName;

    @Value("${hyperic.rabbit.response.queue}")
    private String responseQueueName;

    @Value("${consumer.concurrentConsumers}")
    private String concurrentConsumers;

    @Autowired
    private SingleConnectionFactory singleConnectionFactory;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName + ExchangeType.topic);
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(exchangeName + ExchangeType.fanout);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(exchangeName + ExchangeType.direct);
    }

    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueueName);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(responseQueueName);
    }

    @Bean
    public RabbitBrokerAdmin rabbitBrokerAdmin() {
        return new RabbitBrokerAdmin(singleConnectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(singleConnectionFactory);
        rabbitTemplate.setExchange(directExchange().getName());
        rabbitTemplate.setQueue(requestQueue().getName());
        return rabbitTemplate;
    }

    @Bean
    public RabbitGateway rabbitGateway() {
        RabbitGateway rabbitGateway = new RabbitBrokerGateway(rabbitBrokerAdmin());
        rabbitGateway.createQueue(requestQueue().getName());
        rabbitGateway.createQueue(responseQueue().getName());
        return rabbitGateway;
    }

    @Bean
    public ErlangGateway erlangGatway() {
        return new ErlangBrokerGateway(rabbitBrokerAdmin().getErlangTemplate());
    }

    /* Not read for this yet.
    @Bean
    public RabbitScheduler rabbitTaskScheduler() {
        return new RabbitScheduler(rabbitTemplate());
    }*/

}