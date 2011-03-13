package org.hyperic.hq.amqp;

/**
 * Prototype only. Pre-spring on agent.
 * @author Helena Edelson
 */
public class AgentPreSpringAmqpConfigurer extends AgentManualAmqpConfigurer {

    /** Temporary, for the agent prototype */
    protected static final String queueName = "queues.agent";

    public AgentPreSpringAmqpConfigurer() {
        super(new AmqpServerCommandHandler(), queueName);
    }

}
