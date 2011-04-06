package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.AbstractOperation;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.connection.ConnectionException;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.Routings;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.io.IOException;
import java.util.Random;

/**
 * @author Helena Edelson
 */
public class SimpleRabbitTemplate implements RabbitTemplate {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private final Converter<Object, String> converter;

    private final ChannelTemplate channelTemplate;

    private final Routings routings;

    private final Object monitor = new Object();

    protected final boolean usesNonGuestCredentials;

    protected Channel channel;

    private final String exchangeName;

    protected String serverQueue;

    protected String agentQueue;

    protected String defaultCredential = "guest";

    protected QueueingConsumer queueingConsumer;

    /**
     * Creates an instance with the default SingleConnectionFactory
     * and guest credentials
     */
    public SimpleRabbitTemplate() {
        this(new SingleConnectionFactory());
    }

    /**
     * Creates an instance
     * @param connectionFactory Used to create a connection to send messages on
     */
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory) {
        this(connectionFactory, null);
    }

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param connectionFactory Used to create a connection to send messages on
     * @param exchangeName      The exchange name to use. May be null.
     */
    public SimpleRabbitTemplate(ConnectionFactory connectionFactory, String exchangeName) {
        this(connectionFactory, null, exchangeName);
    }

    /**
     * Creates a new instance that creates a connection and sends messages to a specific exchange
     * @param cf           ConnectionFactory used to create a connection
     * @param serverId     If a serverId exists it is used to initialize org.hyperic.hq.operation.rabbit.mapping.Routings
     *                     and if not, a default server id is generated to initialize org.hyperic.hq.operation.rabbit.mapping.Routings.
     * @param exchangeName The exchange name to use. if null, uses the AMQP default
     * @see org.hyperic.hq.operation.rabbit.util.Routings
     */
    public SimpleRabbitTemplate(ConnectionFactory cf, String exchangeName, String serverId) {
        this.channelTemplate = new ChannelTemplate(cf);
        this.converter = new JsonMappingConverter();
        this.routings = serverId != null ? new Routings(serverId) : new Routings();
        this.exchangeName = exchangeName != null ? exchangeName : "";
        this.usesNonGuestCredentials = !cf.getUsername().equals(defaultCredential) && !cf.getPassword().equals(defaultCredential);
        initialize(cf);
    }

    /**
     * Sends a message
     * @param routingKey The routing key to use
     * @param data       The data to send
     * @throws java.io.IOException
     */
    public void send(String routingKey, Object data) throws IOException {
        send(this.exchangeName, routingKey, data);
    }

    public void send(String exchangeName, String routingKey, Object data) throws IOException {
        publish(exchangeName, routingKey, data);
    }

    private void publish(String exchangeName, String routingKey, Object data) throws IOException {
        byte[] bytes = this.converter.write(data).getBytes(MessageConstants.CHARSET);

        synchronized (this.monitor) {
            this.channel.basicPublish(exchangeName, routingKey, MessageConstants.DEFAULT_MESSAGE_PROPERTIES, bytes);
            logger.debug("sent=" + data);
        }
    }

    /**
     * Because autoAck = false call Channel.basicAck to acknowledge receipt
     * @param exchangeName the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     * @return the response object
     * @throws IOException
     */
    public Object sendAndReceive(String exchangeName, String routingKey, Object data) throws IOException {
        publish(exchangeName, routingKey, data);

        AMQP.BasicProperties bp = getBasicProperties(data);
        String correlationId = bp.getCorrelationId();

        synchronized (monitor) {
            while (true) {
                GetResponse response = channel.basicGet(agentQueue, false);
                if (response != null && response.getProps().getCorrelationId().equals(correlationId)) {
                    this.logger.debug("received=" + response);
                    Object received = this.converter.read(new String(response.getBody()), Object.class);
                    this.channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    return received;
                }
            }
        }
    }

    /**
     * Creates the default message properties and sets a correlationId
     * @param data the object to pull context from
     * @return BasicProperties with a correlationid
     */
    protected AMQP.BasicProperties getBasicProperties(Object data) {
        AMQP.BasicProperties bp = MessageConstants.DEFAULT_MESSAGE_PROPERTIES;

        if (data.getClass().isAssignableFrom(AbstractOperation.class)) {
            bp.setCorrelationId(((AbstractOperation) data).getOperationName());
        } else {
            bp.setCorrelationId(new Random().toString());
        }
        return bp;
    }

    /**
     * Tests for valid broker configuration, if the client
     * can connect to a node, and if the node or server is down.
     * @return
     */
    public boolean hasValidConfigurations() {
        return channelTemplate.validateConnection();
    }


    // TODO remove

    public void timedTest(int append) throws IOException, InterruptedException {
        String msg = "test-" + append;
        byte[] bytes = this.converter.write(msg).getBytes(MessageConstants.CHARSET);
        AMQP.BasicProperties bp = getBasicProperties(msg);
        String correlationId = bp.getCorrelationId();

        synchronized (monitor) {
            this.channel.basicPublish(routings.getToAgentExchange(), "test", bp, bytes);
            while (true) {
                GetResponse response = channel.basicGet(agentQueue, false);
                if (response != null && response.getProps().getCorrelationId().equals(correlationId)) {
                    //this.logger.debug("received=" + this.converter.toObject(new String(response.getBody()), Object.class));
                    break;
                }
            }
        }
    }

    /**
     * TODO automate given agent
     */
    public void shutdown() {
        synchronized (this.monitor) {
            try {
                this.channel.close();
            } catch (IOException e) {
                this.logger.error(e);
            }
        }
    }

    /**
     * TODO routing key
     * Initializes a temporary test with the 2 exchanges for unauthenticated agents only
     * @param connectionFactory The connection factory to use
     */
    private void initialize(ConnectionFactory connectionFactory) {
        try {
            /* assert the state of configuration is correct. If so proceed */
            if (hasValidConfigurations()) {
                this.channel = connectionFactory.newConnection().createChannel();

                if (channel != null) {
                    channel.exchangeDeclare(routings.getToServerUnauthenticatedExchange(), routings.getSharedExchangeType(), true);
                    this.agentQueue = channel.queueDeclare().getQueue();
                    channel.queueBind(agentQueue, routings.getToServerExchange(), "test");

                    channel.exchangeDeclare(routings.getToAgentUnauthenticatedExchange(), routings.getSharedExchangeType(), true);
                    this.serverQueue = channel.queueDeclare().getQueue();
                    channel.queueBind(serverQueue, routings.getToAgentExchange(), "test");

                    this.queueingConsumer = new QueueingConsumer(channel);
                }
            } else {
                throw new ConnectionException("");
            }

        } catch (IOException e) {
            throw new ConnectionException("Error declaring and binding exchanges and queues.", e);
        }
    }
}
