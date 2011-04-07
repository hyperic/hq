package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.OperationEndpointDiscoverer;
import org.hyperic.hq.operation.OperationEndpointException;
import org.hyperic.hq.operation.OperationEndpointRegistry;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public class AnnotatedOperationEndpointMapper implements OperationEndpointDiscoverer {

    public void discover(Object endpointCandidate, OperationEndpointRegistry registry) throws OperationEndpointException {
        Class<?> candidateClass = endpointCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationEndpoint.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    validateReturnType(method, endpointCandidate);
                    validateParameterTypes(method, endpointCandidate);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    register(registry, method, endpointCandidate);
                }
            }
        }
    }

    void validateReturnType(Method candidateMethod, Object endpointCandidate) throws OperationEndpointException {
        if (void.class.equals(candidateMethod.getReturnType())) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have a non-void return type.",
                        candidateMethod, endpointCandidate));
        }
    }

    void validateParameterTypes(Method candidateMethod, Object endpointCandidate) throws OperationEndpointException {
        if (candidateMethod.getParameterTypes().length != 1) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have exactly one parameter",
                        candidateMethod, endpointCandidate));
        }
    }

    void register(OperationEndpointRegistry registry, Method method, Object endpoint) {
        registry.registerOperationEndpoint(method.getAnnotation(Operation.class).value(), method, endpoint);
    }
}
