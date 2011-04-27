package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Represents an argument to a control operation
 * @author jhickey
 * 
 */

@NodeEntity
@Configurable
public class OperationArgType {

    @GraphProperty
    @NotNull
    private String name;

    @GraphProperty
    @NotNull
    private Class<?> type;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    public OperationArgType() {
    }

    /**
     * 
     * @param name The operation argument name
     * @param type The type of the operation argument
     */
    public OperationArgType(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 
     * @return The operation argument name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return The type of the operation argument
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Removes this arg type. Only supported with removal of entire
     * encapsulating ResourceType
     */
    @Transactional("neoTxManager")
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
    }

    /**
     * 
     * @param name The operation argument name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param type The type of the operation argument
     */
    public void setType(Class<?> type) {
        this.type = type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OperationArgType[");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Type: ").append(getType()).append("]");
        return sb.toString();
    }

}
