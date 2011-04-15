package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.DisposableBean;

/**
 * @author Helena Edelson
 */
public class RabbitMessageListenerContainer extends SimpleMessageListenerContainer implements DisposableBean {

    private final Log logger = LogFactory.getLog(RabbitMessageListenerContainer.class);

    private final ConnectionFactory connectionFactory;

    private final ChannelTemplate channelTemplate;
      
    private final Object endpoint;

    private final String queueName;

    public RabbitMessageListenerContainer(ConnectionFactory connectionFactory, Object endpoint, String queueName) {
        super(new SingleConnectionFactory(connectionFactory));
        this.connectionFactory = connectionFactory;
        this.endpoint = endpoint;
        this.queueName = queueName;
        setQueueName(queueName);
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        setMessageListener(new MessageListenerAdapter(endpoint));
    }

   // @PostConstruct   /* temporary */
    public void prepare() {
        ChannelTemplate template = new ChannelTemplate(new ConnectionFactory());
        Channel channel = template.createChannel();

        try {
            channel.exchangeDeclare(Constants.TO_SERVER_EXCHANGE, "topic", true, false, null);
            String requestQueue = channel.queueDeclare("request", true, false, false, null).getQueue();
            channel.queueBind(requestQueue, Constants.TO_SERVER_EXCHANGE, "request.*");

            channel.exchangeDeclare(Constants.TO_AGENT_EXCHANGE, "topic", true, false, null);
            String responseQueue = channel.queueDeclare("response", true, false, false, null).getQueue();
            channel.queueBind(responseQueue, Constants.TO_AGENT_EXCHANGE, "response.*");
        } catch (Exception e) { 
            throw new ChannelException(e.getCause());
        } finally {
            template.releaseResources(channel);
        }
    }

    public Object getEndpoint() {
        return endpoint;
    }
}
