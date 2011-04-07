package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.annotation.Operation;

/**
 * @author Helena Edelson
 */
public interface BindingRegistry {

    void registerBinding(final Operation operation);
    
}
