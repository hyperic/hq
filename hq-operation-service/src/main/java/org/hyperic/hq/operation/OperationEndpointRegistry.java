package org.hyperic.hq.operation;

import java.lang.reflect.Method;

 
public interface OperationEndpointRegistry {

     /**
     * Registers an endpoint 
     * @param operationName The name operation name that this method can handle
     * @param endpointMethod The method
     * @param endpointCandidate The instance to invoke the method on
     */
    void registerOperationEndpoint(String operationName, Method endpointMethod, Object endpointCandidate);
    
}
