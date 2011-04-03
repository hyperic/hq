package org.hyperic.hq.operation.rabbit.demo.ping;

import com.rabbitmq.client.QueueingConsumer;
import org.hyperic.hq.operation.rabbit.demo.ping.AbstractAmqpComponent;
import org.hyperic.hq.operation.rabbit.demo.ping.Ping;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class Agent extends AbstractAmqpComponent implements Ping {

    public Agent() throws IOException {
        super();
    }

    @Override
    public long ping(int attempts) throws IOException, InterruptedException {
        Thread.sleep(100L);
        long startTime = System.currentTimeMillis();
        /*AMQP.BasicProperties props = new AMQP.BasicProperties();
        props.setReplyTo();*/
        channel.basicPublish(serverExchange, routingKey, null, "agent:ping-request".getBytes());

        channel.basicConsume(agentQueue, true, queueingConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("agent received=" + message);
            if (message.length() > 0 && message.contains("agent:ping-response")) {
                shutdown();
                return duration;
            }
        }
    }
}
