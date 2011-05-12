package org.hyperic.hq.operation.rabbit.connection;

/**
 * @author Helena Edelson
 */
public class ConnectionException extends RuntimeException {
    private static final long serialVersionUID = -3364400642077361093L;

    /**
     * Creates a chaining-aware instance
     * @param cause The java.lang.Throwable cause
     */
    public ConnectionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a message
     * @param context The exception message
     */
    public ConnectionException(String context) {
        super(context);
    }

    /**
     * Creates an instance with a message and Throwable cause
     * @param message The exception message
     * @param cause The parent exception
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
