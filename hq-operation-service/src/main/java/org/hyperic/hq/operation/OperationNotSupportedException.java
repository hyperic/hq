package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationNotSupportedException extends RuntimeException {

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public OperationNotSupportedException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public OperationNotSupportedException(String context) {
        super(context + " is not supported.");
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public OperationNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
