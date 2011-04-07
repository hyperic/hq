package org.hyperic.hq.operation.rabbit.core;

import org.apache.log4j.Logger;
import org.hyperic.hq.operation.AsyncQueuedConsumer;

/**
 * @author Helena Edelson
 */
public class SimpleAsyncMessageReceiver implements AsyncQueuedConsumer {

    protected Logger logger = Logger.getLogger(this.getClass());

    public void handleMessage(String message) {
            String status = this + " received=" + message;
            logger.info("********"+status); 
    }

    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }
}
