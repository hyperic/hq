package org.hyperic.hq.operation;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface OperationRegistry {

    /**
     * Registers an org.hyperic.hq.operation.Dispatcher or org.hyperic.hq.operation.Endpoint 
     * @param method  The method
     * @param candidate The instance to invoke the method on
     * @param annotation either org.hyperic.hq.operation.OperationDispatcher or org.hyperic.hq.operation.OperationEndpoint
     */
    void register(Method method, Object candidate, Class<? extends Annotation> annotation);

}
