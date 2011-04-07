package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationDispatcherException extends RuntimeException {

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public OperationDispatcherException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public OperationDispatcherException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public OperationDispatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
