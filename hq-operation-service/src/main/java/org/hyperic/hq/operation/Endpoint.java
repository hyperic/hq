package org.hyperic.hq.operation;

/**
 * TODO exeptions
 * @author Helena Edelson
 */
public interface Endpoint {

    /**
     * Handle this envelope
     * @param envelope The envelope to handle
     */
    void handle(Envelope envelope) throws EnvelopeHandlingException;
    
}
