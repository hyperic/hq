package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.ErrorHandler;

/**
 * @author Helena Edelson
 */
public class RabbitMessageListenerContainer extends SimpleMessageListenerContainer implements DisposableBean {

    private final Log logger = LogFactory.getLog(RabbitMessageListenerContainer.class);

    public RabbitMessageListenerContainer(ConnectionFactory connectionFactory, Object endpoint,
                                          String operationName, ErrorHandler errorHandler) {
        super(new SingleConnectionFactory(connectionFactory));

        MessageListenerAdapter mla = new MessageListenerAdapter(endpoint);
        mla.setDefaultListenerMethod(operationName);
        setMessageListener(mla);
        setQueueName(operationName);
        setErrorHandler(errorHandler);
        afterPropertiesSet();
        start();
        logger.info("created listener for endpoint=" + endpoint + " to invoke handle method=" + operationName);
    }

}
