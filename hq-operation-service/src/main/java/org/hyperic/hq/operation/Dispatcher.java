package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public interface Dispatcher {
 
    void dispatch(Envelope envelope);
    
}
