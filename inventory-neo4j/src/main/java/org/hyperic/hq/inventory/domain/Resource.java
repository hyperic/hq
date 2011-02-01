package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.reference.ConfigType;
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

@Entity
@NodeEntity(partial = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Resource {
    @Transient
    @ManyToOne
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

    @ManyToOne
    @Transient
    @RelatedTo(type = RelationshipTypes.OWNS, direction = Direction.INCOMING, elementClass = AuthzSubject.class)
    private AuthzSubject owner;

    @Transient
    @ManyToOne
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ResourceType.class)
    private ResourceType type;

    @Version
    @Column(name = "version")
    private Integer version;

    public Resource() {
    }
 
    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Agent getAgent() {
        return agent;
    }

    public Config getAutoInventoryConfig() {
        return getConfig(ConfigType.AUTOINVENTORY);
    }

    private Config getConfig(ConfigType type) {
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

    public Config getControlConfig() {
        return getConfig(ConfigType.CONTROL);
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return this.id;
    }

    public String getLocation() {
        return location;
    }

    public Config getMeasurementConfig() {
        return getConfig(ConfigType.MEASUREMENT);
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public String getName() {
        return name;
    }

    public AuthzSubject getOwner() {
        return owner;
    }

    public Config getProductConfig() {
        return getConfig(ConfigType.PRODUCT);
    }

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

    public Object getProperty(String key) {
        PropertyType propertyType = type.getPropertyType(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        try {
            return getUnderlyingState().getProperty(key);
        }catch(NotFoundException e) {
            return null;
        }
    }

    
    public Set<ResourceRelationship> getRelationships(Resource entity, String name, Direction direction) {
       return convertRelationships(entity, getUnderlyingState().getRelationships(
                    DynamicRelationshipType.withName(name)));
    }
    
    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    @Transactional
    public ResourceRelationship relateTo(Resource resource, String relationName) {
        if (!(RelationshipTypes.CONTAINS.equals(relationName)) && !type.isRelatedTo(resource.getType(), relationName)) {
            throw new InvalidRelationshipException();
        }
        return (ResourceRelationship) this.relateTo(resource, ResourceRelationship.class, relationName);
    }
    
    @Transactional
    public void removeRelationships(Resource entity, String name, Direction direction) {
        for (ResourceRelationship relation : getRelationships(entity, name, direction)) {
            relation.getUnderlyingState().delete();
        }
    }

    public void removeRelationship(Resource resource, String relationName) {
        if (isRelatedTo(resource, relationName)) {
            removeRelationships(resource, relationName, Direction.BOTH);
        }
    }

    public void removeRelationships() {
        for(org.neo4j.graphdb.Relationship relationship:getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
    }

    public void removeRelationships(String relationName) {
        for(org.neo4j.graphdb.Relationship relationship:getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.BOTH.toNeo4jDir())) {
            relationship.delete();
        }
    }

    public Set<ResourceRelationship> getRelationships() {
        return convertRelationships(null, getUnderlyingState().getRelationships());
    }

    public Set<ResourceRelationship> getRelationshipsFrom(String relationName) {
        return convertRelationships(null, getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.OUTGOING.toNeo4jDir()));
    }

    public Set<ResourceRelationship> getRelationshipsTo(String relationName) {
        return convertRelationships(null, getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), Direction.INCOMING.toNeo4jDir()));
    }

    public Set<Resource> getResourcesFrom(String relationName) {
        return getRelatedResources(relationName, Direction.OUTGOING);
    }
    
    public Set<Resource> getResourcesTo(String relationName) {
        return getRelatedResources(relationName, Direction.INCOMING);
    }
    
    public Set<Resource> getChildren(boolean recursive) {
        Set<Resource> children = new HashSet<Resource>();
        StopEvaluator stopEvaluator;
        if(recursive) {
            stopEvaluator = StopEvaluator.END_OF_GRAPH;
        }else {
            stopEvaluator = new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            };
        }
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            stopEvaluator, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.CONTAINS), Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            children.add(graphDatabaseContext.createEntityFromState(related,Resource.class));
        }
        return children;
    }
    
    public Set<Integer> getChildrenIds(boolean recursive) {
        Set<Integer> children = new HashSet<Integer>();
        StopEvaluator stopEvaluator;
        if(recursive) {
            stopEvaluator = StopEvaluator.END_OF_GRAPH;
        }else {
            stopEvaluator = new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            };
        }
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            stopEvaluator, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.CONTAINS), Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            children.add((Integer)related.getProperty("foreignId"));
        }
        return children;
    }
     
    public boolean hasChild(Resource resource,boolean recursive) {
        if(getChildren(recursive).contains(resource)) {
            return true;
        }
        return false;
    }
    
    public ResourceRelationship getRelationshipTo(Resource resource, String relationName) {
        Set<ResourceRelationship> relations = convertRelationships(resource, getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName)));
        ResourceRelationship result = null;
        Iterator<ResourceRelationship> i = relations.iterator();

        if (i.hasNext()) {
            result = i.next();
        }

        return result;
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

    public Resource getResourceFrom(String relationName) {
        Set<Resource> resources = getRelatedResources(relationName, Direction.OUTGOING);
        if (resources.isEmpty()) {
            return null;
        }
        // TODO enforce only one?
        return resources.iterator().next();
    }

    public Resource getResourceTo(String relationName) {
        Set<Resource> resources = getRelatedResources(relationName, Direction.INCOMING);
        if (resources.isEmpty()) {
            return null;
        }
        // TODO enforce only one?
        return resources.iterator().next();
    }

    public ResourceType getType() {
        return type;
    }

    public Integer getVersion() {
        return this.version;
    }

    public boolean isConfigUserManaged() {
        // TODO from ConfigResponseDB. remove?
        return true;
    }

    public boolean isOwner(Integer subjectId) {
        // TODO some overlord checking, then check owner's ID
        return true;
    }

    @Transactional
    public Resource merge() {
        Resource merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void remove() {
        removeConfig();
        for(org.neo4j.graphdb.Relationship relationship: getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
        getUnderlyingState().delete();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Resource attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeConfig() {
        Config measurementConfig = getMeasurementConfig();
        if(measurementConfig != null) {
            measurementConfig.remove();
        }
        Config productConfig = getProductConfig();
        if(productConfig != null) {
            productConfig.remove();
        }
        Config aiConfig = getAutoInventoryConfig();
        if(aiConfig != null) {
            aiConfig.remove();
        }
        Config controlConfig = getControlConfig();
        if(controlConfig != null) {
            controlConfig.remove();
        }
    }

    public void removeProperties() {
        for (String key : getUnderlyingState().getPropertyKeys()) {
            getUnderlyingState().removeProperty(key);
        }
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    private void setConfig(Config config, ConfigType type) {
        org.neo4j.graphdb.Relationship rel = this.getUnderlyingState().createRelationshipTo(
            config.getUnderlyingState(),
            DynamicRelationshipType.withName(RelationshipTypes.HAS_CONFIG));
        rel.setProperty("configType", type.toString());
    }

    public void setConfigUserManaged(boolean userManaged) {
        // TODO from ConfigResponseDB. remove?
    }

    public void setConfigValidationError(String error) {
        // TODO from ConfigResponseDB. remove?
    }

    public String getConfigValidationError() {
        // TODO from ConfigResponseDB. remove?
        return null;
    }

    public void setControlConfig(Config config) {
        setConfig((Config)config, ConfigType.CONTROL);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setMeasurementConfig(Config config) {
        setConfig((Config)config, ConfigType.MEASUREMENT);
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(AuthzSubject owner) {
        this.owner = owner;
    }

    public void setProductConfig(Config config) {
        setConfig((Config)config, ConfigType.PRODUCT);
    }

    @Transactional
    public Object setProperty(String key, Object value) {
        if (type.getPropertyType(key) == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        if (value == null) {
            // TODO log a warning?
            // Neo4J doesn't accept null values
            return null;
        }
        // TODO check other stuff? Should def check optional param and maybe
        // disregard nulls, below throws Exception
        // with null values
        Object oldValue = null;
        try {
            oldValue = getUnderlyingState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getUnderlyingState().setProperty(key, value);
        return oldValue;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isInAsyncDeleteState() {
        // TODO remove
        return false;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Type: ").append(getType().getName());
        return sb.toString();
    }
}