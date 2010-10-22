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
import org.springframework.amqp.core.Queue;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
 
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * PopulateData can be run to populate the QA rabbitmq servers
 * Not finished yet.
 * @author Helena Edelson
 */
public class PopulateData {

    private static final String SERVER_NAME = "rabbit@server";

    private static final String HOST = "server";


    public static void main(String[] args) throws Exception {
        Configuration configuration = getConfig();
        configuration.setVirtualHost("/");
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway(configuration);
        RabbitTemplate rabbitTemplate = rabbitGateway.getRabbitTemplate();
        RabbitBrokerAdmin rabbitBrokerAdmin = rabbitGateway.getRabbitBrokerAdmin();
        System.out.println(rabbitBrokerAdmin.getStatus());

        int numMessages = 500;

        final Queue stocksQueue = new Queue("stocks.quotes");
        rabbitBrokerAdmin.declareQueue(stocksQueue);
        rabbitTemplate.setRoutingKey(stocksQueue.getName());
        rabbitTemplate.setQueue(stocksQueue.getName());
        ProducerSample producer = new ProducerSample(rabbitTemplate, numMessages);
        producer.sendMessages();
 
        Queue alertsQueue = new Queue("market.alerts");
        rabbitBrokerAdmin.declareQueue(alertsQueue);
        rabbitTemplate.setRoutingKey(alertsQueue.getName());
        rabbitTemplate.setQueue(alertsQueue.getName());
        ProducerSample producer2 = new ProducerSample(rabbitTemplate, numMessages);
        producer2.sendMessages();


        Queue trendsQueue = new Queue("market.trends");
        rabbitBrokerAdmin.declareQueue(trendsQueue);
        rabbitTemplate.setRoutingKey(trendsQueue.getName());
        rabbitTemplate.setQueue(trendsQueue.getName());
        ProducerSample producer3 = new ProducerSample(rabbitTemplate, numMessages);
        producer3.sendMessages();

        refresh((CachingConnectionFactory) rabbitTemplate.getConnectionFactory(), rabbitGateway);

        /*List<QueueInfo> queues = rabbitGateway.getQueues();
        if (queues != null) {
            System.out.println("queues has " + queues.size());
            for (QueueInfo q : queues) {
                System.out.println(q);
            }
        }*/

        System.exit(0);
    }

    /**
     * @param scf
     * @param rabbitGateway
     * @throws Exception
     */
    private static void refresh(CachingConnectionFactory scf, RabbitGateway rabbitGateway) throws Exception {
        com.rabbitmq.client.Connection conn = scf.createConnection();
        Map<String, Object> props = conn.getServerProperties();
        System.out.println(props);

        conn.createChannel();
        conn.createChannel();

        List<RabbitChannel> channels = rabbitGateway.getChannels();
        assertNotNull(channels);

        List<RabbitConnection> connections = rabbitGateway.getConnections();
        assertNotNull(connections);

        /** kept it open for a while to show some metrics */
        conn.close();
    }

    private static Configuration getConfig() throws PluginException {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, HOST);
        conf.setValue(DetectorConstants.USERNAME, "guest");
        conf.setValue(DetectorConstants.PASSWORD, "guest");
        conf.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
        conf.setValue(DetectorConstants.SERVER_NAME, SERVER_NAME);

        String value = ErlangCookieHandler.configureCookie(conf);
        assertNotNull(value);
        conf.setValue(DetectorConstants.AUTHENTICATION, value);
        return Configuration.toConfiguration(conf);
    }
}
