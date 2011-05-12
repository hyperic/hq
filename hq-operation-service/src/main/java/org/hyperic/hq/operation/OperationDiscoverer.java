package org.hyperic.hq.operation;

/**
 * Implemented by dispatcher and endpoint discoverers
 * @author Helena Edelson
 */
public interface OperationDiscoverer {

    /**
     * Discovers, evaluates, validates and registers candidates.
     * @param candidate  the dispatcher candidate class 
     * @throws org.hyperic.hq.operation.OperationDiscoveryException
     */
    void discover(Object candidate) throws OperationDiscoveryException;
 
}
