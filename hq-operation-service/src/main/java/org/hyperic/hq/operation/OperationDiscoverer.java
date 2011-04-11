package org.hyperic.hq.operation;
 
import java.lang.annotation.Annotation;

/**
 * Implemented by dispatcher and endpoint discoverers
 * @author Helena Edelson
 */
public interface OperationDiscoverer {

    /**
     * Evaluates a discoverer candidate and if valid, registers it.
     * @param candidate The candidate instance which can be a dispatcher or endpoint
     * @throws OperationDiscoveryException
     */
    void discover(Object candidate, Class<? extends Annotation> annotation) throws OperationDiscoveryException;
 
}
