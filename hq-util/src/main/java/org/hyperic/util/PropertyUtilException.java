package org.hyperic.util;

/**
 * Meant to be used by {@link PropertyUtil} and {@link PropertyEncryptionUtil}, to indicate an property-handling related
 * error. The extension of Exception (rather than RuntimeException) was consciously made.
 */
public class PropertyUtilException extends Exception {

    public PropertyUtilException(String message) {
        super(message);
    }

    public PropertyUtilException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyUtilException(Throwable cause) {
        super(cause);
    }
}
