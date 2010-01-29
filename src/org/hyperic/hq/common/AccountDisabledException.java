package org.hyperic.hq.common;

import java.io.Serializable;

import org.hyperic.util.NestedException;

public class AccountDisabledException
extends NestedException 
implements Serializable
{
    private static final long serialVersionUID = 1L;

    public AccountDisabledException() {
        super();
    }
    
    public AccountDisabledException(String message) {
        super(message);
    }
    
    public AccountDisabledException(Throwable throwable) {
        super(throwable);
    }
    
    public AccountDisabledException(String message, Throwable throwable) {
        super(message, throwable);
    }
}