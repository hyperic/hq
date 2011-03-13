package org.hyperic.hq.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.log4j.Logger;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactoryUtils;
import org.springframework.amqp.rabbit.connection.RabbitResourceHolder;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.RabbitUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Helena Edelson
 */
public class AmqpOperationService implements OperationService {

    protected Logger logger = Logger.getLogger(this.getClass());

    private static final long DEFAULT_REPLY_TIMEOUT = 5000;

    /**
     * Injection of template with pre-configured exchange and routing key
     */
    private RabbitTemplate rabbitTemplate;

    /**
     * Temporary, for the agent prototype
     */
    public AmqpOperationService() {
        this(new RabbitTemplate(new SingleConnectionFactory()));
    }

    public AmqpOperationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * TODO desc.
     * @param operationName           The name of the operation that should be performed
     * @param nodeId                  The id of the node this operation should be performed on
     * @param context                 The context for the operation being performed
     * @param operationStatusCallback A callback for notification of selected events in the lifecycle of the operation
     * @param <T>
     * @throws RuntimeException
     */
    public <T> void perform(String operationName, long nodeId, Object context, Object operationStatusCallback) throws RuntimeException {

    }

    /**
     * Sends a message with pre-configured routing.
     */
    public void send(String message) {
        rabbitTemplate.convertAndSend(message);
        logger.info("sent message=" + message);
    }

    /**
     * Sends a message with configurable routing.
     */
    public void send(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        logger.info("sent message=" + message);
    }

    public Object sendAndReceive(String exchange, String routingKey, String message) {
        Object response = convertSendAndReceive(exchange, routingKey, message);
        logger.info("sent message=" + message + ", and received response=" + response);
        return response;
    }

    public Object convertSendAndReceive(final String exchange, final String routingKey, final Object message) throws AmqpException {

        MessageProperties messageProperties = new MessageProperties();
        Message requestMessage = rabbitTemplate.getMessageConverter().toMessage(message, messageProperties);
        Message replyMessage = this.doSendAndReceive(exchange, routingKey, requestMessage);
        if (replyMessage == null) {
            return null;
        }
        return this.rabbitTemplate.getMessageConverter().fromMessage(replyMessage);
    }

    private Message doSendAndReceive(final String exchange, final String routingKey, final Message message) {
        Message replyMessage = this.execute(new ChannelCallback<Message>() {
            public Message doInRabbit(Channel channel) throws Exception {

                final SynchronousQueue<Message> replyHandoff = new SynchronousQueue<Message>();

                Address replyToAddress = message.getMessageProperties().getReplyTo();
                AMQP.Queue.DeclareOk queueDeclaration = channel.queueDeclare();
                logger.info("*****queueDeclaration="+queueDeclaration);
                //queueDeclaration=#method<queue.declare-ok>(queue=amq.gen-6lEYDL/hguwvqk/zouPCEQ==,message-count=0,consumer-count=0)

				if (replyToAddress == null) {
                    //replyToAddress = new Address(ExchangeTypes.DIRECT, "", queueDeclaration.getQueue());
                    replyToAddress = new Address(ExchangeTypes.DIRECT, exchange, routingKey);
                }

                logger.info("\n*****replyto: exchange=" + replyToAddress.getExchangeName() + ", routingkey=" + replyToAddress.getRoutingKey());
                boolean noAck = false;
                String consumerTag = UUID.randomUUID().toString();
                boolean noLocal = true;
                boolean exclusive = true;
                DefaultConsumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        MessageProperties messageProperties = RabbitUtils.createMessageProperties(properties, envelope,
                                "UTF-8");
                        Message reply = new Message(body, messageProperties);
                        try {
                            replyHandoff.put(reply);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                };
       //Caused by: com.rabbitmq.client.ShutdownSignalException: channel error; reason: {#method<channel.close>(reply-code=403,reply-text=ACCESS_REFUSED - queue 'queues.agent' in vhost '/' in exclusive use,class-id=60,method-id=20),null,""}

                channel.basicConsume(replyToAddress.getRoutingKey(), noAck, consumerTag, noLocal, exclusive, null,consumer);
                doSend(channel, exchange, routingKey, message);
                Message reply = (DEFAULT_REPLY_TIMEOUT < 0) ? replyHandoff.take() : replyHandoff.poll(DEFAULT_REPLY_TIMEOUT,
                        TimeUnit.MILLISECONDS);
                channel.basicCancel(consumerTag);
                return reply;
            }
        });
        return replyMessage;
    }

    public <T> T execute(ChannelCallback<T> action) {
        Assert.notNull(action, "Callback object must not be null");
        RabbitResourceHolder resourceHolder = ConnectionFactoryUtils.getTransactionalResourceHolder(new SingleConnectionFactory(), false);

        Channel channel = resourceHolder.getChannel();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Executing callback on RabbitMQ Channel: " + channel);
            }
            return action.doInRabbit(channel);
        } catch (Exception ex) {
            throw convertRabbitAccessException(ex);
        } finally {
            ConnectionFactoryUtils.releaseResources(resourceHolder);
        }
    }

    protected AmqpException convertRabbitAccessException(Exception ex) {
        return RabbitUtils.convertRabbitAccessException(ex);
    }

    /**
     * Send the given message to the specified exchange.
     * @param channel    the RabbitMQ Channel to operate within
     * @param exchange   the name of the RabbitMQ exchange to send to
     * @param routingKey the routing key
     * @param message    the Message to send
     * @throws IOException if thrown by RabbitMQ API methods
     */
    private void doSend(Channel channel, String exchange, String routingKey, Message message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Publishing message on exchange [" + exchange + "], routingKey = [" + routingKey + "]");
        }
        channel.basicPublish(exchange, routingKey, false, false, RabbitUtils.extractBasicProperties(message, "UTF-8"),
                message.getBody());

    }

}
