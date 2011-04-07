package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public interface OperationDispatcherDiscoverer {

    /**
     * Evaluates a dispatcher candidate and if valid, registers it.
     * @param dispatcherCandidate The candidate instance
     * @param registry          The registry to register with
     * @throws OperationDispatcherException
     */
    void discover(Object dispatcherCandidate, OperationDispatcherRegistry registry) throws OperationDispatcherException;

}
