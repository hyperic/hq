package org.hyperic.hq.operation.rabbit.core;

import org.apache.log4j.Logger;
import org.hyperic.hq.operation.Operation;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.connection.SingleConnectionFactory;
import org.hyperic.hq.operation.rabbit.mapping.OperationMappingRegistry;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class RabbitOperationService implements OperationService {

    protected final Logger logger = Logger.getLogger(this.getClass());

    private OperationMappingRegistry operationMappingRegistry;
 
    /**
     * Injection of template with pre-configured exchange and routing key
     */
    private RabbitTemplate rabbitTemplate;

    /**
     * Used by non-Spring clients and guest credentials
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

    //TODO add constructor w/credentials

    /** 
     * @param operation The operation that should be performed. Based on the operation name,
     * the framework knows where to route it
     * @throws OperationFailedException
     */
    public void perform(Operation operation) throws OperationFailedException {
        //send(exchangeName, routingKey, operation);
    }

    /**
     * @param operation The operation that should be performed
     * @param exchangeName The exchange name to use
     * @param routingKey The routing key to use
     * @throws OperationFailedException
     */
    public void perform(Operation operation, String exchangeName, String routingKey) throws OperationFailedException {
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
    public Object performAndReceive(Operation operation, String exchangeName, String routingKey) throws OperationFailedException {
        try {
            return rabbitTemplate.sendAndReceive(exchangeName, routingKey, operation);
        }
        catch (IOException e) {
            throw new OperationFailedException(e.getMessage(), e);
        }
    }
}
