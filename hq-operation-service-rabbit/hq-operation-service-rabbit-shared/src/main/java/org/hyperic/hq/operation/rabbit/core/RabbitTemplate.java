package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.rabbit.connection.ChannelException;

import java.io.IOException;

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
     */
    Boolean send(String exchangeName, String routingKey, Object data) throws ChannelException, IOException;

    /**
     * Sends a message and synchronously receives the response
     * @param exchangeName the exchange name to use
     * @param routingKey The routing key to use
     * @param data The data to send
     * @throws org.hyperic.hq.operation.rabbit.connection.ChannelException
     * if an error occurs during the send process.
     */
    Object sendAndReceive(String exchangeName, String routingKey, Object data) throws ChannelException, IOException;
    
}
