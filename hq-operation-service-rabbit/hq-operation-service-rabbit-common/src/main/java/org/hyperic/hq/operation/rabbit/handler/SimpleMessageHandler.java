package org.hyperic.hq.operation.rabbit.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.AsyncQueuedConsumer;
import org.hyperic.hq.operation.OperationService;

/**
 * Handles messages from agents
 * @author Helena Edelson
 */
public class SimpleMessageHandler implements AsyncQueuedConsumer {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private OperationService operationService;

    public SimpleMessageHandler(OperationService operationService) {
        this.operationService = operationService;
    }

    /**
     * Async message handler.
     * @param message the message converted from byte[]
     */
    public void handleMessage(String message) {


    }

    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }

    //logger.info("***server received=" + message);
    /*try {
        String resp = OperationConstants.AGENT_PING_RESPONSE + (message.substring(message.length()-1));
        logger.info("***server sending=" + OperationConstants.AGENT_PING_RESPONSE);
        operationService.perform("operationName", resp);

    } catch (Exception e) {
        handleException(e, OperationConstants.AGENT_PING_RESPONSE);
    }*/

    protected void handleException(Throwable t, String operation) {
        logger.error(t.getClass().getSimpleName() + " thrown while executing " + operation, t);
    }

}
