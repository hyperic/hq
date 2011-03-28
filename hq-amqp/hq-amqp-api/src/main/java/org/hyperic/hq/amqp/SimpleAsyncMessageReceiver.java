package org.hyperic.hq.amqp;

import org.apache.log4j.Logger;

/**
 * @author Helena Edelson
 */
public class SimpleAsyncMessageReceiver implements AsyncQueueingConsumer {

    protected Logger logger = Logger.getLogger(this.getClass());

    public void handleMessage(String message) {
            String status = this + " received=" + message;
            logger.info("********"+status); 
    }

    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }
}
