package org.hyperic.hq.operation;

import org.hyperic.hq.operation.annotation.Operation;

/**
 * @author Helena Edelson
 */
public interface OperationRoutingRegistry {

    /**
     * Registers the routing for the given operation name
     * @param operation The operation meta-data to map to the exchangeName and routing key
     * or binding pattern by RoutingKey type
     */
    void registerRoutings(Operation operation);
  
}
