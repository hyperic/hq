package org.hyperic.hq.amqp.core;

/**
 * @author Helena Edelson
 */
public class SimpleAsyncResponseHandler extends SimpleAsyncMessageReceiver implements AsyncQueueingConsumer {

    protected OperationService operationService;

    public SimpleAsyncResponseHandler() {
       this(new AgentAmqpOperationService());
    }
    
    public SimpleAsyncResponseHandler(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public void handleMessage(String message) {
            String status = this + " received=" + message;
            logger.info("********"+status);
            operationService.send(status);
    }
}
