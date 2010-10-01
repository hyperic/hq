/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.populate;

import org.hyperic.hq.plugin.rabbitmq.configure.RabbitTestConfiguration;
import org.hyperic.hq.plugin.rabbitmq.core.AMQPStatus;
import org.hyperic.hq.plugin.rabbitmq.core.HypericChannel;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PopulateData can be run to populate the QA rabbitmq servers
 * Not finished yet.
 * @author Helena Edelson
 */
public class PopulateData {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(RabbitTestConfiguration.class);
        addData(ctx);
        RabbitGateway rabbitGateway = ctx.getBean(RabbitGateway.class);
        RabbitTemplate rabbitTemplate = ctx.getBean(RabbitTemplate.class);
        Queue marketDataQueue = ctx.getBean("marketDataQueue", Queue.class);
        Queue responseQueue = ctx.getBean("responseQueue", Queue.class);
        Queue queue = new Queue("stocks.nasdaq.*");
        queue.setDurable(true);
        queue.setAutoDelete(false);
        
        rabbitGateway.createQueue(queue.getName());

        int numMessages = 100;

        ProducerSample producer = new ProducerSample(rabbitTemplate, queue, numMessages);
        ConsumerSample consumer = new ConsumerSample(rabbitTemplate, marketDataQueue);
        producer.sendMessages();
 
        List<QueueInfo> queues = rabbitGateway.getQueues();

        consumer.receiveAsync();

    }

    private static void addData(ConfigurableApplicationContext ctx) throws Exception {
        SingleConnectionFactory singleConnectionFactory = ctx.getBean(SingleConnectionFactory.class);
        RabbitGateway rabbitGateway = ctx.getBean(RabbitGateway.class);

        rabbitGateway.createQueue("quotes.nasdaq.*");
        List<QueueInfo> queues = rabbitGateway.getQueues();
      
        rabbitGateway.createExchange("marketData.topic", ExchangeTypes.TOPIC);
        rabbitGateway.createExchange("app.stock.marketdata", ExchangeTypes.DIRECT);

        Queue marketDataQueue = ctx.getBean("marketDataQueue", Queue.class);

        rabbitGateway.createExchange("app.stock.quotes", ExchangeTypes.TOPIC);
        rabbitGateway.createQueue(marketDataQueue.getName());

        RabbitBrokerAdmin rabbitBrokerAdmin = ctx.getBean(RabbitBrokerAdmin.class);
        System.out.println(rabbitBrokerAdmin.getStatus());

        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        Map<String, Object> props = conn.getServerProperties();
        System.out.println(props);


        AMQPStatus status = rabbitGateway.createQueue(UUID.randomUUID().toString());
        assertTrue(status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0);
        assertTrue(status.name().equalsIgnoreCase(AMQPStatus.RESOURCE_CREATED.name()));

        conn.createChannel();
        conn.createChannel();

        List<HypericChannel> channels = rabbitGateway.getChannels();
        assertNotNull(channels);
        assertTrue(channels.size() > 0);

        Thread.sleep(10000);

        List<String> users = rabbitGateway.getUsers();
        if (users.contains("foo")) {
            rabbitGateway.deleteUser("foo");
        }
        /** kept it open for a while to show some metrics */
        conn.close();
    }
}
