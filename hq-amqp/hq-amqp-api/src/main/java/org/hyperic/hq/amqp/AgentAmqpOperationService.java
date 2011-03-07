package org.hyperic.hq.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

/**
 * Prototype only to work around the current communication and api constraints.
 * @author Helena Edelson
 */
public class AgentAmqpOperationService extends AmqpOperationService implements OperationService {

    private final String agentToServerExchangeName = "agentServerExchange";

    private final String agentToServerRoutingKey = "agentServerQueue";

    private final String serverToAgentQueueName = "serverToAgentQueue";

    private SimpleMessageListenerContainer agentListener;

    /** Temporary, for the agent prototype */
    public AgentAmqpOperationService() {
        super();
        this.agentListener = new SimpleMessageListenerContainer(new SingleConnectionFactory());
        agentListener.setMessageListener(new MessageListenerAdapter(new SimpleAsyncResponseHandler()));
        agentListener.setQueues(new Queue(serverToAgentQueueName));
        agentListener.afterPropertiesSet();
        agentListener.start();
    }

    
    public void send(String message) {
        super.send(agentToServerExchangeName, agentToServerRoutingKey, message);
    }

    public void stop() {
        agentListener.stop();
    }
}
