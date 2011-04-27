package org.hyperic.hq.operation.rabbit.core;

public class ConsumerHandlingException extends RuntimeException {

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public ConsumerHandlingException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public ConsumerHandlingException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public ConsumerHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
