package org.hyperic.hq.amqp;

import org.apache.log4j.Logger;

/**
 * Prototype only.
 * @author Helena Edelson
 */
public class SimpleAsyncResponseHandler implements AsyncQueueConsumer {

    protected Logger logger = Logger.getLogger(this.getClass());

    protected OperationService operationService;

    public SimpleAsyncResponseHandler() {
       this.operationService = new AgentAmqpOperationService();
    }
    
    public SimpleAsyncResponseHandler(OperationService operationService) {
        this.operationService = operationService;
    }

    public void handleMessage(String message) {
            String status = this + " received [" + message + "]";
            logger.info("********"+status);

            operationService.send(status);
    }

    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }

}
