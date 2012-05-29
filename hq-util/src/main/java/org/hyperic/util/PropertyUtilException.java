package org.hyperic.util;

/**
 * Meant to be used by {@link PropertyUtil} and {@link PropertyEncryptionUtil}, to indicate an property-handling related
 * error. The extension of Exception (rather than RuntimeException) was consciously made.
 *
 * @author Adi Baron
 */
public class PropertyUtilException extends Exception {

    public PropertyUtilException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public PropertyUtilException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public PropertyUtilException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
