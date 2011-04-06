package org.hyperic.hq.operation;


public interface OperationService {

    /**
     * Perform an asynchronous operation
     * @param operation The operation that should be performed. Based on the operation name,
     * the framework knows where to route it
     * @throws OperationFailedException
     */
    void perform(Operation operation) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operation The operation that should be performed
     * @param exchangeName The exchange name to use
     * @param routingKey The routing key to use
     * @throws OperationFailedException
     */
    void perform(Operation operation, String exchangeName, String routingKey) throws OperationFailedException;

    /**
     * Perform an asynchronous operation
     * @param operation The name of the operation that should be performed
     * @param exchangeName The exchange name to use
     * @param routingKey The routing key to use
     * @throws OperationFailedException
     */
    Object performAndReceive(Operation operation, String exchangeName, String routingKey) throws OperationFailedException;
   
}
