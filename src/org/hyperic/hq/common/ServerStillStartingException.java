package org.hyperic.hq.common;

import java.io.Serializable;

import org.hyperic.util.NestedException;

public class ServerStillStartingException
extends NestedException 
implements Serializable
{
    private static final long serialVersionUID = 1L;

    public ServerStillStartingException() {
        super();
    }
    
    public ServerStillStartingException(String message) {
        super(message);
    }
    
    public ServerStillStartingException(Throwable throwable) {
        super(throwable);
    }
    
    public ServerStillStartingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
