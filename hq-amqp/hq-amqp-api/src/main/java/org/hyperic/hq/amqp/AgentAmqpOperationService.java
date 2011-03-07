package org.hyperic.hq.amqp;

/**
 * Prototype only to work around the current communication and api constraints.
 * @author Helena Edelson
 */
public class AgentAmqpOperationService extends AmqpOperationService implements OperationService {
 
    /** Temporary, for the agent prototype */
    public AgentAmqpOperationService() {
        super();
    }

    
    public void send(String message) {
        super.send(agentToServerExchangeName, agentToServerRoutingKey, message);
    }

}
