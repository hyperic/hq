package org.hyperic.hq.plugin.rabbitmq.core;


import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.junit.*;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
  
import static org.junit.Assert.*;

/**
 * BrokerAdminTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class BrokerAdminTest extends AbstractSpringTest {

    private HypericRabbitAdmin rabbitAdmin;

    @Before
    public void doBefore() {
        this.rabbitAdmin = configurationManager.getVirtualHostForNode(configuration.getDefaultVirtualHost(), configuration.getNodename());
    }

    @Test
    public void declareDeleteExchange() {
        /*rabbitAdmin.declareExchange("stocks.nasdaq.*", ExchangeTypes.FANOUT, true, false);
        rabbitAdmin.deleteExchange("stocks.nasdaq.*");*/
    }


    @Test
    public void Binding() {
        /*Queue queue = new Queue(UUID.randomUUID().toString());
        TopicExchange exchange = new TopicExchange("stocks.nasdaq.*");

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(exchange);

        rabbitAdmin.declareBinding(BindingBuilder.from(queue).to(exchange).with(queue.getName()));*/
    }

    @Test
    public void listCreateDeletePurgeQueue() {
        /*Queue queue = new Queue(UUID.randomUUID().toString());
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(new FanoutExchange("newFanout"));

        List<QueueInfo> queues = rabbitBrokerAdmin.getQueues();
        assertNotNull(queues);
        assertTrue(queues.size() > 0);
        Map<String, QueueInfo> map = new HashMap<String, QueueInfo>();
        for (QueueInfo qi : queues) {
            map.put(qi.getName(), qi);
        }
        assertTrue(map.containsKey(queue.getName()));*/
        /** hangs forever */
        //rabbitAdmin.purgeQueue(queue.getName(), true);
    }

//    @Test
//    public void stopStartBrokerApplication() {
//        RabbitStatus status = rabbitAdmin.getStatus();
//        assertBrokerAppRunning(status);
//
//        /*   rabbitAdmin.stopBrokerApplication();
//                status = rabbitAdmin.getStatus();
//                assertEquals(0, status.getRunningNodes().size());
//        */
//        /*rabbitAdmin.startBrokerApplication();
//        status = rabbitAdmin.getStatus();
//        assertBrokerAppRunning(status);*/
//    }

    @Test
    public void listCreateDeleteChangePwdUser() {
        /*List<String> users = rabbitAdmin.listUsers();
        if (users.contains("foo")) {
            rabbitAdmin.deleteUser("foo");
        }
        rabbitAdmin.addUser("foo", "bar");
        rabbitAdmin.changeUserPassword("foo", "12345");
        users = rabbitAdmin.listUsers();
        if (users.contains("foo")) {
            rabbitAdmin.deleteUser("foo");
        }*/
    }

    @Test
    @Ignore("NEEDS RABBITMQ_HOME to be set and needs additional node running handling/timing")
    public void startStopRabbitNode() {
        /*try {
            rabbitAdmin.stopNode();
        } catch (OtpIOException e) {
            //assume it is not running.
        }
        rabbitAdmin.startNode();
        assertEquals(1, 1);*/
    }

    @Test
    public void getCreateDeleteVirtualHost() {
        /*assertEquals("/", singleConnectionFactory.getVirtualHost());
        int status = rabbitAdmin.addVhost("newVHost");
        rabbitAdmin.deleteVhost("newVHost");*/
    }

    private boolean isBrokerAppRunning(RabbitStatus status) {
        return status.getRunningNodes().size() == 1;
    }

    private void assertBrokerAppRunning(RabbitStatus status) {
        assertEquals(1, status.getRunningNodes().size());
        assertTrue(status.getRunningNodes().get(0).getName().contains("rabbit"));
    }

}
