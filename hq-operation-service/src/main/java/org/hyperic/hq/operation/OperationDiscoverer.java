package org.hyperic.hq.operation;

/**
 * Implemented by dispatcher and endpoint discoverers
 * @author Helena Edelson
 */
public interface OperationDiscoverer {

    /**
     * Evaluates a discoverer candidate and if valid, registers it.
     * @param candidate The candidate instance which can be a dispatcher or endpoint
     * @param registry The registry to register with 
     * @throws OperationDiscoveryException
     */
    void discover(Object candidate, OperationRegistry registry) throws OperationDiscoveryException;

}
