package org.hyperic.hq.amqp.ping;

import com.rabbitmq.client.QueueingConsumer;

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
        channel.basicPublish(serverExchange, routingKey, null, "agent:ping-request".getBytes());
        
        QueueingConsumer agentConsumer = new QueueingConsumer(channel);
        channel.basicConsume(agentQueue, true, agentConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery = agentConsumer.nextDelivery();
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
