package org.hyperic.hq.amqp.ping;

import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class Server extends AbstractAmqpComponent implements Ping {

    private boolean completed;
    
    public Server() throws IOException {
        super();
    }

    public void listen() throws IOException, InterruptedException {
        System.out.println("Server is running");
        
        QueueingConsumer serverConsumer = new QueueingConsumer(channel);
        channel.basicConsume(serverQueue, true, serverConsumer);

        while (!completed) {
            QueueingConsumer.Delivery delivery = serverConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("server received message=" + message);
            if (message.length() > 0 && message.contains("agent:ping")) {
                channel.basicPublish(agentExchange, routingKey, null, "server:ping".getBytes());
                completed = true;
            }
        }
    }
    
    @Override
    public long ping(int attempts) throws IOException, InterruptedException {
        /*QueueingConsumer serverConsumer = new QueueingConsumer(channel);
        channel.basicConsume(serverQueue, true, serverConsumer);

        while (!completed) {
            QueueingConsumer.Delivery delivery = serverConsumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("server received message=" + message);
            if (message.length() > 0 && message.contains("agent:ping")) {
                channel.basicPublish(agentExchange, routingKey, null, "server:ping".getBytes());
                completed = true;
                return 0;
            }
        }*/
        return 0;
    }
}
