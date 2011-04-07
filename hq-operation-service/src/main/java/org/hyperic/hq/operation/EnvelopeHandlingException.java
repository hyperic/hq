package org.hyperic.hq.operation;

public class EnvelopeHandlingException extends RuntimeException {

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public EnvelopeHandlingException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public EnvelopeHandlingException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and a parent exception
     * @param message The exception message
     * @param cause The parent exception
     */
    public EnvelopeHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
