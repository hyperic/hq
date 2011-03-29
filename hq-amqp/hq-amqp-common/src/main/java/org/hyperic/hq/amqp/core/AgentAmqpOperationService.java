package org.hyperic.hq.amqp.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.hyperic.hq.amqp.util.Operations;

import java.io.IOException;

/**
 * Prototype only to work around the current communication and api constraints.
 * Starting to migrate from prototype.
 * Dealing with no spring on agent currently.
 * @author Helena Edelson
 */
public class AgentAmqpOperationService extends AmqpOperationService implements OperationService {

    protected Channel channel;

    protected final String exchangeType = "direct";

    protected final String routingKey = "ping";

    protected final String agentExchange = "agent";

    protected final String serverExchange = "server";

    protected String serverQueue;

    protected String agentQueue;

    protected QueueingConsumer queueingConsumer;

    private final Object monitor = new Object();

    public AgentAmqpOperationService() {
        try {
            this.channel = new SingleConnectionFactory().newConnection().createChannel();

            channel.exchangeDeclare(agentExchange, exchangeType, false);
            this.agentQueue = channel.queueDeclare().getQueue();
            channel.queueBind(agentQueue, agentExchange, routingKey);

            channel.exchangeDeclare(serverExchange, exchangeType, false);
            this.serverQueue = channel.queueDeclare().getQueue();
            channel.queueBind(serverQueue, serverExchange, routingKey);

            this.queueingConsumer = new QueueingConsumer(channel);
        } catch (IOException e) {
            logger.info(e);
        }
    }

    /* 
    public AgentAmqpOperationService() {
        super();
    }
    @Autowired
    public AgentAmqpOperationService(@Qualifier("agentRabbitTemplate") RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }*/

    public void send(String message) {
        super.send(serverExchange, routingKey, message);
    }

    public void send(String exchangeName, String routingKey, String message) {
        super.send(exchangeName, routingKey, message);
    }

    public Object sendAndReceive(String message) {
        return super.sendAndReceive(serverExchange, routingKey, message);
    }

    public long agentPing() throws IOException, InterruptedException {
        // synchronized (monitor) {
        channel.basicPublish(serverExchange, routingKey, null, Operations.AGENT_PING_REQUEST.getBytes());
        logger.info("***agent sent=" + Operations.AGENT_PING_REQUEST);
        channel.basicConsume(agentQueue, false, queueingConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            String message = new String(delivery.getBody());
            logger.info("***agent received=" + message);
            return (message.length() > 0 && message.contains(Operations.AGENT_PING_RESPONSE)) ? 1 : 0;
        } //}
    }
    /* If using QueueingConsumer in auto-ack mode it will accept messages from the
       broker and store them in memory on the client. */

    public void timedPing(int append) throws IOException, InterruptedException {
        String send = Operations.AGENT_PING_REQUEST + append;
        channel.basicPublish(serverExchange, routingKey, null, send.getBytes());
        channel.basicConsume(agentQueue, false, queueingConsumer);
        while (true) {
            QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            String message = new String(delivery.getBody());
            if (message.startsWith(Operations.AGENT_PING_RESPONSE))
                break;
        }
    }

    public long ping() throws IOException, InterruptedException {
        synchronized (monitor) {
            long startTime = System.currentTimeMillis();
            channel.basicPublish(serverExchange, routingKey, null, Operations.AGENT_PING_REQUEST.getBytes());
            channel.basicConsume(agentQueue, false, queueingConsumer);

            while (true) {
                QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
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
