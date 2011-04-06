package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.rabbit.util.RoutingType;

import java.util.Map;

/**
 * @author Helena Edelson
 */
public interface OperationRoutingRegistry {

    /**
     * Registers the routing for the given operation name
     * @param operationName The operation name to map to the exchangeName
     * @param exchangeName  The exchangeName to use for the operationName
     * @param value         Either the binding pattern or the routing key to use
     * @param type          The routing type: binding pattern or routing key
     */
    void registerOperationMapping(String operationName, String exchangeName, String value, RoutingType type);

    /**
     * Map an operation to its Rabbit routing
     * @param name The operation's name
     * @param type The routing type: either binding pattern or routing key
     * @return The operation's exchange name to use
     */
    OperationRouting getMapping(String name, RoutingType type);

    /**
     * Returns the ConcurrentHashMap instance of mappings
     * @param type type The routing type: binding pattern or routing key
     * @return mappings
     */
    Map<String, OperationRouting> getMappings(RoutingType type);

}
