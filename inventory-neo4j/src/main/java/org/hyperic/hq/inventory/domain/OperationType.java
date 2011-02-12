package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;

@Configurable
@NodeEntity
public class OperationType {
  
    @GraphId
    private Integer id;

    @NotNull
    private String name;

    @RelatedTo(type = RelationshipTypes.HAS_OPERATION_TYPE, direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    public OperationType() {

    }
    
    public OperationType(String name) {
        this.name=name;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("ResourceType: ").append(getResourceType());
        return sb.toString();
    }
}