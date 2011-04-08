package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.*;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.util.OperationRouting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For the non-Spring Agent
 * @author Helena Edelson
 */
public class RabbitOperationDispatcher extends AbstractRabbitOperationEntity implements OperationRegistry, Dispatcher, OperationDiscoverer {

    private final Map<String, MethodInvoker> operationDispatchers = new ConcurrentHashMap<String, MethodInvoker>();
 
    private final OperationRegistry operationRegistry;

    private final RoutingRegistry routingRegistry;

    public RabbitOperationDispatcher(ConnectionFactory connectionFactory, OperationRegistry operationRegistry) {
        super(connectionFactory);
        this.operationRegistry = operationRegistry;
        this.routingRegistry = new OperationToRoutingKeyRegistry(connectionFactory);
    }

    public void discover(Object... dispatcherCandidates) {
        for (Object bean : dispatcherCandidates) {
            discover(bean);
        }
    }

    /**
     * Discovers, evaluates, validates and registers candidates.
     * @param dispatcherCandidate the dispatcher candidate class
     * @param operationRegistry   The operationRegistry to register with
     * @throws OperationDiscoveryException
     */
    public void discover(Object dispatcherCandidate, OperationRegistry operationRegistry) throws OperationDiscoveryException {
        Class<?> candidateClass = dispatcherCandidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationDispatcher.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    validateReturnType(method, dispatcherCandidate);
                    validateParameterTypes(method, dispatcherCandidate);
                    if (!method.isAccessible()) method.setAccessible(true);
                    register(method.getAnnotation(Operation.class).operationName(), method, dispatcherCandidate);
                }
            }
        }
    }
   
    /**
     * Registers a method as operation with a Registry for future dispatch.
     * Registers dispatchers and delegates to RoutingRegistry for further work.
     * @param operationName The name operation name that this method can handle
     * @param dispatcherMethod the method
     * @param dispatcherCandidate the candidate instance
     */
    public void register(String operationName, Method dispatcherMethod, Object dispatcherCandidate) {
        if (!this.operationDispatchers.containsKey(dispatcherMethod.getAnnotation(Operation.class).operationName())) {
            this.operationDispatchers.put(operationName, new MethodInvoker(dispatcherMethod, dispatcherCandidate, this.converter));
            this.operationRegistry.register(operationName, dispatcherMethod, dispatcherCandidate);
            this.routingRegistry.register(dispatcherMethod.getAnnotation(Operation.class));
        }
    }

    /**
     * Hand-off point from Hyperic to Rabbit
     * @param envelope
     */
    public void dispatch(Envelope envelope) {
        if (!this.operationDispatchers.containsKey(envelope.getOperationName()))
            throw new OperationNotSupportedException(envelope.getOperationName());

        MethodInvoker methodInvoker = this.operationDispatchers.get(envelope.getOperationName());

        OperationRouting routing = this.routingRegistry.getMapping(envelope.getOperationName());

        Object data = null;

        try {
            data = methodInvoker.invoke(envelope.getContext());
        } catch (IllegalAccessException e) {
            //logger.error("", e);
        } catch (InvocationTargetException e) {
            //logger.error("", e);
        }

        try {

            this.rabbitTemplate.send(routing.getExchangeName(), routing.getValue(), data);
        } catch (IOException e) {
            //logger.error("", e);
        }
    } 
}
