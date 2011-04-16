package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationDiscoveryException extends Exception {

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public OperationDiscoveryException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public OperationDiscoveryException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public OperationDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
