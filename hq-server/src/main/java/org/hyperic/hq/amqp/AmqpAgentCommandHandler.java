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

    /**
     * TODO: if (unidirectional) return 0;
     */
    private void handleAgentPing(String message) {
        logger.info("***********server received message=" + message);
        try {

            operationService.send(Operations.AGENT_PING_RESPONSE);
            logger.info("***********server sent response back=" + Operations.AGENT_PING_RESPONSE);

        } catch (Exception e) {
            handleException(e, Operations.AGENT_PING_RESPONSE);
        }
    }

    protected void handleException(Throwable t, String operation) {
        logger.error(t.getClass().getSimpleName() + " thrown while executing " + operation, t);
    }

}
