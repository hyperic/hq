package org.hyperic.hq.plugin.rabbitmq;


import org.hyperic.hq.product.PluginException;
import org.junit.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.erlang.OtpIOException;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitStatus;

import java.util.*;

import static org.junit.Assert.*;

/**
 * BrokerAdminTest
 *
 * @author Helena Edelson
 */
@Ignore("Manual cookie value to connect to each node is required")
public class BrokerAdminTest extends AbstractSpringTest {
 
    @Test
    public void getHosts() throws PluginException {
        String host = rabbitTemplate.getConnectionFactory().getHost();
        String virtualHost = rabbitTemplate.getConnectionFactory().getVirtualHost();
        assertNotNull(host);
        assertNotNull(virtualHost);
    }

    @Test
    public void declareDeleteExchange() {
        rabbitBrokerAdmin.declareExchange("stocks.nasdaq.*", ExchangeType.fanout.name(), true, false);
        rabbitBrokerAdmin.deleteExchange("stocks.nasdaq.*");
    }
 

    @Test
    public void Binding() {
        Queue queue = new Queue(UUID.randomUUID().toString());
        TopicExchange exchange = new TopicExchange("stocks.nasdaq.*");

        rabbitBrokerAdmin.declareQueue(queue);
        rabbitBrokerAdmin.declareExchange(exchange);

        rabbitBrokerAdmin.declareBinding(BindingBuilder.from(queue).to(exchange).with(queue.getName()));
    }

    @Test
    public void listCreateDeletePurgeQueue() {
        Queue queue = new Queue(UUID.randomUUID().toString());
        rabbitBrokerAdmin.declareQueue(queue);
        rabbitBrokerAdmin.declareExchange(new FanoutExchange("newFanout"));

        List<QueueInfo> queues = rabbitBrokerAdmin.getQueues();
        assertNotNull(queues);
        assertTrue(queues.size() > 0);
        Map<String, QueueInfo> map = new HashMap<String, QueueInfo>();
        for (QueueInfo qi : queues) {
            map.put(qi.getName(), qi);
        }
        assertTrue(map.containsKey(queue.getName()));
        /** hangs forever */
        //rabbitBrokerAdmin.purgeQueue(queue.getName(), true);
    }

    @Test
    public void stopStartBrokerApplication() {
        RabbitStatus status = rabbitBrokerAdmin.getStatus();
        assertBrokerAppRunning(status);

        /*   rabbitBrokerAdmin.stopBrokerApplication();
                status = rabbitBrokerAdmin.getStatus();
                assertEquals(0, status.getRunningNodes().size());
        */
        rabbitBrokerAdmin.startBrokerApplication();
        status = rabbitBrokerAdmin.getStatus();
        assertBrokerAppRunning(status);
    }

    @Test
    public void listCreateDeleteChangePwdUser() {
        List<String> users = rabbitBrokerAdmin.listUsers();
        if (users.contains("foo")) {
            rabbitBrokerAdmin.deleteUser("foo");
        }
        rabbitBrokerAdmin.addUser("foo", "bar");
        rabbitBrokerAdmin.changeUserPassword("foo", "12345");
        users = rabbitBrokerAdmin.listUsers();
        if (users.contains("foo")) {
            rabbitBrokerAdmin.deleteUser("foo");
        }
    }

    @Test
    @Ignore("NEEDS RABBITMQ_HOME to be set and needs additional node running handling/timing")
    public void startStopRabbitNode() {
        try {
            rabbitBrokerAdmin.stopNode();
        } catch (OtpIOException e) {
            //assume it is not running.
        }
        rabbitBrokerAdmin.startNode();
        assertEquals(1, 1);
    }

    @Test
    public void getCreateDeleteVirtualHost() {
        assertEquals("/", singleConnectionFactory.getVirtualHost());
        int status = rabbitBrokerAdmin.addVhost("newVHost");
        rabbitBrokerAdmin.deleteVhost("newVHost");
    }

    private boolean isBrokerAppRunning(RabbitStatus status) {
        return status.getRunningNodes().size() == 1;
    }

    private void assertBrokerAppRunning(RabbitStatus status) {
        assertEquals(1, status.getRunningNodes().size());
        assertTrue(status.getRunningNodes().get(0).getName().contains("rabbit"));
    }
 
}
