package org.hyperic.hq.amqp;

import org.apache.log4j.Logger;
import org.hyperic.hq.amqp.util.Operations;

/**
 * @author Helena Edelson
 */
public class AmqpServerCommandHandler implements AsyncQueueConsumer {

    protected Logger logger = Logger.getLogger(this.getClass());

    private OperationService operationService;

    public AmqpServerCommandHandler() {
        this.operationService = new AgentAmqpOperationService();
    }

    /**
     * Async message handler.
     * @param message the message converted from byte[]
     */
    public void handleMessage(String message) {
        if (message.equalsIgnoreCase(Operations.AGENT_PING_RESPONSE)) {
            handleServerPing(message);
        }
    }

    private long handleServerPing(String message) {
        logger.info("************Agent received response " + message);
        return 0;
    }

    public void handleMessage(byte[] message) {

    }
}
