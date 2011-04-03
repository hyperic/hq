package org.hyperic.hq.operation;


public interface OperationService {

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed. Based on the operation name,
     * the framework knows where to route it.
     * @param data The data to route
     * @throws OperationFailedException
     */
    void perform(String operationName, Object data) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed
     * @param exchangeName
     * @param routingKey The routing key to send to
     * @param data The data to route
     * @throws OperationFailedException
     */
    void perform(String operationName, String exchangeName, String routingKey, Object data) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operationName The name of the operation that should be performed
     * @param exchangeName
     * @param routingKey The routing key to send to
     * @param data The data to route
     * @throws OperationFailedException
     */
    Object performAndReceive(String operationName, String exchangeName, String routingKey, Object data) throws OperationFailedException;
   
}
