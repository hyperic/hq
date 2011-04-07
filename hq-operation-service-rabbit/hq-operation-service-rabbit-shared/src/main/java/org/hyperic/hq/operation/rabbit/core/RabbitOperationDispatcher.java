package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.*;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.mapping.AbstractRabbitOperationEntity;
import org.hyperic.hq.operation.rabbit.mapping.OperationRouting;
import org.hyperic.hq.operation.rabbit.mapping.RabbitBindingsRegistry;
import org.hyperic.hq.operation.rabbit.util.Routings;
import org.hyperic.util.security.SecurityUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For the non-Spring Agent
 * @author Helena Edelson
 */
public class RabbitOperationDispatcher extends AbstractRabbitOperationEntity implements
        Dispatcher, OperationRegistry, OperationRoutingRegistry, OperationDiscoverer, OperationSupported {

    private final Map<String, MethodInvoker> operationDispatchers = new ConcurrentHashMap<String, MethodInvoker>();

    private final Map<String, OperationRouting> operationToRoutingKeyMappings = new ConcurrentHashMap<String, OperationRouting>();

    private final OperationDiscoverer operationDiscoverer;

    private final OperationRegistry operationRegistry;

    private final RabbitBindingsRegistry bindingsRegistry;

    public RabbitOperationDispatcher(ConnectionFactory connectionFactory, OperationDiscoverer operationDiscoverer, OperationRegistry operationRegistry) {
        super(new SimpleRabbitTemplate(connectionFactory));
        this.operationDiscoverer = operationDiscoverer;
        this.operationRegistry = operationRegistry;
        this.bindingsRegistry = new RabbitBindingsRegistry(connectionFactory);
    }

    public void discover(Object... dispatcherCandidates) {
        for (Object bean : dispatcherCandidates) {
            discover(bean, this.operationRegistry, this.operationRegistry);
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
                    registerRoutings(method.getAnnotation(Operation.class));
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

    public void register(String operationName, Method dispatcherMethod, Object dispatcherCandidate) {
        this.operationDispatchers.put(operationName, new MethodInvoker(dispatcherMethod, dispatcherCandidate, this.converter));
    }

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * @param operationRegistry the registry implementation to use
     * @param method            the operational method
     * @param dispatcher        the dispatcher class
     */
    void register(OperationRegistry operationRegistry, Method method, Object dispatcher) {
        this.operationRegistry.register(method.getAnnotation(Operation.class).operationName(), method, dispatcher);
    }

    /**
     * @param routingRegistry the registry implementation to use
     * @param operation       the operation routing meta-data
     */
    void register(OperationRoutingRegistry routingRegistry, Operation operation) {
       // this.routingRegistry.register(operation);
    }

    /**
     * Registers operation to routing key and exchange mappings
     * @param operation the operation to extract and register
     */
    public void registerRoutings(Operation operation) {
        this.operationToRoutingKeyMappings.put(operation.operationName(),
                new OperationRouting(operation.exchangeName(), operation.value()));
    }
    
    /**
     * Hand-off point from Hyperic to Rabbit
     * @param envelope
     */
    public void dispatch(Envelope envelope) {
        MethodInvoker methodInvoker = this.operationDispatchers.get(envelope.getOperationName());

        OperationRouting routing = this.operationToRoutingKeyMappings.get(envelope.getOperationName());

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

    /**
     * Tests whether the operation has been registered with this registry
     * This implementation supports query by type
     * @param operation The operation name
     * @return true if the operation name is a key in the handler's mapping, false if not
     */
    public boolean supports(OperationData operation) {
        return this.operationDispatchers.containsKey(operation.getOperationName());
    }

    /**
     * Depending on the RoutingType type,
     * @param operationName The operation's name
     * @return the specific mapping for a given operation
     */
    public OperationRouting getMapping(String operationName) {
        return operationToRoutingKeyMappings.get(operationName);
    }

    /**
     * Returns mappings for the given type
     * @return the mapping collection
     */
    public Map<String, OperationRouting> getMappings() {
        return this.operationToRoutingKeyMappings;
    }

    /**
     * Manual vs automated detection to accommodate the non-Spring Agent
     */
    private final class NonSpringRegistryBuilder {
        private int agents = 2;
        private Routings routings = new Routings();

        public void agentRoutingKeys() {

            for (int count = 0; count < agents; count++) {
                String agentToken = SecurityUtil.generateRandomToken();

                List<String> keys = routings.createAgentOperationRoutingKeys(agentToken);

                for (String key : keys) {
                    //testKey(key);
                    //registerOperationMapping(String operationName, String exchangeName, String value, RoutingType type) {
                }
            }
        }

        public void serverRoutingKeys() {
            for (String key : routings.createServerOperationRoutingKeys()) {
                //testKey(key);
            }
        }
    }
}
