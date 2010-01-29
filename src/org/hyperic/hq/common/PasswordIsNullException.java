package org.hyperic.hq.common;

import java.io.Serializable;

import org.hyperic.util.NestedException;

public class PasswordIsNullException
extends NestedException 
implements Serializable
{
    private static final long serialVersionUID = 1L;

    public PasswordIsNullException() {
        super();
    }
    
    public PasswordIsNullException(String message) {
        super(message);
    }
    
    public PasswordIsNullException(Throwable throwable) {
        super(throwable);
    }
    
    public PasswordIsNullException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
