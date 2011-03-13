package org.hyperic.hq.amqp;

import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.LegacyAgentCommandsClientImpl;
import org.hyperic.hq.amqp.util.Operations;

/**
 * Prototype only.
 * @author Helena Edelson
 */
public class AgentAmqpCommandOperationService extends AmqpCommandOperationService {

    /**
     * Temporary, for the agent prototype
     */
    protected final String routingKey = "queues.server";
    protected final String exchangeName = "exchanges.direct.server";


    public AgentAmqpCommandOperationService(AgentCommandsClient legacyClient) {
        super(legacyClient);
    }

    /**
     * The first to be overridden.
     * Do we really need to return a duration during the transition?
     * @return duration
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping() {
        /* The unidirectional client does not work yet: agent unaware of its token. */
        if (unidirectional) return 0;
        logger.info("***********.ping()");
        long sendTime = System.currentTimeMillis();
        long duration = 0;

        try {
            if (legacyClient instanceof LegacyAgentCommandsClientImpl) {
                String response = (String) operationService.sendAndReceive("exchanges.direct.server", "queues.agent", Operations.AGENT_PING_REQUEST);
                logger.info("response="+response);
                
                logger.info("sent message: " + Operations.AGENT_PING_REQUEST);
                duration = System.currentTimeMillis() - sendTime;
            } else {
                operationService.send(Operations.PING);
                logger.info("agent message: " + Operations.PING);
                duration = System.currentTimeMillis() - sendTime;
            }

        } catch (Exception e) {
            handleException(e, Operations.PING);
        }
        logger.info("***********ping() executed, returning duration=" + duration);
        return duration;
    }
}
