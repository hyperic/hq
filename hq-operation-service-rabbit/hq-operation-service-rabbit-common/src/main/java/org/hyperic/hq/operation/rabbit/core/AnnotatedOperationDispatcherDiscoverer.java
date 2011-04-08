package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.util.DiscoveryValidator;

import java.lang.reflect.Method;

/**
 * Detects and maps operations and operation dispatchers to the messaging system
 * while keeping them independent of each other. If candidates are valid, registers them.
 * @author Helena Edelson
 */
public class AnnotatedOperationDispatcherDiscoverer extends DiscoveryValidator implements OperationDiscoverer {

    /**
     * Discovers, evaluates, validates and registers candidates.
     * @param dispatcherCandidate the dispatcher candidate class
     * @param operationRegistry The operationRegistry to register with
     * @throws OperationDiscoveryException
     */
    public void discover(Object dispatcherCandidate, OperationRegistry operationRegistry) throws OperationDiscoveryException {
        Class<?> candidateClass = dispatcherCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationDispatcher.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    validateReturnType(method, dispatcherCandidate);
                    validateParameterTypes(method, dispatcherCandidate);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    register(operationRegistry, method, dispatcherCandidate);
                }
            }
        }
    }

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * @param operationRegistry the registry implementation to use
     * @param method the operational method
     * @param dispatcher the dispatcher class
     */
    void register(OperationRegistry operationRegistry, Method method, Object dispatcher) {
        operationRegistry.register(method.getAnnotation(Operation.class).operationName(), method, dispatcher);
    }
}
