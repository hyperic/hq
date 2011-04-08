package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.AsyncQueuedConsumer;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class SimpleAsyncResponseHandler implements AsyncQueuedConsumer {

    protected RabbitTemplate rabbitTemplate;

    public SimpleAsyncResponseHandler() {
        this(new SimpleRabbitTemplate());
    }

    public SimpleAsyncResponseHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
 
    public void handleMessage(String message) {
        String status = this + " received=" + message;
 
        try {
            rabbitTemplate.send("", "", status);
        } catch (IOException e) {
            //logger.error(e);
        }
    }
    
    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }
    protected void handleException(Throwable t, String operation) {
        //logger.error(t.getClass().getSimpleName() + " thrown while executing " + operation, t);
    }

}
