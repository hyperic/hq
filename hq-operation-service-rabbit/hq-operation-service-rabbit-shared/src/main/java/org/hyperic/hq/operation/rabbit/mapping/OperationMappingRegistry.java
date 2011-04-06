package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
public class OperationMappingRegistry implements OperationRoutingRegistry, OperationRoutingSupported {

    private final Map<String, OperationRouting> operationToBindingPatternMappings = new ConcurrentHashMap<String, OperationRouting>();

    private final Map<String, OperationRouting> operationToRoutingKeyMappings = new ConcurrentHashMap<String, OperationRouting>();

    
    /**
     * @see OperationRoutingRegistry
     * @param operationName The operation name to map to the exchangeName
     * @param exchangeName  The exchangeName to use for the operationName
     * @param value Either the binding pattern or the routing key to use
     */
    public void registerOperationMapping(String operationName, String exchangeName, String value, RoutingType type) {
        if (operationName == null || exchangeName == null || value == null)
                throw new IllegalArgumentException("All method parameters must not be null");

        if (type.equals(RoutingType.ROUTING_KEY)) {
            this.operationToBindingPatternMappings.put(operationName, new OperationRouting(type, exchangeName, value));
        }
        else {
            this.operationToRoutingKeyMappings.put(operationName, new OperationRouting(type, exchangeName, value));
        }
    }
    
    /**
     * Depending on the RoutingType type,
     * @param operationName The operation's name
     * @param type The RoutingType, either binding pattern or routing key
     * @return the specific mapping for a given operation
     */
    public OperationRouting getMapping(String operationName, RoutingType type) {
            return type.equals(RoutingType.ROUTING_KEY) ? operationToRoutingKeyMappings.get(operationName)
                    : operationToBindingPatternMappings.get(operationName);
    }

    /**
     * Returns mappings for the given type
     * @param type type The routing type: binding pattern or routing key
     * @return the mapping collection
     */
    public Map<String, OperationRouting> getMappings(RoutingType type) {
        return type.equals(RoutingType.ROUTING_KEY) ? operationToRoutingKeyMappings : operationToBindingPatternMappings;
    }

    /**
     * Tests whether the operation has been registered with this handler
     * @param operation The operation name
     * @return Returns true if the operation name is a key in the handler's mapping, false if not
     */
    public boolean supports(Operation operation, RoutingType type) {
       return type.equals(RoutingType.ROUTING_KEY) ?
               this.operationToRoutingKeyMappings.containsKey(operation.getOperationName())
                    : this.operationToBindingPatternMappings.containsKey(operation.getOperationName());
    }

    /**
     * This implementation supports query by type only
     * @param operation The operation name
     * @return
     */
    public boolean supports(Operation operation) {
        return false;
    }
}
