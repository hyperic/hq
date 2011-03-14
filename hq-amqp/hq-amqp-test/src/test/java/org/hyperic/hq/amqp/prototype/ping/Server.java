package org.hyperic.hq.amqp.prototype.ping;

import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class Server extends AbstractAmqpComponent implements Ping {

    public Server() throws IOException {
        super();
    }

    public void listen() throws IOException, InterruptedException {
        channel.basicConsume(serverQueue, true, queueingConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            if (message.length() > 0 && message.contains("agent:ping-request")) {
                channel.basicPublish(agentExchange, routingKey, null, "agent:ping-response".getBytes());
                System.out.println("server received=" + message);
                shutdown();
                break;
            }
        }
    }
    
    @Override
    public long ping(int attempts) throws IOException, InterruptedException {
        return 0;
    }
}
