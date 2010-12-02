package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;


import org.hyperic.hq.inventory.InvalidRelationshipException;
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
import org.springframework.transaction.annotation.Transactional;

@Entity
@NodeEntity(partial = true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Resource {


    @PersistenceContext
    transient EntityManager entityManager;

    @javax.annotation.Resource
    transient FinderFactory finderFactory;

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    private Long id;

    // TODO do I need Indexed and GraphProperty?
    @NotNull
    @Indexed
    @GraphProperty
    @Transient
    private String name;

    @Transient
    @ManyToOne
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ResourceType.class)
    private ResourceType type;

    @Version
    @Column(name = "version")
    private Integer version;

    public Resource() {
    }

    public Resource(Node n) {
        setUnderlyingState(n);
    }

    public long count() {
        return finderFactory.getFinderForClass(Resource.class).count();

    }

    public Resource findById(Long id) {
        return finderFactory.getFinderForClass(Resource.class).findById(id);

    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.flush();
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return name;
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
        return getUnderlyingState().getProperty(key, propertyType.getDefaultValue());
    }

    public Set<ResourceRelation> getRelationships() {
        // TODO This is hardcoded for the demo, however should be able to
        // specify direction/relationship name via parameters
        Iterable<Relationship> relationships = getUnderlyingState().getRelationships(
            org.neo4j.graphdb.Direction.OUTGOING);
        Set<ResourceRelation> resourceRelations = new HashSet<ResourceRelation>();
        for (Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Class<?> otherEndType = graphDatabaseContext.getJavaType(relationship
                    .getOtherNode(getUnderlyingState()));
                if (Resource.class.isAssignableFrom(otherEndType)) {
                    resourceRelations.add(graphDatabaseContext.createEntityFromState(relationship,
                        ResourceRelation.class));
                }
            }
        }
        return resourceRelations;
    }

    public ResourceRelation getRelationshipTo(Resource resource, String relationName) {
        // TODO this doesn't take direction into account
        return (ResourceRelation) getRelationshipTo(resource, ResourceRelation.class, relationName);
    }

    public ResourceType getType() {
        return type;
    }

    public Integer getVersion() {
        return this.version;
    }

    public boolean isRelatedTo(Resource resource, String relationName) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {

               
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), org.neo4j.graphdb.Direction.OUTGOING);
        for (Node related : relationTraverser) {
            if (related.equals(resource.getUnderlyingState())) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public Resource merge() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        Resource merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.persist(this);
        // TODO this call appears to be necessary to get Alert populated with
        // its underlying node
        getId();
    }

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
    public void remove() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Resource attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    @Transactional
    public void removeRelationship(Resource resource, String relationName) {
        if (this.isRelatedTo(resource, relationName)) {
            this.removeRelationshipTo(resource, relationName);
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProperty(String key, Object value) {
        if (type.getPropertyType(key) == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        // TODO check other stuff?
        getUnderlyingState().setProperty(key, value);
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
    
    //TODO other config types and setters
    public Set<Config> getMeasurementConfig() {
        Set<Config> config = new HashSet<Config>();
        Iterable<Relationship> relationships = this.getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName("HAS_CONFIG"), org.neo4j.graphdb.Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if ("Measurement".equals(relationship.getProperty("configType"))) {
                config.add(graphDatabaseContext.createEntityFromState(
                    relationship.getOtherNode(getUnderlyingState()), Config.class));
            }
        }
        return config;
    }

    public static long countResources() {
        return entityManager().createQuery("select count(o) from Resource o", Long.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new Resource().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<Resource> findAllResources() {
        return entityManager().createQuery("select o from Resource o", Resource.class)
            .getResultList();
    }

    public static Resource findResource(Long id) {
        if (id == null)
            return null;
        return entityManager().find(Resource.class, id);
    }

    public static Resource findResourceByName(String name) {
        return new Resource().finderFactory.getFinderForClass(Resource.class).findByPropertyValue(
            "name", name);
    }

    public static List<Resource> findResourceEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from Resource o", Resource.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

}
