package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.events.CPropChangeEvent;
import org.hyperic.hq.messaging.MessagePublisher;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.annotation.Indexed;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.typerepresentation.SubReferenceNodeTypeRepresentationStrategy;
import org.springframework.transaction.annotation.Transactional;

/**
 * A managed resource
 * @author jhickey
 * @author dcrutchfield
 * 
 */

@NodeEntity
@Configurable
public class Resource {

    @RelatedTo(type = RelationshipTypes.HAS_CONFIG, direction = Direction.OUTGOING, elementClass = Config.class)
    private Set<Config> configs;

    @GraphProperty
    private String description;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    // TODO unique ID string instead of number
    @GraphProperty
    private Integer id;

    @GraphProperty
    private String location;

    @Autowired
    private transient MessagePublisher messagePublisher;

    @GraphProperty
    private String modifiedBy;

    @NotNull
    @Indexed
    @GraphProperty
    private String name;

    @GraphProperty
    @Indexed
    private String owner;

    @NotNull
    @Indexed
    @GraphProperty
    private String sortName;

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
        setName(name);
        this.type = type;
    }

    /**
     * Adds config for this resource
     * @param config The config, whose ConfigType should be one supported by
     *        this Resource's ResourceType
     */
    @Transactional("neoTxManager")
    public void addConfig(Config config) {
        // TODO can't do this in a detached env b/c relationship doesn't take
        // unless both items are node-backed
        String cfgType = config.getType().getName();
        if (type.getConfigType(cfgType) == null) {
            throw new IllegalArgumentException("Config " + cfgType +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        configs.add(config);
    }

    private Set<ResourceRelationship> convertRelationships(Resource entity,
                                                           Iterable<org.neo4j.graphdb.Relationship> relationships) {
        Set<ResourceRelationship> relations = new HashSet<ResourceRelationship>();
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship
                .isType(SubReferenceNodeTypeRepresentationStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Node node = relationship.getOtherNode(getPersistentState());
                Class<?> otherEndType = graphDatabaseContext.getNodeTypeRepresentationStrategy()
                    .getJavaType(node);
                if (Resource.class.isAssignableFrom(otherEndType)) {
                    if (entity == null || node.equals(entity.getPersistentState())) {
                        relations.add(graphDatabaseContext.createEntityFromState(relationship,
                            ResourceRelationship.class));
                    }
                }
            }
        }
        return relations;
    }

    @SuppressWarnings("unused")
    private int countRelatedResources(String relationName, Direction direction) {
        int count = 0;
        Traverser relationTraverser = getTraverser(relationName, direction);
        for (Node related : relationTraverser) {
            count++;
        }
        return count;
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The number of resources related to this resource by relationship
     *         name (outgoing)
     */
    public int countResourcesFrom(String relationName) {
        return countRelatedResources(relationName, Direction.OUTGOING);
    }

    private Traverser getChildTraverser(boolean recursive) {
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
        return getPersistentState().traverse(Traverser.Order.BREADTH_FIRST, stopEvaluator,
            ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.CONTAINS),
            Direction.OUTGOING.toNeo4jDir());
    }

    /**
     * 
     * @param recursive true if return children of children, etc
     * @return The children of this resource (associated by a
     *         {@link RelationshipTypes.CONTAINS} relationship)
     */
    public Set<Resource> getChildren(boolean recursive) {
        Set<Resource> children = new HashSet<Resource>();
        Traverser relationTraverser = getChildTraverser(recursive);
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
        Traverser relationTraverser = getChildTraverser(recursive);
        for (Node related : relationTraverser) {
            children.add((int) related.getId());
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
    public Config getConfig(String configType) {
        for (Config config : configs) {
            if (config.getType().getName().equals(configType)) {
                return config;
            }
        }
        return null;
    }

    public Set<Config> getConfigs() {
        return this.configs;
    }

    /**
     * 
     * @return A description of this Resource
     */
    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
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
     * @return The owner of the Resource
     */
    public String getOwner() {
        return owner;
    }

    /**
     * 
     * @return A Map of property keys and values, including those whose type is
     *         marked as hidden
     */
    public Map<String, Object> getProperties() {
        return getProperties(true);
    }

    /**
     * @param includeHidden true if properties whose types are marked as
     *        "hidden" should be returned
     * @return A Map of property keys and values, possibly not including those
     *         marked as "hidden"
     */
    public Map<String, Object> getProperties(boolean includeHidden) {
        Map<String, Object> properties = new HashMap<String, Object>();
        // TODO perf tradeoff of hidden properties
        for (String key : getPersistentState().getPropertyKeys()) {
            if (!(includeHidden)) {
                // PropertyType propType = type.getPropertyType(key);
                // if (propType != null && propType.isHidden()) {
                // continue;
                // }
            }
            // filter out the properties we've defined at class-level, like
            // name
            if (!(key.equals("location")) && !(key.equals("name")) &&
                !(key.equals("description")) && !(key.equals("modifiedBy")) &&
                !(key.equals("owner")) && !(key.equals("id")) && !(key.equals("privateGroup")) &&
                !(key.equals("sortName")) && !(key.equals("__type__"))) {
                // try {
                properties.put(key, getProperty(key));
                // } catch (IllegalArgumentException e) {
                // filter out the properties we've defined at class-level, like
                // name
                // }
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
        // TODO set default values when Resource created
        // PropertyType propertyType = type.getPropertyType(key);
        // if (propertyType == null) {
        // throw new IllegalArgumentException("Property " + key +
        // " is not defined for resource of type " +
        // type.getName());
        // }

        try {
            return getPersistentState().getProperty(key);
        } catch (NotFoundException e) {
            return "";
            // return propertyType.getDefaultValue();
        }
    }

    private Set<Resource> getRelatedResources(String relationName, Direction direction) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getTraverser(relationName, direction);
        for (Node related : relationTraverser) {
            Resource resource = graphDatabaseContext.createEntityFromState(related, Resource.class);
            resources.add(resource);
        }
        return resources;
    }

    private Resource getRelatedResource(String relationName, Direction direction,
                                        String propertyName, Object propertyValue) {
        Traverser relationTraverser = getTraverser(relationName, direction);
        for (Node related : relationTraverser) {
            if (related.getProperty(propertyName).equals(propertyValue)) {
                return graphDatabaseContext.createEntityFromState(related, Resource.class);
            }
        }
        return null;
    }

    private Set<Resource> getRelatedResources(String relationName, Direction direction,
                                        String propertyName, Object propertyValue) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getTraverser(relationName, direction);
        for (Node related : relationTraverser) {
            if (related.getProperty(propertyName).equals(propertyValue)) {
                resources.add(graphDatabaseContext.createEntityFromState(related, Resource.class));
            }
        }
        return resources;
    }

    /**
     * 
     * @return All the relationships this Resource is involved in (both Incoming
     *         and Outgoing)
     */
    public Set<ResourceRelationship> getRelationships() {
        return convertRelationships(null, getPersistentState().getRelationships());
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
            getPersistentState().getRelationships(DynamicRelationshipType.withName(name),
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
            getPersistentState().getRelationships(DynamicRelationshipType.withName(relationName),
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
            getPersistentState().getRelationships(DynamicRelationshipType.withName(relationName),
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
        Set<ResourceRelationship> relations = convertRelationships(resource, getPersistentState()
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

    public Resource getResourceFrom(String relationName, String propertyName, Object propertyValue) {
        return getRelatedResource(relationName, Direction.OUTGOING, propertyName, propertyValue);
    }

    public Set<Resource> getResourcesFrom(String relationName, String propertyName,
                                          Object propertyValue) {
        return getRelatedResources(relationName, Direction.OUTGOING, propertyName, propertyValue);
    }

    public Set<Integer> getResourceIdsFrom(String relationName) {
        Set<Integer> ids = new HashSet<Integer>();
        Traverser relationTraverser = getTraverser(relationName, Direction.OUTGOING);
        for (Node related : relationTraverser) {
            ids.add((int) related.getId());
        }
        return ids;
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

    public String getSortName() {
        return sortName;
    }

    private Traverser getTraverser(String relationName, Direction direction) {
        return getPersistentState().traverse(Traverser.Order.BREADTH_FIRST, new StopEvaluator() {
            public boolean isStopNode(TraversalPosition currentPos) {
                return currentPos.depth() >= 1;
            }
        }, ReturnableEvaluator.ALL_BUT_START_NODE, DynamicRelationshipType.withName(relationName),
            direction.toNeo4jDir());
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
        Traverser relationTraverser = getChildTraverser(recursive);
        for (Node related : relationTraverser) {
            if (related.equals(resource.getPersistentState())) {
                return true;
            }
        }
        return false;
    }

    public boolean isInAsyncDeleteState() {
        // TODO remove
        return false;
    }

    /**
     * 
     * @param resource The resource to test relation to
     * @param relationName The name of the relationship
     * @return true if this resource is directly related to the supplied
     *         Resource by Outgoing relationship
     */
    public boolean isRelatedTo(Resource resource, String relationName) {
        Traverser relationTraverser = getTraverser(relationName, Direction.OUTGOING);
        for (Node related : relationTraverser) {
            if (related.equals(resource.getPersistentState())) {
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
    @Transactional("neoTxManager")
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
    @Transactional("neoTxManager")
    public void remove() {
        removeConfig();
        graphDatabaseContext.removeNodeEntity(this);
    }

    private void removeConfig() {
        Iterable<org.neo4j.graphdb.Relationship> relationships = this.getPersistentState()
            .getRelationships(DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG),
                Direction.OUTGOING.toNeo4jDir());
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            graphDatabaseContext.createEntityFromState(
                relationship.getOtherNode(getPersistentState()), Config.class).remove();
        }
    }

    /**
     * Remove all properties of this Resource
     */
    @Transactional("neoTxManager")
    public void removeProperties() {
        for (String key : getPersistentState().getPropertyKeys()) {
            getPersistentState().removeProperty(key);
        }
    }

    /**
     * Remove all relationships this Resource is involved in
     */
    @Transactional("neoTxManager")
    public void removeRelationships() {
        for (org.neo4j.graphdb.Relationship relationship : getPersistentState().getRelationships()) {
            relationship.delete();
        }
    }

    /**
     * Removes specific relationships
     * @param resource The resource related to
     * @param relationName The name of the relationship
     */
    @Transactional("neoTxManager")
    public void removeRelationships(Resource resource, String relationName) {
        removeRelationships(resource, relationName, Direction.BOTH);
    }

    /**
     * Removes specific relationships
     * @param entity The resource related to
     * @param name The name of the relationship
     * @param direction The {@link Direction} of the relationship to remove
     */
    @Transactional("neoTxManager")
    public void removeRelationships(Resource entity, String name, Direction direction) {
        for (ResourceRelationship relation : getRelationships(entity, name, direction)) {
            relation.remove();
        }
    }

    /**
     * Remove specific relationships
     * @param relationName The name of the relationship
     */
    @Transactional("neoTxManager")
    public void removeRelationships(String relationName) {
        for (org.neo4j.graphdb.Relationship relationship : getPersistentState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.BOTH.toNeo4jDir())) {
            relationship.delete();
        }
    }

    /**
     * 
     * @param description The Resource description
     */
    @Transactional("neoTxManager")
    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @param location The Resource location
     */
    @Transactional("neoTxManager")
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 
     * @param modifiedBy The user that last modified the Resource
     */
    @Transactional("neoTxManager")
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * 
     * @param name The name of the Resource
     */
    @Transactional("neoTxManager")
    public void setName(String name) {
        this.name = name;
        if (this.sortName == null) {
            // Strip out all special chars b/c Lucene can't sort tokenized
            // Strings
            this.sortName = name.toUpperCase().replaceAll("\\W", "");
        }
    }

    /**
     * 
     * @param owner The owner of the Resource
     */
    @Transactional("neoTxManager")
    public void setOwner(String owner) {
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
    @Transactional("neoTxManager")
    public Object setProperty(String key, Object value) {
        if (value == null) {
            // You can't set null property values in Neo4j, so we won't know if
            // a missing property means explicit set to null or to return
            // default value
            throw new IllegalArgumentException("Null property values are not allowed");
        }
        // TODO maybe set properties and indexes up front and validate that way.
        // This call is very expensive in initial import
        // PropertyType propertyType = type.getPropertyType(key);
        // if (propertyType == null) {
        // throw new IllegalArgumentException("Property " + key +
        // " is not defined for resource of type " +
        // type.getName());
        // }
        // if (propertyType.getPropertyValidator() != null) {
        // TODO validation
        // propertyType.getPropertyValidator().validate()
        // }
        Object oldValue = null;
        try {
            oldValue = getPersistentState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getPersistentState().setProperty(key, value);
        // if (propertyType.isIndexed()) {
        if (key.equals("AppdefTypeId") || key.equals("mixed") || key.equals("groupEntResType")) {
            graphDatabaseContext.getIndex(Resource.class, null).add(getPersistentState(), key,
                value);
        }

        CPropChangeEvent event = new CPropChangeEvent(getId(), key, oldValue, value);
        messagePublisher.publishMessage(MessagePublisher.EVENTS_TOPIC, event);
        return oldValue;
    }

    @Transactional("neoTxManager")
    public void setSortName(String sortName) {
        this.sortName = sortName;
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