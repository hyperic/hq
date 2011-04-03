package org.hyperic.hq.operation;

import java.lang.reflect.Method;

 
public interface OperationHandlerRegistry {

     /**
     * Register a handler
     *
     * @param operationName The name operation name that this method can handle
     * @param handlerMethod The method
     * @param instance The instance to invoke the method on
     */
    void registerOperationHandler(String operationName, Method handlerMethod, Object instance); 
    
}
