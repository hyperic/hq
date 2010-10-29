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

import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.configure.ConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitTestConfiguration;
import org.springframework.amqp.core.Queue;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * PopulateData can be run to populate the QA rabbitmq servers.
 * I set up a synchronous consumer for QA because having the
 * async consumer up in the context for all tests is not desirable.
 * @author Helena Edelson
 */
public class PopulateData extends AbstractSpringTest {

    private static ConfigurationManager configurationManager;

    private static Configuration key;

    private static int numMessages = 500;

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(RabbitTestConfiguration.class);
        ctx.refresh();

        key = ctx.getBean(Configuration.class);
        final List<Queue> queues = ctx.getBean(List.class);
        configurationManager = ctx.getBean(ConfigurationManager.class);

        HypericRabbitAdmin rabbitAdmin = configurationManager.getVirtualHostForNode(key.getDefaultVirtualHost(), key.getNodename());
        final RabbitTemplate rabbitTemplate = configurationManager.getRabbitTemplate();

        if (rabbitAdmin.getQueues() == null && queues != null) {
            createQueues(rabbitAdmin, queues);
        }

        for (Queue q : queues) {
            rabbitTemplate.setRoutingKey(q.getName());
            rabbitTemplate.setQueue(q.getName());
            ProducerSample producer = new ProducerSample(rabbitTemplate, numMessages);
            producer.sendMessages();
        }

        final List<QueueInfo> brokerQueues = rabbitAdmin.getQueues();

        /** rerun with synchronous consumer for each */
        List<Thread> threads = new ArrayList<Thread>();

        Thread thread1 = new Thread(
                new Runnable() {
                    public void run() {
                        try {
                            simulateCollection();
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                });

        Thread thread2 = new Thread(
                new Runnable() {
                    public void run() {
                        for (Queue q : queues) {
                            rabbitTemplate.setRoutingKey(q.getName());
                            rabbitTemplate.setQueue(q.getName());
                            ConsumerSample consumer = new ConsumerSample(rabbitTemplate, numMessages);
                            consumer.receiveSync(brokerQueues);
                            ProducerSample producer = new ProducerSample(rabbitTemplate, 100);
                            producer.sendMessages();

                        }
                    }
                });

        threads.add(thread1);
        threads.add(thread2);
        thread1.start();
        thread2.start();

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }

    private static void createQueues(HypericRabbitAdmin rabbitAdmin, List<Queue> queues) {
        System.out.println("There are no queues, creating queues and declaring them in the broker...");
        for (Queue q : queues) {
            rabbitAdmin.declareQueue(q);
        }
    }

    /**
     * @throws Exception
     */
    private static void simulateCollection() throws Exception {
        com.rabbitmq.client.Connection conn = configurationManager.getConnectionFactory().createConnection();
        System.out.println("ConnectionProperties = " + conn.getServerProperties());

        conn.createChannel();
        conn.createChannel();

        HypericRabbitAdmin admin = configurationManager.getVirtualHostForNode(key.getDefaultVirtualHost(), key.getNodename());
        System.out.println("Queues = " + admin.getQueues().size());
        System.out.println("Exchanges = " + admin.getExchanges().size());
        System.out.println("Connections = " + admin.getConnections().size());
        System.out.println("Channels = " + admin.getChannels().size());
        System.out.println("Broker Info = " + admin.getStatus());

        conn.close();
    }

}
