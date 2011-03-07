package org.hyperic.hq.amqp;

import org.hyperic.hq.amqp.core.OperationFailedException;

/**
 * @author Helena Edelson
 */
public interface OperationService {

    /**
     * Perform an asynchronous operation
     * @param operationName           The name of the operation that should be performed
     * @param nodeId                  The id of the node this operation should be performed on
     * @param context                 The context for the operation being performed
     * @param operationStatusCallback A callback for notification of selected events in the lifecycle of the operation
     * @throws org.hyperic.hq.amqp.core.OperationFailedException if there is any problem sending the operation abroad
     */
    <T> void perform(String operationName, long nodeId, Object context, Object operationStatusCallback) throws OperationFailedException;

    /**
     * Sends a message with pre-configured routing.
     */
    void send(String message);

    /**
     * Sends a message with configurable routing.
     */
    void send(String exchange, String routingKey, String message);

}
