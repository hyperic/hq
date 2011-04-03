package org.hyperic.hq.operation.rabbit.component;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.mapping.DeclarativeBindingHandler;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Helena Edelson
 */
public abstract class AmqpFactoryBean implements FactoryBean<String> {

    protected final DeclarativeBindingHandler declarativeBindingHandler;

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
    public AmqpFactoryBean(ConnectionFactory connectionFactory, String exchangeName, String exchangeType, String routingKey, boolean durable) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.declarativeBindingHandler = new DeclarativeBindingHandler(connectionFactory);
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
