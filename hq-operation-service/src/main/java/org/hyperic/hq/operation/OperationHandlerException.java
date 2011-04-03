package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationHandlerException extends Exception {

    /**
     * Creates an instance with an exception message and a parent cause
     * @param message The exception message
     * @param cause The parent cause
     */
    public OperationHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance with an exception message
     * @param message The exception message
     */
    public OperationHandlerException(String message) {
        super(message);
    }
}
