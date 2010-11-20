package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hyperic.hq.alert.domain.Alert;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.plugin.domain.PropertyType;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.datastore.annotation.Indexed;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.datastore.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.ManyToMany;
import javax.persistence.CascadeType;

@NodeEntity(partial=true)
@RooToString
@RooJavaBean
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@RooEntity
public class Resource {

    //TODO do I need Indexed and GraphProperty?
	@NotNull
    @Indexed
    @GraphProperty
    @Transient
    private String name;

    @Transient
    @ManyToOne
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ResourceType.class)
    private ResourceType type;

    //TODO why can't I push these instance vars in?
    @javax.annotation.Resource
    protected transient FinderFactory finderFactory2;

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext2;

    @OneToMany
    @Transient
    private Set<Alert> alerts;

    @Transactional
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
    
    @Transactional
    public void removeRelationship(Resource resource, String relationName, boolean junk) {
        //TODO what is the junk boolean?
    	if (this.isRelatedTo(resource, relationName)) {
    		this.removeRelationshipTo(resource, relationName);
    	}
    }
    
    public ResourceRelation getRelationshipTo(Resource resource, String relationName) {
    	//TODO this doesn't take direction into account
        return (ResourceRelation) getRelationshipTo(resource, ResourceRelation.class, relationName);
    }

    public Set<ResourceRelation> getRelationships() {
    	// TODO This is hardcoded for the demo, however should be able to specify direction/relationship name via parameters
        Iterable<Relationship> relationships = getUnderlyingState().getRelationships(org.neo4j.graphdb.Direction.OUTGOING);
        Set<ResourceRelation> resourceRelations = new HashSet<ResourceRelation>();
        for (Relationship relationship : relationships) {
        	//Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Class<?> otherEndType = graphDatabaseContext2.getJavaType(relationship.getOtherNode(getUnderlyingState()));
                //TODO does instanceOf work here?
                if(Resource.class.equals(otherEndType) || ResourceGroup.class.equals(otherEndType)) {
                    resourceRelations.add(graphDatabaseContext2.createEntityFromState(relationship, ResourceRelation.class));
                }
            }
        }
        return resourceRelations;
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
        return new Resource().finderFactory2.getFinderForClass(Resource.class).findByPropertyValue("name", name);
    }

    public void setProperty(String key, Object value) {
        if (type.getPropertyType(key) == null) {
            throw new IllegalArgumentException("Property " + key + " is not defined for resource of type " + type.getName());
        }
        //TODO check other stuff?
        getUnderlyingState().setProperty(key, value);
    }

    public Object getProperty(String key) {
        PropertyType propertyType = type.getPropertyType(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Property " + key + " is not defined for resource of type " + type.getName());
        }
        return getUnderlyingState().getProperty(key, propertyType.getDefaultValue());
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : getUnderlyingState().getPropertyKeys()) {
            try {
                properties.put(key, getProperty(key));
            } catch (IllegalArgumentException e) {
            	//filter out the properties we've defined at class-level, like name
            }
        }
        return properties;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
        //TODO this call appears to be necessary to get Alert populated with its underlying node
        getId();
    }
}
