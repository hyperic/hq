package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;

/**
 * @author Helena Edelson
 */
public interface RabbitTemplate {

    /**
     * Publishes a message to the broker
     * @param exchange   the exchange name to use
     * @param routingKey The routing key to use
     * @param data       The data to send
     * @param props      AMQP properties containing a correlation id and other meta-data
     * @throws com.vmware.vcib.operation.rabbit.connection.ChannelException
     *          if an error occurs during the send process
     */
    void publish(String exchange, String routingKey, Object data, AMQP.BasicProperties props) throws ChannelException;

    /**
     * Publishes a message to the broker.
     * @param channelToUse the container's channel
     * @param exchange     the exchange name to use
     * @param routingKey   The routing key to use
     * @param data         The data to send
     * @param props        AMQP properties containing a correlation id and other meta-data
     * @throws com.vmware.vcib.operation.rabbit.connection.ChannelException
     *          if an error occurs during the send process.
     */
    void publish(Channel channelToUse, String exchange, String routingKey, Object data, AMQP.BasicProperties props) throws ChannelException;

    /**
     * Publishes a message to the broker and synchronously receives the response
     * @param responseQueue     the name of the queue to consume the response from
     * @param exchange          the exchange name to use
     * @param routingKey        The routing key to use
     * @param data              The data to send
     * @param convertResponseTo the type to convert the response to
     * @param props             AMQP properties containing a correlation id and other meta-data
     * @return the data returned from the response
     * @throws com.vmware.vcib.operation.rabbit.connection.ChannelException
     *          if an error occurs during the send process.
     */
    Object publishAndReceive(String responseQueue, String exchange, String routingKey, Object data, Class<?> convertResponseTo, AMQP.BasicProperties props) throws ChannelException;

    void close();

}
