package org.hyperic.hq.inventory.domain;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class Neo4jResourceTypeRelationship {

    @StartNode
    private Neo4jResourceType from;

    @EndNode
    private Neo4jResourceType to;

    public Neo4jResourceTypeRelationship() {
    }

    public String getName() {
        return getUnderlyingState().getType().name();
    }

    public Neo4jResourceType getFrom() {
        return from;
    }

    public void setFrom(Neo4jResourceType from) {
        this.from = from;
    }

    public Neo4jResourceType getTo() {
        return to;
    }

    public void setTo(Neo4jResourceType to) {
        this.to = to;
    }

}
