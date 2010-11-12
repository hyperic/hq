package org.hyperic.hq.plugin.domain;

import javax.validation.constraints.NotNull;

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
public class ResourceType {

    @NotNull
    private String name;

    public ResourceTypeRelation relateTo(ResourceType resourceType, String relationName) {
        return (ResourceTypeRelation) this.relateTo(resourceType, ResourceTypeRelation.class,
            relationName);
    }

    public boolean isRelatedTo(ResourceType resourceType, String relationName) {
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
            if (related.getId() == resourceType.getId()) {
                return true;
            }
        }

        // TODO traverse resource type hierarchy for relationship from
        // superclasses

        // Code below returns true even if not directed
        //
        // if(
        // getRelationshipTo(resourceType,ResourceTypeRelation.class,relationName)
        // != null) {
        // return true;
        // }
        return false;
    }

}
