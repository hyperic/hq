package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.plugin.domain.Plugin;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Metadata for Resources that can be created
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Entity
@Configurable
@NodeEntity(partial = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceType {

    @RelatedTo(type = RelationshipTypes.HAS_CONFIG_TYPE, direction = Direction.OUTGOING, elementClass = ConfigType.class)
    @Transient
    private Set<ConfigType> configTypes;

    @GraphProperty
    @Transient
    private String description;

    @PersistenceContext
    private transient EntityManager entityManager;

    @Autowired
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

    @RelatedTo(type = RelationshipTypes.HAS_OPERATION_TYPE, direction = Direction.OUTGOING, elementClass = OperationType.class)
    @Transient
    private Set<OperationType> operationTypes;

    @Transient
    @RelatedTo(type = RelationshipTypes.DEFINED_BY, direction = Direction.OUTGOING, elementClass = Plugin.class)
    private Plugin plugin;

    @RelatedTo(type = RelationshipTypes.HAS_PROPERTY_TYPE, direction = Direction.OUTGOING, elementClass = PropertyType.class)
    @Transient
    private Set<PropertyType> propertyTypes;

    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.INCOMING, elementClass = Resource.class)
    @Transient
    private Set<Resource> resources;

    public ResourceType() {
    }

    /**
     * 
     * @param name The name of this ResourceType
     */
    public ResourceType(String name) {
        this.name = name;
    }

    /**
     * 
     * @param name The name of this ResourceType
     * @param description The description of this ResourceType
     */
    public ResourceType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 
     * @param configType The ConfigType to add
     */
    public void addConfigType(ConfigType configType) {
        entityManager.persist(configType);
        configType.getId();
        relateTo(configType, DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG_TYPE));
    }

    /**
     * 
     * @param operationType The OperationType to add
     */
    public void addOperationType(OperationType operationType) {
        entityManager.persist(operationType);
        operationType.getId();
        relateTo(operationType,
            DynamicRelationshipType.withName(RelationshipTypes.HAS_OPERATION_TYPE));
    }

    /**
     * 
     * @param propertyType The PropertyType to add
     */
    public void addPropertyType(PropertyType propertyType) {
        entityManager.persist(propertyType);
        propertyType.getId();
        relateTo(propertyType,
            DynamicRelationshipType.withName(RelationshipTypes.HAS_PROPERTY_TYPE));
    }

    private Set<ResourceTypeRelationship> convertRelationships(ResourceType entity,
                                                               Iterable<org.neo4j.graphdb.Relationship> relationships) {
        Set<ResourceTypeRelationship> relations = new HashSet<ResourceTypeRelationship>();
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Node node = relationship.getOtherNode(getUnderlyingState());
                Class<?> otherEndType = graphDatabaseContext.getJavaType(node);
                if (ResourceType.class.isAssignableFrom(otherEndType)) {
                    if (entity == null || node.equals(entity.getUnderlyingState())) {
                        relations.add(graphDatabaseContext.createEntityFromState(relationship,
                            ResourceTypeRelationship.class));
                    }
                }
            }
        }
        return relations;
    }

    /**
     * 
     * @param name The name of the ConfigType
     * @return The ConfigType or null if it doesn't exist
     */
    public ConfigType getConfigType(String name) {
        for (ConfigType configType : configTypes) {
            if (name.equals(configType.getName())) {
                return configType;
            }
        }
        return null;
    }

    /**
     * 
     * @return The ConfigTypes for this ResourceType
     */
    public Set<ConfigType> getConfigTypes() {
        return configTypes;
    }

    /**
     * 
     * @return The description of the ResourceType
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @return The ID of the ResourceType
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 
     * @return The name of the ResourceType
     */
    public String getName() {
        return name;
    }
    
    /**
     * 
     * @param name The name of the OperationType
     * @return The OperationType or null if it doesn't exist
     */
    public OperationType getOperationType(String name) {
        for (OperationType operationType : operationTypes) {
            if (name.equals(operationType.getName())) {
                return operationType;
            }
        }
        return null;
    }

    /**
     * 
     * @return The OperationTypes
     */
    public Set<OperationType> getOperationTypes() {
        return operationTypes;
    }

    /**
     * 
     * @return The Plugin that defined this ResourceType
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * 
     * @param name The name of the PropertyType
     * @return The PropertyType or null if none exists
     */
    public PropertyType getPropertyType(String name) {
        for (PropertyType propertyType : propertyTypes) {
            if (name.equals(propertyType.getName())) {
                return propertyType;
            }
        }
        return null;
    }

    /**
     * 
     * @return The PropertyTypes, including those marked as hidden
     */
    public Set<PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    /**
     * 
     * @param includeHidden true to include PropertyTypes marked as hidden
     * @return The PropertyTypes, possibly excluding those that are hidden
     */
    public Set<PropertyType> getPropertyTypes(boolean includeHidden) {
        if (!(includeHidden)) {
            Set<PropertyType> propTypes = new HashSet<PropertyType>();
            for (PropertyType propType : propertyTypes) {
                if (!(propType.isHidden())) {
                    propTypes.add(propType);
                }
            }
            return propTypes;
        }
        return getPropertyTypes();
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

    /**
     * 
     * @return All relationships this ResourceType is involved in
     */
    public Set<ResourceTypeRelationship> getRelationships() {
        return convertRelationships(null, getUnderlyingState().getRelationships());
    }

    /**
     * 
     * @param entity The possibly related entity
     * @param name The relationship name
     * @param direction The direction of the relationship
     * @return A single relationship, 2 relationships if the Direction is BOTH,
     *         or null if no relationship exists
     */
    public Set<ResourceTypeRelationship> getRelationships(ResourceType entity, String name,
                                                          Direction direction) {
        return convertRelationships(
            entity,
            getUnderlyingState().getRelationships(DynamicRelationshipType.withName(name),
                direction.toNeo4jDir()));
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The relationships of specified name OUTGOING from this
     *         ResourceTYpe
     */
    public Set<ResourceTypeRelationship> getRelationshipsFrom(String relationName) {
        return getRelationships(null, relationName, Direction.OUTGOING);
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The relationships of specified name INCOMING to this ResourceTYpe
     */
    public Set<ResourceTypeRelationship> getRelationshipsTo(String relationName) {
        return getRelationships(null, relationName, Direction.INCOMING);
    }

    /**
     * 
     * @return All Resources of this type
     */
    public Set<Resource> getResources() {
        return resources;
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The ResourceTypes related by specified relationship OUTGOING from
     *         this ResourceTYpe
     */
    public Set<ResourceType> getResourceTypesFrom(String relationName) {
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.OUTGOING);
    }

    /**
     * 
     * @param relationName The relationship name
     * @return The ResourceTypes related by specified relationship INCOMING from
     *         this ResourceTYpe
     */
    public Set<ResourceType> getResourceTypesTo(String relationName) {
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.INCOMING);
    }

    /**
     * 
     * @return true if resources exist of this type
     */
    public boolean hasResources() {
        return resources.size() > 0;
    }

    /**
     * 
     * @param entity The ResourceType to test relation to
     * @param relationName The name of the relationship
     * @return true if this resource type is directly related to the supplied
     *         ResourceType by Outgoing relationship
     */
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

    /**
     * 
     * @param entity The entity to relate to
     * @param relationName The name of the relationship
     * @return The created relationship
     */
    @Transactional
    public ResourceTypeRelationship relateTo(ResourceType entity, String relationName) {
        return (ResourceTypeRelationship) this.relateTo(entity, ResourceTypeRelationship.class,
            relationName);
    }

    /**
     * Removes this ResourceType, including all Resources of this type and all
     * relationships
     */
    @Transactional
    public void remove() {
        removeResources();
        removePropertyTypes();
        removeOperationTypes();
        removeConfigTypes();
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ResourceType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeConfigTypes() {
        for (ConfigType configType : configTypes) {
            configType.remove();
        }
    }

    private void removeOperationTypes() {
        for (OperationType operationType : operationTypes) {
            operationType.remove();
        }
    }

    private void removePropertyTypes() {
        for (PropertyType propertyType : propertyTypes) {
            propertyType.remove();
        }
    }

    /**
     * Removes all relationships
     */
    @Transactional
    public void removeRelationships() {
        for (org.neo4j.graphdb.Relationship relationship : getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
    }

    /**
     * Removes relationships
     * @param entity The related ResourceType
     * @param relationName The name of the relationship
     */
    @Transactional
    public void removeRelationships(ResourceType entity, String relationName) {
        removeRelationships(entity, relationName, Direction.BOTH);
    }

    /**
     * Removes relationships
     * @param entity The related ResourceType
     * @param name The name of the relationship
     * @param direction The Direction of the relationship
     */
    @Transactional
    public void removeRelationships(ResourceType entity, String name, Direction direction) {
        for (ResourceTypeRelationship relation : getRelationships(entity, name, direction)) {
            relation.getUnderlyingState().delete();
        }
    }

    /**
     * Removes relationships
     * @param relationName The name of the relationship
     */
    @Transactional
    public void removeRelationships(String relationName) {
        for (org.neo4j.graphdb.Relationship relationship : getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.BOTH.toNeo4jDir())) {
            relationship.delete();
        }
    }

    private void removeResources() {
        for (Resource resource : resources) {
            resource.remove();
        }
    }

    /**
     * 
     * @param description The ResourceType decscription
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @param id The ID (Hibernate internal)
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @param plugin The plugin that defines this ResourceType
     */
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceType[ ");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ").append("]");
        return sb.toString();
    }
}
