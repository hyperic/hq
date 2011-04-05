package org.hyperic.hq.operation.rabbit.core;

import org.apache.log4j.Logger;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;

import java.io.IOException;

/**
 * TODO add converter
 * @author Helena Edelson
 */
public class RabbitOperationService implements OperationService {

    protected Logger logger = Logger.getLogger(this.getClass());

    private static final long DEFAULT_REPLY_TIMEOUT = 5000;

    /**
     * Injection of template with pre-configured exchange and routing key
     */
    private RabbitTemplate rabbitTemplate;

    /**
     * Used by the Agent which does not use Spring (yet)
     */
    public RabbitOperationService() {
        this(new SimpleRabbitTemplate(new SingleConnectionFactory()));
    }

    /**
     * Used by the Server
     * @param rabbitTemplate
     */
    public RabbitOperationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Temporary until conversion complete
     * @param operationName The name of the operation that should be performed. Based on the operation name,
     * the framework knows where to route it.
     * @param data The data to route
     * @throws OperationFailedException
     */
    public void perform(String operationName, Object data) throws OperationFailedException {
        if (data instanceof String) {
            handleStringMessage(null, null, data);
        }
    }

    /**
     * Temporary until conversion complete
     * @param operationName The name of the operation that should be performed
     * @param exchangeName
     * @param routingKey The routing key to send to
     * @param data The data to route
     * @throws OperationFailedException
     */
    public void perform(String operationName, String exchangeName, String routingKey, Object data) throws OperationFailedException {
        if (data instanceof String) {
            handleStringMessage(exchangeName, routingKey, data);
        }
    }

    /**
     * Handle legacy synchronous operations
     * @param operationName the operation name
     * @param exchangeName the exchange name to use
     * @param routingKey the routing key
     * @param data the payload
     * @throws org.hyperic.hq.operation.OperationFailedException if an
     * error occurs during the synchronous send and receive
     */
    public Object performAndReceive(String operationName, String exchangeName, String routingKey, Object data) throws OperationFailedException { 
        try {
            Object response = rabbitTemplate.sendAndReceive(exchangeName, routingKey, data.toString());
            logger.info("sent=" + data + ", received=" + response);
            return response;
        }
        catch (IOException e) {
            throw new OperationFailedException(e.getMessage());
        } 
    }

    /**
     *
     * @param exchangeName
     * @param routingKey
     * @param data
     */
    private void handleStringMessage(String exchangeName, String routingKey, Object data) {
        try {
           rabbitTemplate.send(exchangeName, routingKey, data.toString());
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }
    }

}
