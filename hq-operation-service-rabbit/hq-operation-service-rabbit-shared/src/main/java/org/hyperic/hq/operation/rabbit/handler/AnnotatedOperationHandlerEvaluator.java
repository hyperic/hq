package org.hyperic.hq.operation.rabbit.handler;

import org.hyperic.hq.operation.OperationHandlerEvaluator;
import org.hyperic.hq.operation.OperationHandlerException;
import org.hyperic.hq.operation.OperationHandlerRegistry;

/**
 * @author Helena Edelson
 */
public class AnnotatedOperationHandlerEvaluator implements OperationHandlerEvaluator {

    public void evaluate(Object handlerCandidate, OperationHandlerRegistry registry) throws OperationHandlerException {

    }
    /*public void evaluate(Object handlerCandidate, OperationHandlerRegistry registry) throws IllegalOperationHandlerException {
        Class<?> candidateClass = handlerCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationHandler.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) { 
                    validateReturnType(method, handlerCandidate);
                    validateParameterTypes(method, handlerCandidate);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    register(registry, method, handlerCandidate);
                }
            }
        }
    }

    void validateReturnType(Method candidateMethod, Object handlerCandidate) throws IllegalOperationHandlerException {
        if (void.class.equals(candidateMethod.getReturnType())) {
            throw new IllegalOperationHandlerException(String.format(
                "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have a non-void return type.", candidateMethod,
                handlerCandidate));
        }
    }

    void validateParameterTypes(Method candidateMethod, Object handlerCandidate) throws IllegalOperationHandlerException {
        if (candidateMethod.getParameterTypes().length != 1) {
            throw new IllegalOperationHandlerException(String.format(
                "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have exactly one parameter", candidateMethod,
                handlerCandidate));
        }
    }

    void register(OperationHandlerRegistry registry, Method method, Object handler) {
        registry.registerOperationHandler(method.getAnnotation(Operation.class).value(), method, handler);
    } */
}
