package org.hyperic.hq.operation.rabbit.api;

/** 
 * @author Helena Edelson
 */
public interface Endpoint {

    /**
     * Handle this envelope
     * @param envelope The envelope to handle
     */
    void handle(Envelope envelope) throws EnvelopeHandlingException;
    
}
