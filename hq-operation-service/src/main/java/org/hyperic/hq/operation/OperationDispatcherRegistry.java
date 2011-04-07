package org.hyperic.hq.operation;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface OperationDispatcherRegistry {

    /**
        * Registers a dispatcher
        * @param operationName The name operation name that this method can handle
        * @param dispatcherMethod The method
        * @param candidate The instance to invoke the method on
        */
       void registerOperationDispatcher(String operationName, Method dispatcherMethod, Object candidate);

}
