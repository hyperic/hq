package org.hyperic.hq.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.amqp.util.Operations;

/**
 * @author Helena Edelson
 */
public class AmqpAgentCommandHandler implements AsyncQueueConsumer {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private OperationService operationService;

    public AmqpAgentCommandHandler(OperationService operationService) {
        this.operationService = operationService;
    }

    /**
     * Async message handler.
     * @param message the message converted from byte[]
     */
    public void handleMessage(String message) {
        if (message.equalsIgnoreCase(Operations.AGENT_PING_REQUEST)) {
            handleAgentPing(message);
        }
    }

    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }

    private void handleAgentPing(String message) {
        System.out.println("server received=" + message);
        logger.info("***server received=" + message);
        try {

            operationService.send(Operations.AGENT_PING_RESPONSE);
            System.out.println("server sent=" + Operations.AGENT_PING_RESPONSE);
            logger.info("***server sent=" + Operations.AGENT_PING_RESPONSE);

        } catch (Exception e) {
            handleException(e, Operations.AGENT_PING_RESPONSE);
        }
    }

    protected void handleException(Throwable t, String operation) {
        logger.error(t.getClass().getSimpleName() + " thrown while executing " + operation, t);
    }

}
