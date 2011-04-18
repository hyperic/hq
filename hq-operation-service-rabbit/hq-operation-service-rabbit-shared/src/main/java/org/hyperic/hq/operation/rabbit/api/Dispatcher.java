package org.hyperic.hq.operation.rabbit.api;

/**
 * @author Helena Edelson
 */
public interface Dispatcher {
 
    Object dispatch(String operationName, Object data);
    
}
