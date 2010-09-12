package org.hyperic.hq.plugin.rabbitmq;


import org.hyperic.hq.product.PluginException;
import org.junit.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import org.springframework.erlang.OtpIOException;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.junit.Assert.*;

/**
 * BrokerAdminTest
 *
 * @author Helena Edelson
 */
@ContextConfiguration("classpath:/etc/rabbit-test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Need to set up a rabbit server for QA")
public class BrokerAdminTest {

    protected static final Log logger = LogFactory.getLog(BrokerAdminTest.class);

    @Autowired
    private RabbitBrokerAdmin rabbitBrokerAdmin;

    @Autowired
    private SingleConnectionFactory singleConnectionFactory;

    @Autowired
    private Queue sampleQueue;

    @Autowired
    private TopicExchange sampleExchange;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Before
    public void before() {
        /** todo if we fail prior to getting here there is no rabbit server running that we are trying to connect to. */
        assertNotNull("rabbitConnectionFactory should not be null", singleConnectionFactory);
        assertNotNull("rabbitBrokerAdmin should not be null", rabbitBrokerAdmin);
        RabbitStatus status = rabbitBrokerAdmin.getStatus();
        assertNotNull(status);

    }


    @Test
    public void getHosts() throws PluginException {
        String host = rabbitTemplate.getConnectionFactory().getHost();
        String virtualHost = rabbitTemplate.getConnectionFactory().getVirtualHost();
        assertNotNull(host);
        assertNotNull(virtualHost);
    }

    @Test
    //${hyperic.rabbit.exchange}
    public void declareDeleteExchange() {
        rabbitBrokerAdmin.declareExchange("stocks.nasdaq.*", ExchangeType.fanout.name(), true, false);
        rabbitBrokerAdmin.deleteExchange("stocks.nasdaq.*");
    }
 

    @Test
    public void Binding() {
        rabbitBrokerAdmin.declareBinding(BindingBuilder.from(
                sampleQueue).to(sampleExchange).with(sampleQueue.getName()));
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
