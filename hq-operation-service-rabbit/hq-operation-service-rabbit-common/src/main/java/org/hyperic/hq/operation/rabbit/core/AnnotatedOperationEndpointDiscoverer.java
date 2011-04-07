package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.OperationRoutingRegistry;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.util.DiscoveryValidator;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public class AnnotatedOperationEndpointDiscoverer extends DiscoveryValidator implements OperationDiscoverer {

    private final OperationRoutingRegistry routingRegistry;

    /**
     * @param routingRegistry The routingRegistry to register with
     */
    public AnnotatedOperationEndpointDiscoverer(OperationRoutingRegistry routingRegistry) {
       this.routingRegistry = routingRegistry;
    }

    /**
     * Discovers, evaluates, validates and registers candidates.
     * @param endpointCandidate the endpoint candidate class
     * @param registry The registry to register with
     * @throws OperationDiscoveryException
     */
    public void discover(Object endpointCandidate, OperationRegistry registry) throws OperationDiscoveryException {
        Class<?> candidateClass = endpointCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationEndpoint.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    register(this.routingRegistry, method.getAnnotation(Operation.class));
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

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * @param registry the registry implementation to use
     * @param method the operational method
     * @param endpoint the endpoint class
     */
    void register(OperationRegistry registry, Method method, Object endpoint) {
        registry.register(method.getAnnotation(Operation.class).value(), method, endpoint);
    }
    /**
     * @param routingRegistry the registry implementation to use
     * @param operation the operation routing meta-data
     */
    void register(OperationRoutingRegistry routingRegistry, Operation operation) {
        routingRegistry.registerRoutings(operation);
    }

}
