package org.hyperic.hq.inventory.domain;

import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@NodeEntity
@RooToString
@RooJavaBean
@RooEntity
public class Resource {

    @NotNull
    private String name;

    @ManyToOne
    @NotNull
    private ResourceType type;

    public ResourceRelation relateTo(Resource resource, String relationName) {
        // TODO only allow certain relation names or RelationTypes by enum?
        // Check the metadata to ensure these resource types can be related with
        // this name
        if (type.getName().equals("System")) {
            if (!(relationName.equals("CONTAINS"))) {
                throw new InvalidRelationshipException();
            }
        } else if (!type.isRelatedTo(resource.getType(), relationName)) {
            throw new InvalidRelationshipException();
        }
        return (ResourceRelation) this.relateTo(resource, ResourceRelation.class, relationName);
    }

    public boolean isRelatedTo(Resource resource, String relationName) {
        // TODO more efficient way to find out if one node related to other?
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                @Override
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), Direction.OUTGOING);
        for (Node related : relationTraverser) {
            // TODO will node IDs always be around for uniqueness?
            if (related.getId() == resource.getId()) {
                return true;
            }
        }

        return false;
        // Code below will return true if relationship in either direction
        // if (getRelationshipTo(resource, ResourceRelation.class, relationName)
        // != null) {
        // return true;
        // }
        // return false;
    }
}
