package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.transaction.annotation.Transactional;

/**
 * A managed resource
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Entity
@NodeEntity(partial = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Resource {

    @Transient
    @RelatedTo(type = RelationshipTypes.MANAGED_BY, direction = Direction.OUTGOING, elementClass = Agent.class)
    private Agent agent;

    @GraphProperty
    @Transient
    private String description;

    @PersistenceContext
    transient EntityManager entityManager;

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @GraphProperty
    @Transient
    private String location;

    @GraphProperty
    @Transient
    private String modifiedBy;

    @NotNull
    @Indexed
    @GraphProperty
    @Transient
    private String name;

    @Transient
    @RelatedTo(type = RelationshipTypes.OWNS, direction = Direction.INCOMING, elementClass = AuthzSubject.class)
    private AuthzSubject owner;

    @Transient
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ResourceType.class)
    private ResourceType type;

    public Resource() {
    }

    /**
     * 
     * @param name The resource name
     * @param type The resource type
     */
    public Resource(String name, ResourceType type) {
        this.name = name;
        this.type = type;
    }

    private Set<ResourceRelationship> convertRelationships(Resource entity,
                                                           Iterable<org.neo4j.graphdb.Relationship> relationships) {
        Set<ResourceRelationship> relations = new HashSet<ResourceRelationship>();
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Node node = relationship.getOtherNode(getUnderlyingState());
                Class<?> otherEndType = graphDatabaseContext.getJavaType(node);
                if (Resource.class.isAssignableFrom(otherEndType)) {
                    if (entity == null || node.equals(entity.getUnderlyingState())) {
                        relations.add(graphDatabaseContext.createEntityFromState(relationship,
                            ResourceRelationship.class));
                    }
                }
            }
        }
        return relations;
    }

    /**
     * 
     * @return The agent managing this resource
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * 
     * @param recursive true if return children of children, etc
     * @return The children of this resource (associated by a
     *         {@link RelationshipTypes.CONTAINS} relationship)
     */
    public Set<Resource> getChildren(boolean recursive) {
        Set<Resource> children = new HashSet<Resource>();
        StopEvaluator stopEvaluator;
        if (recursive) {
            stopEvaluator = StopEvaluator.END_OF_GRAPH;
        } else {
            stopEvaluator = new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            };
        }
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            stopEvaluator, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.CONTAINS),
            Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            children.add(graphDatabaseContext.createEntityFromState(related, Resource.class));
        }
        return children;
    }

    /**
     * 
     * @param recursive true if return children of children, etc
     * @return The IDs of children of this resource (associated by a
     *         {@link RelationshipTypes.CONTAINS} relationship)
     */
    public Set<Integer> getChildrenIds(boolean recursive) {
        Set<Integer> children = new HashSet<Integer>();
        StopEvaluator stopEvaluator;
        if (recursive) {
            stopEvaluator = StopEvaluator.END_OF_GRAPH;
        } else {
            stopEvaluator = new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            };
        }
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            stopEvaluator, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.CONTAINS),
            Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            children.add((Integer) related.getProperty("foreignId"));
        }
        return children;
    }

    /**
     * 
     * @param type The type of config to get (for example, "measurement" or
     *        "control")
     * @return The Config or null if this resource has no config of specified
     *         type
     */
    public Config getConfig(String type) {
        Iterable<org.neo4j.graphdb.Relationship> relationships = this.getUnderlyingState()
            .getRelationships(DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG),
                Direction.OUTGOING.toNeo4jDir());
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            if (type.toString().equals(relationship.getProperty("configType"))) {
                // TODO enforce no more than one?
                return graphDatabaseContext.createEntityFromState(
                    relationship.getOtherNode(getUnderlyingState()), Config.class);
            }
        }
        return null;
    }

    public String getConfigValidationError() {
        // TODO from ConfigResponseDB. remove?
        return null;
    }

    /**
     * 
     * @return A description of this Resource
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @return The Resource Id
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 
     * @return The Resource location
     */
    public String getLocation() {
        return location;
    }

    /**
     * 
     * @return The name of the user who last modified this Resource
     */
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * 
     * @return The Resource name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return The owner of this Resource
     */
    public AuthzSubject getOwner() {
        return owner;
    }

    /**
     * 
     * @return A Map of property keys and values
     */
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : getUnderlyingState().getPropertyKeys()) {
            try {
                properties.put(key, getProperty(key));
            } catch (IllegalArgumentException e) {
                // filter out the properties we've defined at class-level, like
                // name
            }
        }
        return properties;
    }

    /**
     * 
     * @param key The property key
     * @return The property value
     * @throws IllegalArgumentException If the property is not defined on the
     *         {@link ResourceType}
     */
    public Object getProperty(String key) {
        PropertyType propertyType = type.getPropertyType(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        try {
            return getUnderlyingState().getProperty(key);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private Set<Resource> getRelatedResources(String relationName, Direction direction) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), direction.toNeo4jDir());
        for (Node related : relationTraverser) {
            Resource resource = graphDatabaseContext.createEntityFromState(related, Resource.class);
            resource.getId();
            resources.add(resource);
        }
        return resources;
    }

    /**
     * 
     * @return All the relationships this Resource is involved in (both Incoming
     *         and Outgoing)
     */
    public Set<ResourceRelationship> getRelationships() {
        return convertRelationships(null, getUnderlyingState().getRelationships());
    }

    /**
     * 
     * @param entity The Resource this resource is related to
     * @param name The name of the relationship
     * @param direction The direction of the relationship
     * @return The relationships (more than one if direction is
     *         {@link Direction.BOTH})
     */
    public Set<ResourceRelationship> getRelationships(Resource entity, String name,
                                                      Direction direction) {
        return convertRelationships(
            entity,
            getUnderlyingState().getRelationships(DynamicRelationshipType.withName(name),
                direction.toNeo4jDir()));
    }

    /**
     * 
     * @param relationName The relationship name
     * @return All relations of specified name that are Outgoing from this
     *         Resource
     */
    public Set<ResourceRelationship> getRelationshipsFrom(String relationName) {
        return convertRelationships(
            null,
            getUnderlyingState().getRelationships(DynamicRelationshipType.withName(relationName),
                Direction.OUTGOING.toNeo4jDir()));
    }

    /**
     * 
     * @param relationName The relationship name
     * @return All relations of specified name that are Incoming to this
     *         Resource
     */
    public Set<ResourceRelationship> getRelationshipsTo(String relationName) {
        return convertRelationships(
            null,
            getUnderlyingState().getRelationships(DynamicRelationshipType.withName(relationName),
                Direction.INCOMING.toNeo4jDir()));
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The single relationship of specified name that is Incoming to
     *         this Resource, or null relationship does not exist
     * @throw {@link NotUniqueException} If more than one relationship exists
     */
    public ResourceRelationship getRelationshipTo(Resource resource, String relationName) {
        Set<ResourceRelationship> relations = convertRelationships(resource, getUnderlyingState()
            .getRelationships(DynamicRelationshipType.withName(relationName)));
        if (relations.isEmpty()) {
            return null;
        }
        return relations.iterator().next();
    }

    /**
     * 
     * @param relationName The relationship name
     * @return A single end node of specified relationship
     * @throws NotUniqueException If multiple relationships exist
     */
    public Resource getResourceFrom(String relationName) {
        Set<Resource> resources = getRelatedResources(relationName, Direction.OUTGOING);
        if (resources.isEmpty()) {
            return null;
        }
        if (resources.size() > 1) {
            throw new NotUniqueException();
        }
        return resources.iterator().next();
    }

    /**
     * 
     * @param relationName The relationship name
     * @return All end nodes of specified relationship
     */
    public Set<Resource> getResourcesFrom(String relationName) {
        return getRelatedResources(relationName, Direction.OUTGOING);
    }

    /**
     * 
     * @param relationName The relationship name
     * @return All start nodes of specified relationship
     */
    public Set<Resource> getResourcesTo(String relationName) {
        return getRelatedResources(relationName, Direction.INCOMING);
    }

    /**
     * 
     * @param relationName The relationship name
     * @return A single start node of specified relationship
     * @throws NotUniqueException If multiple relationships exist
     */
    public Resource getResourceTo(String relationName) {
        Set<Resource> resources = getRelatedResources(relationName, Direction.INCOMING);
        if (resources.isEmpty()) {
            return null;
        }
        if (resources.size() > 1) {
            throw new NotUniqueException();
        }
        return resources.iterator().next();
    }

    /**
     * 
     * @return The Resource type
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * 
     * @param resource The potential child
     * @param recursive true if we should consider deep ancestry
     * @return true if the specified relationship is a child
     */
    public boolean hasChild(Resource resource, boolean recursive) {
        if (getChildren(recursive).contains(resource)) {
            return true;
        }
        return false;
    }

    public boolean isConfigUserManaged() {
        // TODO from ConfigResponseDB. remove?
        return true;
    }

    public boolean isInAsyncDeleteState() {
        // TODO remove
        return false;
    }

    /**
     * 
     * @param subjectId The ID of a user
     * @return true if the specified user is the owner of this Resource
     */
    public boolean isOwner(Integer subjectId) {
        // TODO some overlord checking
        if (this.owner == null) {
            return false;
        }
        return owner.getId().equals(subjectId);
    }

    /**
     * 
     * @param resource The resource to test relation to
     * @param relationName The name of the relationship
     * @return true if this resource is directly related to the supplied
     *         Resource by Outgoing relationship
     */
    public boolean isRelatedTo(Resource resource, String relationName) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            if (related.equals(resource.getUnderlyingState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param resource The resource to create a relationship to
     * @param relationName The name of the relationship
     * @return The created relationship
     */
    @Transactional
    public ResourceRelationship relateTo(Resource resource, String relationName) {
        if (!(RelationshipTypes.CONTAINS.equals(relationName)) &&
            !type.isRelatedTo(resource.getType(), relationName)) {
            throw new InvalidRelationshipException();
        }
        return (ResourceRelationship) this.relateTo(resource, ResourceRelationship.class,
            relationName);
    }

    /**
     * Remove this resource, including all related Config and properties and
     * relationships
     */
    @Transactional
    public void remove() {
        removeConfig();
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Resource attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeConfig() {
        Iterable<org.neo4j.graphdb.Relationship> relationships = this.getUnderlyingState()
            .getRelationships(DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG),
                Direction.OUTGOING.toNeo4jDir());
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            graphDatabaseContext.createEntityFromState(
                relationship.getOtherNode(getUnderlyingState()), Config.class).remove();
        }
    }

    /**
     * Remove all properties of this Resource
     */
    @Transactional
    public void removeProperties() {
        for (String key : getUnderlyingState().getPropertyKeys()) {
            getUnderlyingState().removeProperty(key);
        }
    }

    /**
     * Remove all relationships this Resource is involved in
     */
    @Transactional
    public void removeRelationships() {
        for (org.neo4j.graphdb.Relationship relationship : getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
    }

    /**
     * Removes specific relationships
     * @param resource The resource related to
     * @param relationName The name of the relationship
     */
    @Transactional
    public void removeRelationships(Resource resource, String relationName) {
        removeRelationships(resource, relationName, Direction.BOTH);
    }

    /**
     * Removes specific relationships
     * @param entity The resource related to
     * @param name The name of the relationship
     * @param direction The {@link Direction} of the relationship to remove
     */
    @Transactional
    public void removeRelationships(Resource entity, String name, Direction direction) {
        for (ResourceRelationship relation : getRelationships(entity, name, direction)) {
            relation.getUnderlyingState().delete();
        }
    }

    /**
     * Remove specific relationships
     * @param relationName The name of the relationship
     */
    @Transactional
    public void removeRelationships(String relationName) {
        for (org.neo4j.graphdb.Relationship relationship : getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.BOTH.toNeo4jDir())) {
            relationship.delete();
        }
    }

    /**
     * 
     * @param agent The agent that manages this Resource
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * Sets config for this resource
     * @param configType The type of config (example "measurement" or "control")
     * @param config The config
     */
    @Transactional
    public void setConfig(String configType, Config config) {
        // Config may not be persisted yet since we don't provide a direct way
        // to persist via API
        if (!config.hasUnderlyingNode()) {
            entityManager.persist(config);
            config.getId();
        }
        // TODO change config after set?
        org.neo4j.graphdb.Relationship rel = this.getUnderlyingState().createRelationshipTo(
            config.getUnderlyingState(),
            DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG));
        rel.setProperty("configType", configType);
    }

    public void setConfigUserManaged(boolean userManaged) {
        // TODO from ConfigResponseDB. remove?
    }

    public void setConfigValidationError(String error) {
        // TODO from ConfigResponseDB. remove?
    }

    /**
     * 
     * @param description The Resource description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @param id The ID of the Resource
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @param location The Resource location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 
     * @param modifiedBy The user that last modified the Resource
     */
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * 
     * @param name The name of the Resource
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param owner The resource owner
     */
    public void setOwner(AuthzSubject owner) {
        this.owner = owner;
    }

    /**
     * 
     * @param key The property name
     * @param value The property value
     * @return The previous property value
     * @throws IllegalArgumentException If the property is not defined for the
     *         {@link ResourceType}
     */
    @Transactional
    public Object setProperty(String key, Object value) {
        PropertyType propertyType = type.getPropertyType(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        if (value == null) {
            Object oldValue = getUnderlyingState().removeProperty(key);
            if (propertyType.isIndexed()) {
                graphDatabaseContext.getNodeIndex(null).remove(getUnderlyingState(), key, value);
            }
            return oldValue;
        }
        // TODO check other stuff? Should def check optional param
        Object oldValue = null;
        try {
            oldValue = getUnderlyingState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getUnderlyingState().setProperty(key, value);
        if (propertyType.isIndexed()) {
            graphDatabaseContext.getNodeIndex(null).add(getUnderlyingState(), key, value);
        }
        return oldValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Type: ").append(getType().getName()).append("]");
        return sb.toString();
    }
}