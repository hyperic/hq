package org.hyperic.hq.amqp.ping;

import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class Agent extends AbstractAmqpComponent implements Ping {

    private boolean completed;

    public Agent() throws IOException {
        super();
    }

    @Override
    public long ping(int attempts) throws IOException, InterruptedException {
        Thread.sleep(100L);
        long startTime = System.currentTimeMillis();
        channel.basicPublish(serverExchange, routingKey, null, "agent:ping".getBytes());
        System.out.println("agent sent ping to server");

        QueueingConsumer agentConsumer = new QueueingConsumer(channel);
        channel.basicConsume(agentQueue, true, agentConsumer);
        /* needs to block until receives response */
        while (!completed) {
            QueueingConsumer.Delivery delivery = agentConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("agent received=" + message);
            if (message.length() > 0 && message.contains("server:ping")) { 
                completed = true;
                shutdown();
                return duration;
            }
        }
        return 0;
    }
}
