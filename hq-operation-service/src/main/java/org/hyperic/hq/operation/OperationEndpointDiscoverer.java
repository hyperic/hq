package org.hyperic.hq.operation;
 
public interface OperationEndpointDiscoverer {

    /**
     * Evaluates an endpoint candidate and if valid, registers it.
     * @param endpointCandidate The candidate instance
     * @param registry The registry to register with
     * @throws OperationEndpointException
     */
    void discover(Object endpointCandidate, OperationEndpointRegistry registry) throws OperationEndpointException;

}
