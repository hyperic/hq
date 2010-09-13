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
package org.hyperic.hq.plugin.rabbitmq;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.AMQPStatus;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo; 
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; 

/**
 * RabbitGatewayTest
 *
 * @author Helena Edelson
 */
@ContextConfiguration("classpath:/etc/rabbit-test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Need to set up a rabbit server for QA")
public class RabbitGatewayTest {

    protected Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private SingleConnectionFactory singleConnectionFactory;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue requestQueue;

    @Autowired
    private Queue responseQueue;

    @Autowired
    private RabbitGateway rabbitGateway;

    @Autowired
    private ErlangGateway erlangGateway;

    private String host;

    private String virtualHost;

    private static final String USERNAME = "guest";

    private static final String PASSWORD = "password";

    @Before
    public void before() {
        assertNotNull("rabbitTemplate must not be null", rabbitTemplate);
        this.host = rabbitTemplate.getConnectionFactory().getHost();
        this.virtualHost = rabbitTemplate.getConnectionFactory().getVirtualHost();
        assertNotNull(rabbitGateway);
        assertNotNull(erlangGateway);
    }

     /*@Test
    public void testPublish()  {
        ConnectionFactoryStub factory = new ConnectionFactoryStub();
        Connection connection = createMock(Connection.class);
        factory.setConnection(connection);
        Channel channel = createMock(Channel.class);
        expect(connection.createChannel()).andReturn(channel);
        // record some more expected calls
        replay(connection, channel);
        // create your object under test and inject the ConnectionFactoryStub
        // invoke the method that you are testing
        verify(connection, channel);
        // make some assertions
    }

    private static class ConnectionFactoryStub extends ConnectionFactory {
        Connection connection;
        public void setConnection(Connection connection) {
            this.connection = connection;
        }
        @Override
        public Connection newConnection() {
            return this.connection;
        }
    }*/

    @Test
    public void getVirtualHosts() throws Exception {
        List<String> virtualHosts = rabbitGateway.getVirtualHosts();
        assertNotNull(virtualHosts);
        assertTrue(virtualHosts.size() >= 0);
    }

    @Test
    public void createQueueResult() {
        AMQPStatus status = rabbitGateway.createQueue(UUID.randomUUID().toString());
        assertTrue(status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0);
        assertTrue(status.name().equalsIgnoreCase(AMQPStatus.RESOURCE_CREATED.name()));
    }

    @Test
    //@Ignore("this is hanging..have to fix")
    public void purgeQueue() {
        List<QueueInfo> queues = rabbitGateway.getQueues();
       
        /*AMQPStatus status = rabbitGateway.purgeQueue(queues.get(0).getName());
        assertTrue(status.compareTo(AMQPStatus.NO_CONTENT) == 0);
        assertTrue(status.name().equalsIgnoreCase(AMQPStatus.NO_CONTENT.name()));*/
    }

    @Test
    public void getHosts() throws PluginException {
        String host = rabbitTemplate.getConnectionFactory().getHost();
        String virtualHost = rabbitTemplate.getConnectionFactory().getVirtualHost();
        assertNotNull(host);
        assertNotNull(virtualHost);
    }

    @Test //finish
    public void declareDeleteExchange() throws Exception {
        rabbitGateway.createExchange("34566", "fanout");
        rabbitGateway.deleteExchange("34566");

    }

    @Test
    public void listCreateDeletePurgeQueue() {
        String queueName = UUID.randomUUID().toString();
        rabbitGateway.createQueue(queueName);

        List<QueueInfo> queues = rabbitGateway.getQueues();
        assertNotNull(queues);

        Map<String, QueueInfo> map = new HashMap<String, QueueInfo>();
        for (QueueInfo qi : queues) {
            map.put(qi.getName(), qi);
        }
        assertTrue(map.containsKey(queueName));
        /** hangs forever */
        //rabbitGateway.purgeQueue(queueName);

        rabbitGateway.deleteQueue(queueName);
    }

    @Test
    public void stopStartBrokerApplication() {
        RabbitStatus status = rabbitGateway.getRabbitStatus();
        assertBrokerAppRunning(status);

        rabbitGateway.stopBrokerApplication();
        status = rabbitGateway.getRabbitStatus();
        assertEquals(0, status.getRunningNodes().size());

        rabbitGateway.startBrokerApplication();
        status = rabbitGateway.getRabbitStatus();
        assertBrokerAppRunning(status);
    }

    @Test
    public void listCreateDeleteChangePwdUser() {
        List<String> users = rabbitGateway.getUsers();
        if (users.contains("foo")) {
            rabbitGateway.deleteUser("foo");
        }
        rabbitGateway.createUser("foo", "bar");
        rabbitGateway.updateUserPassword("foo", "12345");
        users = rabbitGateway.getUsers();
        if (users.contains("foo")) {
            rabbitGateway.deleteUser("foo");
        }
    }

    @Test
    //@Ignore("NEEDS RABBITMQ_HOME to be set and needs additional node running handling/timing")
    public void startStopRabbitNode() {
        //rabbitGateway.stopRabbitNode();
        rabbitGateway.startRabbitNode("/Users/hedelson/tools/rabbitmq_server-1.8.1/");
    }

    private boolean isBrokerAppRunning(RabbitStatus status) {
        return status.getRunningNodes().size() == 1;
    }

    private void assertBrokerAppRunning(RabbitStatus status) {
        assertEquals(1, status.getRunningNodes().size());
        assertTrue(status.getRunningNodes().get(0).getName().contains("rabbit"));
    }

}