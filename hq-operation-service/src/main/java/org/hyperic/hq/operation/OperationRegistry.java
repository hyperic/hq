package org.hyperic.hq.operation;


import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface OperationRegistry {
 
    /**
     * Registers an org.hyperic.hq.operation.Dispatcher or org.hyperic.hq.operation.Endpoint
     * @param method  The method
     * @param candidate The instance to invoke the method on
     */
    void register(Method method, Object candidate) throws OperationDiscoveryException;

}
