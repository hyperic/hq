package org.hyperic.hq.amqp;

import org.apache.log4j.Logger;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Helena Edelson
 */
@Component
public class AmqpOperationService implements OperationService {

    protected Logger logger = Logger.getLogger(this.getClass());
 
    /**
     * Injection of template with pre-configured exchange and routing key
     */
    private RabbitTemplate rabbitTemplate;

    /**
     * Temporary, for the agent prototype
     */
    public AmqpOperationService() {
        this.rabbitTemplate = new RabbitTemplate(new SingleConnectionFactory());
    }
 
    @Autowired 
    public AmqpOperationService(@Qualifier("serverRabbitTemplate") RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * TODO desc.
     * @param operationName           The name of the operation that should be performed
     * @param nodeId                  The id of the node this operation should be performed on
     * @param context                 The context for the operation being performed
     * @param operationStatusCallback A callback for notification of selected events in the lifecycle of the operation
     * @param <T>
     * @throws RuntimeException
     */
    public <T> void perform(String operationName, long nodeId, Object context, Object operationStatusCallback) throws RuntimeException {

    }

    /**
     * Sends a message with pre-configured routing.
     */
    public void send(String message) {
        rabbitTemplate.convertAndSend(message);
        logger.info("Sent message: " + message);
    }

    /**
     * Sends a message with configurable routing.
     */
    public void send(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        logger.info("Sent message: " + message);
    } 
}
