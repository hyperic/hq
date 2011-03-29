package org.hyperic.hq.amqp;

import java.util.Map;

/**
 * @author Helena Edelson
 */
public interface OperationService {

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed. Based on the operation name,
     * the framework knows where to route it.
     * @param data The data to route
     * @param properties Any properties to apply, can be null
     * @throws OperationFailedException
     */
    <T> void perform(String operationName, Object data, Map<String,?> properties) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed
     * @param exchangeName
     * @param routingKey The routing key to send to
     * @param data The data to route
     * @param properties Any properties to apply, can be null
     * @throws OperationFailedException
     */
    <T> void perform(String operationName, String exchangeName, String routingKey, Object data, Map<String,?> properties) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed
     * @param exchangeName
     * @param routingKey The routing key to send to
     * @param data The data to route
     * @param properties Any properties to apply, can be null
     * @throws OperationFailedException
     */
    <T> void performAndReceive(String operationName, String exchangeName, String routingKey, Object data, Map<String,?> properties) throws OperationFailedException;
   
}
