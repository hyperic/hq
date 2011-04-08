package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.Dispatcher;
import org.hyperic.hq.operation.OperationData;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class RabbitOperationService implements OperationService {


    /**
     * Injection of template with pre-configured exchange and routing key
     */
    private RabbitTemplate rabbitTemplate;

    private final Dispatcher dispatcher;

    /**
     * Used by non-Spring clients and guest credentials
     */
    public RabbitOperationService(Dispatcher dispatcher) {
        this.dispatcher = dispatcher; 
    }

    //TODO add constructor w/credentials
    /*       when(this.converter.write(context)).thenReturn("");
        this.operationService.perform("test.operation.name", "0", context, this.operationStatusCallback);*/
    
    /** 
     * @param operation The operation that should be performed. Based on the operation name,
     * the framework knows where to route it
     * @throws OperationFailedException
     */
    public void perform(OperationData operation) throws OperationFailedException {
        //send(exchangeName, routingKey, operation);
    }

    /**
     * @param operation The operation that should be performed
     * @param exchangeName The exchange name to use
     * @param routingKey The routing key to use
     * @throws OperationFailedException
     */
    public void perform(OperationData operation, String exchangeName, String routingKey) throws OperationFailedException {
        try {
            rabbitTemplate.send(exchangeName, routingKey, operation);
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }
    }

    /**
     * Handle legacy synchronous operations
     * @param operation The name of the operation that should be performed
     * @param exchangeName The exchange name to use
     * @param routingKey The routing key to use
     * @return
     * @throws OperationFailedException
     */
    public Object performAndReceive(OperationData operation, String exchangeName, String routingKey) throws OperationFailedException {
        try {
            return rabbitTemplate.sendAndReceive(exchangeName, routingKey, operation);
        }
        catch (IOException e) {
            throw new OperationFailedException(e.getMessage(), e);
        }
    }
}
