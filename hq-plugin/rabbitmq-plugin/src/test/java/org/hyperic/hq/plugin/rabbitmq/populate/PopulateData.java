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
import org.hyperic.hq.plugin.rabbitmq.core.HypericChannel;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

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
        RabbitGateway rabbitGateway = ctx.getBean(RabbitGateway.class);
        RabbitTemplate rabbitTemplate = ctx.getBean(RabbitTemplate.class);
        RabbitBrokerAdmin rabbitBrokerAdmin = ctx.getBean(RabbitBrokerAdmin.class);
        System.out.println(rabbitBrokerAdmin.getStatus());

        addData(ctx);

        int numMessages = 10;

        TopicExchange marketDataExchange = ctx.getBean("marketDataExchange", TopicExchange.class);

        rabbitTemplate.setExchange(marketDataExchange.getName());
        rabbitTemplate.setRoutingKey("stocks.nasdaq.*");
        ProducerSample producer = new ProducerSample(rabbitTemplate, numMessages);
        producer.sendMessages();
 
        List<QueueInfo> queues = rabbitGateway.getQueues("/");
        /* comparing...
        if (queues != null) {
            System.out.println("queues has " + queues.size());
            for (QueueInfo q : queues) {
                System.out.println("queues=" + q);
            }
        }

        List<QueueInfo> queues2 = rabbitBrokerAdmin.getQueues();
        if (queues2 != null) {
            System.out.println("queues2 has " + queues2.size());
            for (QueueInfo q : queues2) {
                System.out.println("queues2=" + q);
            }
        }*/

        /*ConsumerSample consumer = new ConsumerSample(rabbitTemplate, numMessages);
        consumer.receiveSync(queues);
        */
        System.exit(0);
    }

    private static void addData(ConfigurableApplicationContext ctx) throws Exception {
        SingleConnectionFactory singleConnectionFactory = ctx.getBean(SingleConnectionFactory.class);
        String vHost = singleConnectionFactory.getVirtualHost();
        RabbitBrokerAdmin rabbitBrokerAdmin = ctx.getBean(RabbitBrokerAdmin.class);
        RabbitGateway rabbitGateway = ctx.getBean(RabbitGateway.class);
        RabbitManager rabbitManager = ctx.getBean(RabbitManager.class);

        TopicExchange marketDataExchange = ctx.getBean("marketDataExchange", TopicExchange.class);
        rabbitBrokerAdmin.declareExchange(marketDataExchange);

        Queue marketDataQueue = ctx.getBean("marketDataQueue", Queue.class);
        rabbitBrokerAdmin.declareQueue(marketDataQueue);

        Binding marketDataBinding = ctx.getBean("marketDataBinding", Binding.class);
        rabbitBrokerAdmin.declareBinding(marketDataBinding);

        String dev = "development";
        rabbitBrokerAdmin.addVhost("development");

        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        Map<String, Object> props = conn.getServerProperties();
        System.out.println(props);

        conn.createChannel();
        conn.createChannel();

        List<HypericChannel> channels = rabbitGateway.getChannels(vHost);
        assertNotNull(channels);
        assertTrue(channels.size() > 0);

        /** kept it open for a while to show some metrics */
        conn.close();
    }
      
}
