package org.hyperic.hq.common;

import java.io.Serializable;

import org.hyperic.util.NestedException;
/**
 * General checked Exception to indicate that a requested item was not found
 * @author jhickey
 *
 */
public class NotFoundException extends NestedException implements Serializable 
{
    public NotFoundException() {
        super();
    }
    
    public NotFoundException(String s){
        super(s);
    }
    
    public NotFoundException(Throwable t) {
        super(t);
    }
    
    public NotFoundException(String s, Throwable t) {
        super(s,t);
    }
    
}
