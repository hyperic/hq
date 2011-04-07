package org.hyperic.hq.operation;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface OperationRegistry {

    /**
        * Registers a registry entity
        * @param operationName The name operation name that this method can handle
        * @param entityMethod The method
        * @param candidate The instance to invoke the method on
        */
       void register(String operationName, Method entityMethod, Object candidate);

}
