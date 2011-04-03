package org.hyperic.hq.operation;

public class EnvelopeHandlingException extends Exception {

    /**
     * Creates an instance with an exception message and a parent cause
     * @param message The exception message
     * @param cause   The parent cause
     */
    public EnvelopeHandlingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance with an exception message
     * @param message The exception message
     */
    public EnvelopeHandlingException(String message) {
        super(message);
    }
}
