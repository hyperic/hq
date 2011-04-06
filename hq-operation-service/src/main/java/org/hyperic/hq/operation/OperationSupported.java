package org.hyperic.hq.operation;

/**
 * @author Helena Edelson
 */
public interface OperationSupported {

    /**
     * Tests whether the operation has been registered with this handler
     * @param operation The operation name
     * @return Returns true if the operation name is a key in the handler's mapping, false if not
     */
    boolean supports(Operation operation);
    
}
