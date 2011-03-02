package org.hyperic.hq.inventory.domain;

import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

/**
 * Metadata for relationships that can be created between Resources
 * @author jhickey
 * @author dcrutchfield
 * 
 */

@RelationshipEntity
public class ResourceTypeRelationship {

    @StartNode
    private ResourceType from;

    @EndNode
    private ResourceType to;

    public ResourceTypeRelationship() {
    }

    /**
     * 
     * @return The type of the relationship start node
     */
    public ResourceType getFrom() {
        return from;
    }

    /**
     * 
     * @return The relationship name
     */
    public String getName() {
        return getUnderlyingState().getType().name();
    }

    /**
     * 
     * @return The type of the relationship end node
     */
    public ResourceType getTo() {
        return to;
    }

    /**
     * 
     * @param from The type of the relationship start node
     */
    public void setFrom(ResourceType from) {
        this.from = from;
    }

    /**
     * 
     * @param to The type of the relationship end node
     */
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
