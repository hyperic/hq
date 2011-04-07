package org.hyperic.hq.operation.rabbit.util;

import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationEndpointException;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public class DiscoveryValidator {

    public void validateReturnType(Method candidateMethod, Object candidate) throws OperationDiscoveryException {
        if (void.class.equals(candidateMethod.getReturnType())) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have a non-void return type.",
                        candidateMethod, candidate));
        }
    }

   public void validateParameterTypes(Method candidateMethod, Object candidate) throws OperationDiscoveryException {
        if (candidateMethod.getParameterTypes().length != 1) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have exactly one parameter",
                        candidateMethod, candidate));
        }
    }

    public boolean validArguments(String operationName, String exchangeName, String value) {
        return operationName == null || exchangeName == null || value == null;
    }
}
