package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class ResourceTypeRelationship {

    @StartNode
    private ResourceType from;

    @EndNode
    private ResourceType to;

    public ResourceTypeRelationship() {
    }

    public String getName() {
        return getUnderlyingState().getType().name();
    }

    public ResourceType getFrom() {
        return from;
    }

    public void setFrom(ResourceType from) {
        this.from = from;
    }

    public ResourceType getTo() {
        return to;
    }

    public void setTo(ResourceType to) {
        this.to = to;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceTypeRelationship[");
        sb.append("From: ").append(getFrom()).append(", ");
        sb.append("To: ").append(getTo()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }

}
