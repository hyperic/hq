package org.hyperic.hq.amqp.ping;

import com.rabbitmq.client.Channel;
import org.hyperic.hq.amqp.core.SingleConnectionFactory;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class AbstractAmqpComponent {

    protected Channel channel;

    protected final String exchangeType = "direct";

    protected final String routingKey = "ping";

    protected final String agentExchange = "agent";

    protected final String serverExchange = "server";

    protected final String serverQueue;

    protected final String agentQueue;

    public AbstractAmqpComponent() throws IOException {
        this.channel = new SingleConnectionFactory().newConnection().createChannel();
        System.out.println(channel);

        channel.exchangeDeclare(agentExchange, exchangeType, false);
        this.agentQueue = channel.queueDeclare().getQueue();
        channel.queueBind(agentQueue, agentExchange, routingKey);

        channel.exchangeDeclare(serverExchange, exchangeType, false);
        this.serverQueue = channel.queueDeclare().getQueue();
        channel.queueBind(serverQueue, serverExchange, routingKey);
    }
 
    public void shutdown() throws IOException {
        this.channel.close();
    }
}
