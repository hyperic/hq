package org.hyperic.hq.operation;


import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface OperationRegistry {
 
    /**
     * Registers an candidate and its annotated operation methods
     * @param method  The method
     * @param candidate The instance to invoke the method on
     * @throws OperationDiscoveryException if an exception occurs
     */
    void register(Method method, Object candidate) throws OperationDiscoveryException;

}
