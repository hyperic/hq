package org.hyperic.hq.operation.rabbit.api;

import org.hyperic.hq.operation.rabbit.connection.ChannelException;

/**
 * @author Helena Edelson
 */
public interface RabbitTemplate {

    /**
     * Sends a message
     * @param exchangeName the exchange name to use
     * @param routingKey The routing key to use
     * @param data The data to send
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     * if an error occurs during the send process. 
     * @return if successful, returns true else returns false
     */
    Boolean send(final String exchangeName, final String routingKey, final String data) throws ChannelException;

    /**
     * Sends a message and synchronously receives the response
     * @param queueName the name of the queue to consume the response from
     * @param exchangeName the exchange name to use
     * @param routingKey The routing key to use
     * @param data The data to send
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     * if an error occurs during the send process.
     * @return the data returned from the response
     */
    Object sendAndReceive(String queueName, String exchangeName, String routingKey, String data) throws ChannelException;
    
}
