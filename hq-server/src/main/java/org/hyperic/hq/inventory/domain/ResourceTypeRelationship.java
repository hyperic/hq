package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class ResourceTypeRelationship {

    @StartNode
    private ResourceType from;

    @EndNode
    private ResourceType to;

    public ResourceTypeRelationship() {
    }

    public ResourceTypeRelationship(org.neo4j.graphdb.Relationship r) {
        setUnderlyingState(r);
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

}
