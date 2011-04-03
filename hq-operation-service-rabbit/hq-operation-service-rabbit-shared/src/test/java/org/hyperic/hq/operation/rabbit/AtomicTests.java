package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Helena Edelson
 */
@Ignore
public class AtomicTests {

    private Channel channel;
    private String agentQueue;
    private String serverQueue;

    @Before
    public void prepare() throws IOException {
        this.channel = new SingleConnectionFactory().newConnection().createChannel();
        System.out.println(channel);

        channel.exchangeDeclare("agent", "direct", false);
        this.agentQueue = channel.queueDeclare().getQueue();
        channel.queueBind(agentQueue, "agent", "ping");

        channel.exchangeDeclare("server", "direct", false);
        this.serverQueue = channel.queueDeclare().getQueue();
        channel.queueBind(serverQueue, "server", "ping");
    }

    @After
    public void destroy() throws IOException {
        this.channel.close();
    }

    @Test
    public void basic() throws IOException, InterruptedException {
        int messagesToSend = 11;
        for (int i = 0; i < messagesToSend; i++) {
            channel.basicPublish("server", "ping", null, (i + "-message").getBytes());
        }
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(serverQueue, true, consumer);

        int serverReceived = 0;
        while (serverReceived < messagesToSend) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("received message=" + message);
            serverReceived++;
        }
        System.out.println("finished");
        assertEquals(messagesToSend, serverReceived);
    }

    @Test
    public void basicPing() throws IOException, InterruptedException {
        channel.basicPublish("server", "ping", null, "agent:ping".getBytes());

        QueueingConsumer serverConsumer = new QueueingConsumer(channel);
        channel.basicConsume(serverQueue, true, serverConsumer);

        QueueingConsumer agentConsumer = new QueueingConsumer(channel);
        channel.basicConsume(agentQueue, true, agentConsumer);

        int serverReceived = 0;
        while (serverReceived < 1) {
            QueueingConsumer.Delivery delivery = serverConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("server received message=" + message);
            serverReceived++;
            channel.basicPublish("agent", "ping", null, "server:ping".getBytes());
        }

        int agentReceived = 0;
        while (agentReceived < 1) {
            QueueingConsumer.Delivery delivery = agentConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("agent received message=" + message);
            serverReceived++;
        }
        System.out.println("finished");

        assertEquals(1, serverReceived);
        assertEquals(1, agentReceived);
    }

    @Test
    public void testAtomic1() throws IOException {
        boolean autoAck = false;
        channel.basicPublish("server", "ping", null, "first-message".getBytes());

        QueueingConsumer serverConsumer = new QueueingConsumer(channel);
        /*Start a non-nolocal, non-exclusive consumer, with a server-generated consumerTag.*/
        channel.basicConsume(serverQueue, true, serverConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery;
            try {
                delivery = serverConsumer.nextDelivery();

                String body = new String(delivery.getBody());
                System.out.println("received message=" + body);
            } catch (InterruptedException ie) {
                continue;
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }

    @Test
    public void testAtomic2() throws IOException {
        boolean autoAck = false;
        for (int i = 0; i < 10; i++) {
            channel.basicPublish("server", "ping", null, (i + "-message").getBytes());
            System.out.println("sent message " + i);
        }

        while (true) {
            try {
                System.out.println("listening...");
                GetResponse response = channel.basicGet(serverQueue, true);
                if (response != null) {
                    AMQP.BasicProperties props = response.getProps();
                    String body = new String(response.getBody());
                    System.out.println("received message=" + body);
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false); // acknowledge receipt of the message
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

}