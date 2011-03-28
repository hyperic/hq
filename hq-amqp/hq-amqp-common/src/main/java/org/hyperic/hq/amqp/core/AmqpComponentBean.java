package org.hyperic.hq.amqp.core;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Helena Edelson
 */
public abstract class AmqpComponentBean implements FactoryBean<String> {

    protected final DeclarativeBindingDelegate declarativeBindingDelegate;

    protected final ChannelTemplate channelTemplate;

    protected final String exchangeName;

    protected final String exchangeType;

    protected final String routingKey;

    protected final boolean durable;


    /**
     * Creates a new anonymous {@link com.rabbitmq.client.AMQP.Queue}
     * @param connectionFactory Used to get a connection to create the queue with
     * @param exchangeName      The name of the exchange to link the queue with
     */
    public AmqpComponentBean(ConnectionFactory connectionFactory, String exchangeName, String exchangeType, String routingKey, boolean durable) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.declarativeBindingDelegate = new DeclarativeBindingDelegate(connectionFactory);
        this.exchangeName = exchangeName;
        this.exchangeType = exchangeType;
        this.routingKey = routingKey;
        this.durable = durable;
    }
 
    public abstract String getObject() throws ChannelException;

    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
