package org.hyperic.hq.operation.rabbit.handler;

import org.hyperic.hq.operation.AsyncQueuedConsumer;
import org.hyperic.hq.operation.rabbit.core.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.core.SimpleRabbitTemplate;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class SimpleAsyncResponseHandler extends SimpleAsyncMessageReceiver implements AsyncQueuedConsumer {

    protected RabbitTemplate rabbitTemplate;

    public SimpleAsyncResponseHandler() {
        this(new SimpleRabbitTemplate());
    }

    public SimpleAsyncResponseHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void handleMessage(String message) {
        String status = this + " received=" + message;
        logger.info("********" + status);
        try {
            rabbitTemplate.send("", "", status);
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
