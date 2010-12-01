package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
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
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.datastore.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@NodeEntity(partial = true)
public class ResourceType {

    @NotNull
    @Indexed
    @GraphProperty
    @Transient
    private String name;

//    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.INCOMING, elementClass = Resource.class)
//    @OneToMany
//    @Transient
//    private Set<Resource> resources;

    @RelatedTo(type = "HAS_PROPERTIES", direction = Direction.OUTGOING, elementClass = PropertyType.class)
    @OneToMany
    @Transient
    private Set<PropertyType> propertyTypes;

    @RelatedTo(type = "HAS_OPERATIONS", direction = Direction.OUTGOING, elementClass = OperationType.class)
    @OneToMany
    @Transient
    private Set<OperationType> operationTypes;

    @javax.annotation.Resource
    transient FinderFactory finderFactory;

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "version")
    private Integer version;

    public ResourceType() {
    }

    public ResourceType(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public ResourceTypeRelation relateTo(ResourceType resourceType, String relationName) {
        return (ResourceTypeRelation) this.relateTo(resourceType, ResourceTypeRelation.class,
            relationName);
    }

    @Transactional
    public void removeRelationship(ResourceType resourceType, String relationName) {
        if (this.isRelatedTo(resourceType, relationName)) {
            this.getRelationshipTo(resourceType, relationName);
        }
    }

    public ResourceTypeRelation getRelationshipTo(ResourceType resourceType, String relationName) {
        return (ResourceTypeRelation) this.getRelationshipTo(resourceType, relationName);
    }

    public Set<ResourceTypeRelation> getRelationships() {
        Iterable<Relationship> relationships = this.getUnderlyingState().getRelationships(
            org.neo4j.graphdb.Direction.OUTGOING);
        Set<ResourceTypeRelation> resourceTypeRelations = new HashSet<ResourceTypeRelation>();

        for (Relationship relationship : relationships) {
            if (!relationship.isType(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Class<?> otherEndType = graphDatabaseContext.getJavaType(relationship
                    .getOtherNode(this.getUnderlyingState()));

                if (ResourceType.class.equals(otherEndType)) {
                    resourceTypeRelations.add(graphDatabaseContext.createEntityFromState(
                        relationship, ResourceTypeRelation.class));
                }
            }
        }

        return resourceTypeRelations;
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
            if (related.equals(resourceType.getUnderlyingState())) {
                return true;
            }
        }
        return false;
    }

    public static ResourceType findResourceTypeByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        ResourceType type = new ResourceType().finderFactory.getFinderForClass(ResourceType.class)
            .findByPropertyValue("name", name);
        if (type != null) {
            type.getId();
        }
        return type;
    }

    public PropertyType getPropertyType(String name) {
        for (Object propertyType : propertyTypes) {
            if (PropertyType.class.isInstance(propertyType)) {
                PropertyType pt = (PropertyType) propertyType;
                if (name.equals(pt.getName())) {
                    return pt;
                }
            }
        }
        return null;
    }

    public Set<PropertyType> getPropertyTypes() {
        Set<PropertyType> result = new HashSet<PropertyType>();
        // TODO is there something other than PropertyTypes in the PropertyType
        // set? Shouldn't be the case. Another place we can't use same
        // relationship name?
        for (Object propertyType : this.propertyTypes) {
            if (PropertyType.class.isInstance(propertyType)) {
                result.add((PropertyType) propertyType);
            }
        }

        return result;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.persist(this);
        getId();
    }

    public static long countResourceTypes() {
        return entityManager().createQuery("select count(o) from ResourceType o", Long.class)
            .getSingleResult();
    }

    public static List<ResourceType> findAllResourceTypes() {
        return entityManager().createQuery("select o from ResourceType o", ResourceType.class)
            .getResultList();
    }

    public static ResourceType findResourceType(Long id) {
        if (id == null)
            return null;
        return entityManager().find(ResourceType.class, id);
    }

    public static List<ResourceType> findResourceTypeEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from ResourceType o", ResourceType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.flush();
    }

    @Transactional
    public ResourceType merge() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        ResourceType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

    @Transactional
    public void remove() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ResourceType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public static final EntityManager entityManager() {
        EntityManager em = new ResourceType().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getId() {
        return this.id;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ResourceType findById(Long id) {
        return finderFactory.getFinderForClass(ResourceType.class).findById(id);

    }

    public long count() {
        return finderFactory.getFinderForClass(ResourceType.class).count();

    }

}
