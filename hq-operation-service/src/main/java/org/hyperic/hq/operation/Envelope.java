package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public interface Envelope extends Operation {

    /**
     * Returns the id of the operation
     * @return operation id
     */
    long getOperationId();

    /**
     * Returns the context of the operation
     * @return the operation context
     */
    String getContext();

    /**
     * Returns the destination that responses should be sent to
     * @return the destination for responses to the operation
     */
    String getReplyTo();
}
