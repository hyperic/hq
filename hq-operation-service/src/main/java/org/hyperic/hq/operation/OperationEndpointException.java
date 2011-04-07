package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationEndpointException extends RuntimeException {
    private static final long serialVersionUID = -4901175883619184424L;

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public OperationEndpointException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public OperationEndpointException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public OperationEndpointException(String message, Throwable cause) {
        super(message, cause);
    }
}
