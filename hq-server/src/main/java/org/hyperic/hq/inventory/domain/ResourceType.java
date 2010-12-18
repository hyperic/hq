package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.reference.ConfigType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.annotation.Indexed;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.datastore.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@NodeEntity(partial = true)
public class ResourceType {

    @PersistenceContext
    transient EntityManager entityManager;

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Indexed
    @GraphProperty
    @Transient
    private String name;

    @GraphProperty
    @Transient
    private String description;

    @RelatedTo(type = RelationshipTypes.HAS_OPERATION_TYPE, direction = Direction.OUTGOING, elementClass = OperationType.class)
    @OneToMany(cascade = CascadeType.ALL)
    @Transient
    private Set<OperationType> operationTypes;

    @RelatedTo(type = RelationshipTypes.HAS_PROPERTY_TYPE, direction = Direction.OUTGOING, elementClass = PropertyType.class)
    @OneToMany(cascade = CascadeType.ALL)
    @Transient
    private Set<PropertyType> propertyTypes;

    @RelatedTo(type = RelationshipTypes.HAS_CONFIG_OPT_TYPE, direction = Direction.OUTGOING, elementClass = ConfigOptionType.class)
    @OneToMany(cascade = CascadeType.ALL)
    @Transient
    private Set<ConfigOptionType> configTypes;

    @Transient
    @ManyToOne
    @RelatedTo(type = RelationshipTypes.DEFINED_BY, direction = Direction.OUTGOING, elementClass = Plugin.class)
    private Plugin plugin;

    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.INCOMING, elementClass = Resource.class)
    @OneToMany(cascade = CascadeType.REMOVE)
    @Transient
    private Set<Resource> resources;

    @Version
    @Column(name = "version")
    private Integer version;

    public ResourceType() {
    }

    public ResourceType(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public PropertyType getPropertyType(String name) {
        for (PropertyType propertyType : propertyTypes) {
            if (name.equals(propertyType.getName())) {
                return propertyType;
            }
        }
        return null;
    }

    public OperationType getOperationType(String name) {
        for (OperationType operationType : operationTypes) {
            if (name.equals(operationType.getName())) {
                return operationType;
            }
        }
        return null;
    }

    public Set<PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public Set<OperationType> getOperationTypes() {
        return operationTypes;
    }

    @SuppressWarnings("unchecked")
    public Set<Relationship<ResourceType>> getRelationships(ResourceType entity, String name,
                                                            Direction direction) {
        Set<Relationship<ResourceType>> relations = new HashSet<Relationship<ResourceType>>();
        Iterable<org.neo4j.graphdb.Relationship> relationships;
        
        if (name != null) {
            if (direction != null) {
                relationships = getUnderlyingState().getRelationships(
                    DynamicRelationshipType.withName(name), direction.toNeo4jDir());
            } else {
                relationships = getUnderlyingState().getRelationships(
                    DynamicRelationshipType.withName(name));
            }
        } else {
            if (direction != null) {
                relationships = getUnderlyingState().getRelationships(direction.toNeo4jDir());
            } else {
                relationships = getUnderlyingState().getRelationships();
            }
        }

        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Node node = relationship.getOtherNode(getUnderlyingState());
                Class<?> otherEndType = graphDatabaseContext.getJavaType(node);

                if (Resource.class.isAssignableFrom(otherEndType)) {
                    if (entity == null || node.equals(entity.getUnderlyingState())) {
                        relations.add(graphDatabaseContext.createEntityFromState(relationship,
                            Relationship.class));
                    }
                }
            }
        }

        return relations;
    }

    public boolean isRelatedTo(ResourceType entity, String name) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {

                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE, DynamicRelationshipType.withName(name),
            org.neo4j.graphdb.Direction.OUTGOING);
        for (Node related : relationTraverser) {
            if (related.equals(entity.getUnderlyingState())) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Relationship<ResourceType> relateTo(ResourceType entity, String relationName) {
        return (Relationship<ResourceType>) this.relateTo(entity, Relationship.class, relationName);
    }

    @Transactional
    public void removeRelationships(ResourceType entity, String name,
                                    Direction direction) {
        // TODO getRelationships only does one direction
        for (Relationship<ResourceType> relation : getRelationships(entity, name, direction)) {
            relation.getUnderlyingState().delete();
        }
    }

    public void removeRelationship(ResourceType entity, String relationName) {
        if (isRelatedTo(entity, relationName)) {
            removeRelationships(entity, relationName, Direction.BOTH);
        }
    }

    public void removeRelationships() {
        removeRelationships(null, null, Direction.BOTH);
    }

    public void removeRelationships(String relationName) {
        removeRelationships(null, relationName, Direction.BOTH);
    }

    public Set<Relationship<ResourceType>> getRelationships() {
        return getRelationships(null, null, Direction.BOTH);
    }

    public Set<Relationship<ResourceType>> getRelationshipsFrom(String relationName) {
        return getRelationships(null, relationName, Direction.OUTGOING);
    }

    public Set<Relationship<ResourceType>> getRelationshipsTo(String relationName) {
        return getRelationships(null, relationName, Direction.INCOMING);
    }

    public Relationship<ResourceType> getRelationshipTo(ResourceType entity, String relationName) {
        Set<Relationship<ResourceType>> relations = getRelationships(entity, relationName, null);
        Relationship<ResourceType> result = null;
        Iterator<Relationship<ResourceType>> i = relations.iterator();

        if (i.hasNext()) {
            result = i.next();
        }

        return result;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public ResourceType merge() {
        ResourceType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void persist() {
        this.entityManager.persist(this);
        getId();
    }

    @Transactional
    public void remove() {
        removeResources();
        removePropertyTypes();
        removeOperationTypes();
        removeConfigTypes();
        for(org.neo4j.graphdb.Relationship relationship: getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
        getUnderlyingState().delete();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ResourceType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeResources() {
        for (Resource resource : resources) {
            resource.remove();
        }
    }

    private void removePropertyTypes() {
        for (PropertyType propertyType : propertyTypes) {
            propertyType.remove();
        }
    }

    private void removeOperationTypes() {
        for (OperationType operationType : operationTypes) {
            operationType.remove();
        }
    }

    private void removeConfigTypes() {
        for (ConfigOptionType configType : configTypes) {
            configType.remove();
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasResources() {
        return resources.size() > 0;
    }

    public Set<ResourceType> getResourceTypesFrom(String relationName) {
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.OUTGOING);
    }

    public Set<ResourceType> getResourceTypesTo(String relationName) {
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.INCOMING);
    }

    public ResourceType getResourceTypeFrom(String relationName) {
        Set<ResourceType> resourceTypes = getRelatedResourceTypes(relationName,
            org.neo4j.graphdb.Direction.OUTGOING);
        if (resourceTypes.isEmpty()) {
            return null;
        }
        // TODO validate only one
        return resourceTypes.iterator().next();
    }

    public ResourceType getResourceTypeTo(String relationName) {
        Set<ResourceType> resourceTypes = getRelatedResourceTypes(relationName,
            org.neo4j.graphdb.Direction.INCOMING);
        if (resourceTypes.isEmpty()) {
            return null;
        }
        // TODO validate only one
        return resourceTypes.iterator().next();
    }

    private Set<ResourceType> getRelatedResourceTypes(String relationName,
                                                      org.neo4j.graphdb.Direction direction) {
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), direction);
        for (Node related : relationTraverser) {
            ResourceType type = graphDatabaseContext.createEntityFromState(related,
                ResourceType.class);
            type.getId();
            resourceTypes.add(type);
        }
        return resourceTypes;
    }

    public void setPropertyTypes(Set<PropertyType> propertyTypes) {
        this.propertyTypes = propertyTypes;
    }

    // TODO other config types and setters
    public Set<ConfigOptionType> getMeasurementConfigTypes() {
        Set<ConfigOptionType> configTypes = new HashSet<ConfigOptionType>();
        Iterable<org.neo4j.graphdb.Relationship> relationships = this.getUnderlyingState()
            .getRelationships(DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG_OPT_TYPE),
                org.neo4j.graphdb.Direction.OUTGOING);
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            if (ConfigType.MEASUREMENT.toString().equals(relationship.getProperty("configType"))) {
                configTypes.add(graphDatabaseContext.createEntityFromState(
                    relationship.getOtherNode(getUnderlyingState()), ConfigOptionType.class));
            }
        }
        return configTypes;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}