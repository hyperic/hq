package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public class OperationNotSupportedException extends Exception {
    private static final long serialVersionUID = 2264203750040263921L;

    /**
     * Creates an instance with an exception message and a parent cause
     * @param message The exception message
     */
    public OperationNotSupportedException(String message) {
        super(message);
    }
}
