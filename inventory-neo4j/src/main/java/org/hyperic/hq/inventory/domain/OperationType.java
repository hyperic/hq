package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;

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

    @GraphId
    private Integer id;

    @NotNull
    private final String name;

    /**
     * 
     * @param name The operation name
     */
    public OperationType(String name) {
        this.name = name;
    }

    /**
     * 
     * @return The ID
     */
    public Integer getId() {
        return this.id;
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
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OperationType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}