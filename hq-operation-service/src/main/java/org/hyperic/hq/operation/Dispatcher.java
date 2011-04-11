package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public interface Dispatcher {
 
    Object dispatch(String operationName, Object data);
    
}
