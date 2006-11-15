package org.hyperic.hq.control.shared;

import org.hyperic.hq.common.ApplicationException;

public class ControlActionTimeoutException
    extends ApplicationException {

    public ControlActionTimeoutException() {
        super();
    }
    
    public ControlActionTimeoutException(String s){
        super(s);
    }
    
    public ControlActionTimeoutException(Exception e){
        super(e);
    }
    
    public ControlActionTimeoutException(Throwable t) {
        super(t);
    }
    
    public ControlActionTimeoutException(String s, Throwable t) {
        super(s,t);
    }
}
