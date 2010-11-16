package org.hyperic.hq.inventory.domain;


import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.datastore.annotation.Indexed;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;



@NodeEntity
@RooToString
@RooJavaBean
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@RooEntity
public class Resource {

    @NotNull
    @Indexed
    private String name;

    @ManyToOne(targetEntity = Resource.class)
    @NotNull
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ResourceType.class)
    private ResourceType type;
    
    @javax.annotation.Resource
    protected FinderFactory finderFactory;

    public ResourceRelation relateTo(Resource resource, String relationName) {
        if (type.getName().equals("System")) {
            if (!(relationName.equals(RelationshipTypes.CONTAINS))) {
                throw new InvalidRelationshipException();
            }
        } else if (!type.isRelatedTo(resource.getType(), relationName)) {
            throw new InvalidRelationshipException();
        }
        return (ResourceRelation) this.relateTo(resource, ResourceRelation.class, relationName);
    }

    public boolean isRelatedTo(Resource resource, String relationName) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST, new StopEvaluator() {

            @Override
            public boolean isStopNode(TraversalPosition currentPos) {
                return currentPos.depth() >= 1;
            }
        }, ReturnableEvaluator.ALL_BUT_START_NODE, DynamicRelationshipType.withName(relationName), org.neo4j.graphdb.Direction.OUTGOING);
        for (Node related : relationTraverser) {
            if (related.getId() == resource.getId()) {
                return true;
            }
        }
        return false;
    }
    
    
    public static Resource findResourceByName(String name) {
        return new Resource().finderFactory.getFinderForClass(Resource.class).findByPropertyValue("name", name);
    }
  
}
