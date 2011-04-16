package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.api.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.ErrorHandler;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class RabbitMessageListenerContainer extends SimpleMessageListenerContainer implements DisposableBean {

    private final Log logger = LogFactory.getLog(RabbitMessageListenerContainer.class);
     
    private final ChannelTemplate channelTemplate;

    private final MessageListenerAdapter messageListener;

    private final Object endpoint;

    public RabbitMessageListenerContainer(ConnectionFactory connectionFactory, Object endpoint,
                                          String operationName, ErrorHandler errorHandler) {
        super(new SingleConnectionFactory(connectionFactory));
        this.endpoint = endpoint;
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.messageListener = new MessageListenerAdapter(endpoint);
        this.messageListener.setDefaultListenerMethod(operationName);
        setMessageListener(this.messageListener);
        setQueueName(operationName);
        setErrorHandler(errorHandler);
        initialize();
    }

    /* temporary */
    public void initialize() {
        this.channelTemplate.execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare(Constants.TO_SERVER_EXCHANGE, "topic", true, false, null);
                    String requestQueue = channel.queueDeclare("request", true, false, false, null).getQueue();
                    channel.queueBind(requestQueue, Constants.TO_SERVER_EXCHANGE, "request.*");

                    channel.exchangeDeclare(Constants.TO_AGENT_EXCHANGE, "topic", true, false, null);
                    String responseQueue = channel.queueDeclare("response", true, false, false, null).getQueue();
                    channel.queueBind(responseQueue, Constants.TO_AGENT_EXCHANGE, "response.*");
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });
    }

    public Object getEndpoint() {
        return endpoint;
    }
}
