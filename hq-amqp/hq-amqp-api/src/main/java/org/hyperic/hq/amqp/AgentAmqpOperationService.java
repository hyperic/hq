package org.hyperic.hq.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Prototype only to work around the current communication and api constraints.
 * @author Helena Edelson
 */
public class AgentAmqpOperationService extends AmqpOperationService implements OperationService {

    /** Temporary, for the agent prototype */
    protected final String serverQueueName = "queues.server";
    protected final String serverDirectExchangeName = "exchanges.direct.server";


    /** Temporary, for the agent prototype */
    public AgentAmqpOperationService() {
        super();
    }

    @Autowired
    public AgentAmqpOperationService(@Qualifier("agentRabbitTemplate") RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    public void send(String message) {
        super.send(serverDirectExchangeName, serverQueueName, message);
    }

    public void send(String exchangeName, String routingKey, String message) {
        super.send(exchangeName, routingKey, message);
    }

    public Object sendAndReceive(String message) {
        return super.sendAndReceive(serverDirectExchangeName, serverQueueName, message);
    }
}
