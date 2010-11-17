package org.hyperic.hq.plugin.domain;

import java.util.Set;

import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.inventory.domain.Resource;
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
@RooEntity
public class ResourceType {
	
    @NotNull
    @Indexed
    private String name;
    
    //TODO was using IS_A to relate ResourceTypes to ResourceTypes also, but somehow those related ResourceTypes were ending up in this Set
    //causing ClassCastExceptions.  Switched to using EXTENDS for ResourceTypes, but still strange issue similar to Resource.findRelationships
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.INCOMING, elementClass = org.hyperic.hq.inventory.domain.Resource.class)
    @OneToMany
    private Set<Resource> resources;
    
    @RelatedTo(type = RelationshipTypes.CONTAINS, direction = Direction.OUTGOING, elementClass = PropertyType.class)
    @OneToMany
    private Set<PropertyType> propertyTypes;
    
    @javax.annotation.Resource
    private FinderFactory finderFactory2;

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
            DynamicRelationshipType.withName(relationName), org.neo4j.graphdb.Direction.OUTGOING);
        for (Node related : relationTraverser) {
            if (related.getId() == resourceType.getId()) {
                return true;
            }
        }
        return false;
    }

    public static ResourceType findResourceTypeByName(String name) {
        return new ResourceType().finderFactory2.getFinderForClass(ResourceType.class)
            .findByPropertyValue("name", name);
    }
    
    public PropertyType getPropertyType(String name) {
        for(PropertyType propertyType: propertyTypes) {
            if(name.equals(propertyType.getName())) {
                return propertyType;
            }
        }
        return null;
    }

}
