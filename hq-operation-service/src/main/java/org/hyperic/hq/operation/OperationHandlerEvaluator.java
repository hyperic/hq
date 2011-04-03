package org.hyperic.hq.operation;
 
public interface OperationHandlerEvaluator {

    /**
     * Evaluates a handler candidate and if valid, registers it.
     * @param handlerCandidate The candidate instance
     * @param registry The registry to register with
     */
    void evaluate(Object handlerCandidate, OperationHandlerRegistry registry) throws OperationHandlerException;

}
