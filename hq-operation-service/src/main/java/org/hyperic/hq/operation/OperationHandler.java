package org.hyperic.hq.operation;

import org.hyperic.hq.operation.Envelope;

/**
 * TODO exeptions
 * @author Helena Edelson
 */
public interface OperationHandler {

    /**
     * Handle this envelope
     * @param envelope The envelope to handle
     */
    void handle(Envelope envelope) throws Exception;
    
}
