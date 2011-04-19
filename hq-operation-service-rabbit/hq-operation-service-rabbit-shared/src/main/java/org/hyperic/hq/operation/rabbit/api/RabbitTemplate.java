package org.hyperic.hq.operation.rabbit.api;

import com.rabbitmq.client.AMQP;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;

/**
 * @author Helena Edelson
 */
public interface RabbitTemplate {

    /**
     * Sends a message
     * @param exchange the exchange name to use
     * @param routingKey The routing key to use
     * @param data The data to send
     * @param props AMQP properties containing a correlation id
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     * if an error occurs during the send process
     */
    void send(String exchange, String routingKey, Object data, AMQP.BasicProperties props) throws ChannelException;

    /**
     * Sends a message and synchronously receives the response
     * @param responseQueue the name of the queue to consume the response from
     * @param exchange the exchange name to use
     * @param routingKey The routing key to use
     * @param data The data to send
     * @param props AMQP properties containing a correlation id
     * @param responseType
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     * if an error occurs during the send process.
     * @return the data returned from the response
     */
    Object sendAndReceive(String responseQueue, String exchange, String routingKey, Object data, AMQP.BasicProperties props, Class<?> responseType) throws ChannelException;
    
}
