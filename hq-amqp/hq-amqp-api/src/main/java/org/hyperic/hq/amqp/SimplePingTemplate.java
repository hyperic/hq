package org.hyperic.hq.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.amqp.core.SingleConnectionFactory;
import org.hyperic.hq.amqp.util.Operations;

import java.io.IOException;

/**
 * Prototype only.
 * @author Helena Edelson
 */
public class SimplePingTemplate {

    private final Log logger = LogFactory.getLog(SimplePingTemplate.class);

    protected Channel channel;

    protected final String exchangeType = "direct";

    protected final String routingKey = "ping";

    protected final String agentExchange = "agent";

    protected final String serverExchange = "server";

    protected String serverQueue;

    protected String agentQueue;

    private final Object monitor = new Object();

    public SimplePingTemplate() {
        try {
            this.channel = new SingleConnectionFactory().newConnection().createChannel();
            System.out.println(channel);
 
            channel.exchangeDeclare(agentExchange, exchangeType, false);
            this.agentQueue = channel.queueDeclare().getQueue();
            channel.queueBind(agentQueue, agentExchange, routingKey);

            channel.exchangeDeclare(serverExchange, exchangeType, false);
            this.serverQueue = channel.queueDeclare().getQueue();
            channel.queueBind(serverQueue, serverExchange, routingKey);
        } catch (IOException e) {
            logger.info(e);
        }
    }

    public long agentPing() throws IOException, InterruptedException {
        synchronized (monitor) {

            long startTime = System.currentTimeMillis();
            channel.basicPublish(serverExchange, routingKey, null, Operations.AGENT_PING_REQUEST.getBytes());

            QueueingConsumer agentConsumer = new QueueingConsumer(channel);
            channel.basicConsume(agentQueue, true, agentConsumer);

            while (true) {
                QueueingConsumer.Delivery delivery = agentConsumer.nextDelivery();
                String message = new String(delivery.getBody());
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("***agent received=" + message);
                logger.info("***agent received=" + message);
                if (message.length() > 0 && message.contains(Operations.AGENT_PING_RESPONSE)) {
                    return duration;
                }
            }
        }
    }

    public long ping() throws IOException, InterruptedException {
        synchronized (monitor) {
            long startTime = System.currentTimeMillis();
            channel.basicPublish(serverExchange, routingKey, null, Operations.AGENT_PING_REQUEST.getBytes());

            QueueingConsumer agentConsumer = new QueueingConsumer(channel);
            channel.basicConsume(agentQueue, true, agentConsumer);

            while (true) {
                QueueingConsumer.Delivery delivery = agentConsumer.nextDelivery();
                String message = new String(delivery.getBody());
                long duration = System.currentTimeMillis() - startTime;
                logger.info("***********agent received=" + message);
                if (message.length() > 0 && message.contains(Operations.AGENT_PING_RESPONSE)) {
                    return duration;
                }
            }
        }
    }
 
    public void shutdown() throws IOException {
        this.channel.close();
    }
}
