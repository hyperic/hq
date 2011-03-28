package org.hyperic.hq.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

/**
 * Prototype only. Pre-spring on agent.
 * @author Helena Edelson
 */
public class AgentManualAmqpConfigurer {

    protected final Log logger = LogFactory.getLog(this.getClass());
    
    private SimpleMessageListenerContainer listener;
 
    public AgentManualAmqpConfigurer(AsyncQueueingConsumer asyncConsumer, String queueName) {
        this.listener = new SimpleMessageListenerContainer(new SingleConnectionFactory());
        listener.setMessageListener(new MessageListenerAdapter(asyncConsumer));
        listener.setQueueName(queueName);
        listener.afterPropertiesSet();
        logger.info("***********Created manual amqp configurer for listener with " + queueName + " and " + asyncConsumer);
    }

    public void start() {
        listener.start();
        logger.info("*********** started listener");
    }

    public void stop() {
        this.listener.stop();
        logger.info("*********** stopped listener");
    }
}
