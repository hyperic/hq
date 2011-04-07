package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.OperationDispatcherDiscoverer;
import org.hyperic.hq.operation.OperationDispatcherException;
import org.hyperic.hq.operation.OperationDispatcherRegistry;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;

import java.lang.reflect.Method;

/**
 * Detects and maps operations and operation dispatchers
 * to the messaging system while keeping them independent of each other.
 * @author Helena Edelson
 */
public class AnnotatedOperationDispatcherMapper implements OperationDispatcherDiscoverer {

    public void discover(Object dispatcherCandidate, OperationDispatcherRegistry registry) throws OperationDispatcherException {
        Class<?> candidateClass = dispatcherCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationDispatcher.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    validateReturnType(method, dispatcherCandidate);
                    validateParameterTypes(method, dispatcherCandidate);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    register(registry, method, dispatcherCandidate);
                }
            }
        }
    }
    
    void validateReturnType(Method candidateMethod, Object dispatcherCandidate) throws OperationDispatcherException {
        if (void.class.equals(candidateMethod.getReturnType())) {
            throw new OperationDispatcherException(String.format(
                "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have a non-void return type.",
                    candidateMethod, dispatcherCandidate));
        }
    }

    void validateParameterTypes(Method candidateMethod, Object dispatcherCandidate) throws OperationDispatcherException {
        if (candidateMethod.getParameterTypes().length != 1) {
            throw new OperationDispatcherException(String.format(
                "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have exactly one parameter",
                    candidateMethod, dispatcherCandidate));
        }
    }

    void register(OperationDispatcherRegistry registry, Method method, Object dispatcher) {
        registry.registerOperationDispatcher(method.getAnnotation(Operation.class).value(), method, dispatcher);
    }
}
