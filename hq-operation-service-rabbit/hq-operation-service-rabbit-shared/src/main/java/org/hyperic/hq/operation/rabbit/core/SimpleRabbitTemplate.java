package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.convert.Converter;
import org.hyperic.hq.operation.rabbit.convert.SimpleConverter;
import org.hyperic.hq.operation.rabbit.mapping.Operations;
import org.hyperic.hq.operation.rabbit.mapping.Routings;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class SimpleRabbitTemplate implements RabbitTemplate {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private final ConnectionFactory connectionFactory;

    private Converter converter;

    private final ChannelTemplate channelTemplate;

    protected Channel channel;

    private final String exchangeName;

    protected final String routingKey = "ping";

    protected final String agentExchange = "agent";

    protected final String serverExchange = "server";

    protected String serverQueue;

    protected String agentQueue;

    protected QueueingConsumer queueingConsumer;

    private final Object monitor = new Object();

    public SimpleRabbitTemplate() {
        this(new SingleConnectionFactory());
    }

    public SimpleRabbitTemplate(String username, String password) {
        this(new SingleConnectionFactory(username, password));
    }

    public SimpleRabbitTemplate(ConnectionFactory connectionFactory) {
        this(connectionFactory, "", new SimpleConverter());
    }

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory Used to create a connection to send messages on
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     *
     */
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory, String exchangeName, Converter converter) {
        this.connectionFactory = connectionFactory;
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.converter = converter;
        this.exchangeName = exchangeName;
        initialiseTemporaryTest(connectionFactory); 
    }

    public void send(String routingKey, Object message) throws IOException {
        /* temporary */
        if (message instanceof String) {
            synchronized (this.monitor) {
                this.channel.basicPublish(this.exchangeName, routingKey, MessageConstants.MESSAGE_PROPERTIES, ((String) message).getBytes());
            }
        }
        /* TODO - JSON JIRA ticket
        byte[] bytes = this.converter.write(message).getBytes(MessageConstants.CHARSET);
        synchronized (this.monitor) {
            this.channel.basicPublish(this.exchangeName, routingKey, MESSAGE_PROPERTIES, bytes);
        }*/
    }

    private void initialiseTemporaryTest(ConnectionFactory connectionFactory) {
        try {
            this.channel = connectionFactory.newConnection().createChannel();

            channel.exchangeDeclare(agentExchange, Routings.SHARED_EXCHANGE_TYPE, false);
            this.agentQueue = channel.queueDeclare().getQueue();
            channel.queueBind(agentQueue, agentExchange, routingKey);

            channel.exchangeDeclare(serverExchange, Routings.SHARED_EXCHANGE_TYPE, false);
            this.serverQueue = channel.queueDeclare().getQueue();
            channel.queueBind(serverQueue, serverExchange, routingKey);

            this.queueingConsumer = new QueueingConsumer(channel);
        } catch (IOException e) {
            logger.info(e);
        }
    }
 
    public boolean verifyConnection()  {
        return channelTemplate.validateCredentials();
    }
 
    public void send(String exchangeName, String routingKey, Object message) throws IOException {

    }

     // TODO finish
    public Object sendAndReceive(String exchangeName, String routingKey, String data) throws IOException, InterruptedException {
        channel.basicPublish(serverExchange, routingKey, null, data.getBytes());
        logger.info("sent=" + data);
        channel.basicConsume(agentQueue, false, queueingConsumer);

        while (true) {
            QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            Object response = new String(delivery.getBody());
            logger.info("received=" + response);
            if (response != null) {
                return response;
            }
        }
    }

    /* If using QueueingConsumer in auto-ack mode it will accept messages from the
       broker and store them in memory on the client. */
    // TODO remove
    public void timedTest(int append) throws IOException, InterruptedException {
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

    public void shutdown() {
        synchronized (this.monitor) {
            try {
                this.channel.close();
            } catch (IOException e) {
                this.logger.error(e);
            }
        }
    }

}
