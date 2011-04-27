package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents an operation that can be performed against Resources of the
 * associated ResourceType
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity
public class OperationType {

    @GraphProperty
    @NotNull
    private String name;

    @GraphProperty
    private String returnType;

    @RelatedTo(type = RelationshipTypes.OPERATION_ARG_TYPE, direction = Direction.OUTGOING, elementClass = OperationArgType.class)
    private Set<OperationArgType> operationArgTypes;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    public OperationType() {
    }

    /**
     * 
     * @param name The operation name
     */
    public OperationType(String name) {
        this.name = name;
    }

    /**
     * Adds metadata about arguments to be passed to the operation
     * @param argType The argument type for the operation
     */
    @Transactional("neoTxManager")
    public void addOperationArgType(OperationArgType argType) {
        // TODO can't do this in a detached env b/c relationship doesn't take
        // unless both items are node-backed
        argType.persist();
        operationArgTypes.add(argType);
    }
    
    /**
     * Adds metadata about arguments to be passed to the operation
     * @param argTypes The argument types for the operation
     */
    @Transactional("neoTxManager")
    public void addOperationArgTypes(Set<OperationArgType> argTypes) {
        // TODO can't do this in a detached env b/c relationship doesn't take
        // unless both items are node-backed
        for(OperationArgType argType: argTypes) {
            argType.persist();
        }
        this.operationArgTypes.addAll(argTypes);
    }

    /**
     * 
     * @return The operation name
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     * @return Arguments to be passed to the operation
     */
    public Set<OperationArgType> getOperationArgTypes() {
        return operationArgTypes;
    }

    /**
     * 
     * @return The return value type or null if operation has no return value
     */
    public String getReturnType() {
        return this.returnType;
    }

    /**
     * Removes the OperationType. Only supported as part of ResourceType removal
     */
    @Transactional("neoTxManager")
    public void remove() {
        removeArgTypes();
        graphDatabaseContext.removeNodeEntity(this);
    }

    private void removeArgTypes() {
        for (OperationArgType argType : operationArgTypes) {
            argType.remove();
        }
    }

    /**
     * 
     * @param returnType The type of the operation return value
     */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OperationType[");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}