package org.hyperic.hq.authz;

import org.hyperic.hq.authz.shared.AuthzConstants;


public interface HasAuthzOperations {
    /**
     * Get the Authz permission to perform the operation.
     *
     * e.g. getAuthzOp('create')   // -> "createServer"
     *      getAuthzOp('modify')   // -> "modifyServer"
     * 
     * remove, add, view, monitor, control, manage
     * 
     * 
     * Classes implementing this interface should return the associated
     * operation type (as in {@link AuthzConstants}), or throw an InvalidArgument 
     * exception.
     */
    String getAuthzOp(String op);
}
