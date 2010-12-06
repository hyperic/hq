package org.hyperic.hq.inventory.domain;

import java.util.Collection;
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
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
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
    private Integer id;

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

    @RelatedTo(type = "HAS_MEMBER", direction = Direction.INCOMING, elementClass = ResourceGroup.class)
    @OneToMany
    @Transient
    private Set<ResourceGroup> resourceGroups;

    @OneToMany
    @Transient
    @RelatedTo(type = "OWNS", direction = Direction.INCOMING, elementClass = AuthzSubject.class)
    private AuthzSubject owner;

    public Resource() {
    }

    public Resource(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public Set<ResourceGroup> getResourceGroups() {
        return resourceGroups;
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

    public void removeProperties() {
        for (String key : getUnderlyingState().getPropertyKeys()) {
            getUnderlyingState().removeProperty(key);
        }
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

    public Set<ResourceRelation> getRelationshipsFrom(String relationName) {
        return getRelationships(relationName, org.neo4j.graphdb.Direction.OUTGOING);
    }

    public Set<ResourceRelation> getRelationshipsTo(String relationName) {
        return getRelationships(relationName, org.neo4j.graphdb.Direction.INCOMING);
    }

    private Set<ResourceRelation> getRelationships(String relationName,
                                                   org.neo4j.graphdb.Direction direction) {
        Set<ResourceRelation> resourceRelations = new HashSet<ResourceRelation>();
        Iterable<Relationship> relationships = getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), direction);
        for (Relationship relationship : relationships) {
            resourceRelations.add(graphDatabaseContext.createEntityFromState(relationship,
                ResourceRelation.class));
        }
        return resourceRelations;
    }

    public Set<Resource> getResourcesFrom(String relationName) {
        return getRelatedResources(relationName, org.neo4j.graphdb.Direction.OUTGOING);
    }

    public Set<Resource> getResourcesTo(String relationName) {
        return getRelatedResources(relationName, org.neo4j.graphdb.Direction.INCOMING);
    }

    private Set<Resource> getRelatedResources(String relationName,
                                              org.neo4j.graphdb.Direction direction) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), direction);
        for (Node related : relationTraverser) {
            resources.add(graphDatabaseContext.createEntityFromState(related, Resource.class));
        }
        return resources;
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

    public void removeRelationships() {
        // TODO getRelationships only does one direction
        for (ResourceRelation relation : getRelationships()) {
            relation.getUnderlyingState().delete();
        }
    }

    public void removeRelationships(String relationName) {
        Iterable<Relationship> relationships = getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName(relationName), org.neo4j.graphdb.Direction.BOTH);
        for (Relationship relationship : relationships) {
            relationship.delete();
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object setProperty(String key, Object value) {
        if (type.getPropertyType(key) == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        // TODO check other stuff?
        Object oldValue = getUnderlyingState().getProperty(key);
        getUnderlyingState().setProperty(key, value);
        return oldValue;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AuthzSubject getOwner() {
        return owner;
    }

    public void setOwner(AuthzSubject owner) {
        this.owner = owner;
    }

    public Config getMeasurementConfig() {
        return getConfig("Measurement");
    }

    public Config getControlConfig() {
        return getConfig("Control");
    }

    public Config getProductConfig() {
        return getConfig("Product");
    }

    public Config getAutoInventoryConfig() {
        return getConfig("AutoInventory");
    }

    public Config getResponseTimeConfig() {
        return getConfig("ResponseTime");
    }

    private Config getConfig(String type) {
        Iterable<Relationship> relationships = this.getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName("HAS_CONFIG"), org.neo4j.graphdb.Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (type.equals(relationship.getProperty("configType"))) {
                // TODO enforce no more than one?
                return graphDatabaseContext.createEntityFromState(
                    relationship.getOtherNode(getUnderlyingState()), Config.class);
            }
        }
        return null;
    }

    public void setMeasurementConfig(Config config) {
        setConfig(config, "Measurement");
    }

    public void setProductConfig(Config config) {
        setConfig(config, "Product");
    }

    public void setControlConfig(Config config) {
        setConfig(config, "Control");
    }

    public void setResponseTimeConfig(Config config) {
        setConfig(config, "ResponseTime");
    }

    private void setConfig(Config config, String type) {
        Relationship rel = this.getUnderlyingState().createRelationshipTo(
            config.getUnderlyingState(), DynamicRelationshipType.withName("HAS_CONFIG"));
        rel.setProperty("configType", type);
    }

    public static int countResources() {
        return entityManager().createQuery("select count(o) from Resource o", Integer.class)
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

    public static Resource findResource(Integer id) {
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

    public static Resource findResourcePrototypeByName(String name) {
        // TODO remove
        return null;
    }

    public static Resource findRootResource() {
        // TODO AuthzConstants.RootResourceId. We may need a root resource.
        // Check concept of Neo4J ref node
        return null;
    }

    public boolean isOwner(Integer subjectId) {
        // TODO some overlord checking, then check owner's ID
        return true;
    }

    public static List<Resource> findResourcesOfPrototype(Resource proto, PageInfo pInfo) {
        // TODO get rid of this
        return null;
    }

    public static List<Resource> findAppdefPrototypes() {
        // TODO get rid of this
        return null;
    }

    public static List<Resource> findAllAppdefPrototypes() {
        // TODO get rid of this
        return null;
    }

    public boolean isInAsyncDeleteState() {
        // TODO get rid of this
        return false;
    }

    public static Collection<Resource> findByOwner(AuthzSubject owner) {
        // TODO best way to implement cutting across to AuthzSubject
        return null;
    }

    public Resource getPrototype() {
        // TODO remove
        return null;
    }

    public void setPrototype(Resource resource) {
        // TODO remove
    }

    public Integer getInstanceId() {
        // TODO remove this
        return id;
    }

    public void setInstanceId(Integer instanceId) {
        // TODO remove this
    }

    public static Resource findByInstanceId(Integer typeId, Integer instanceId) {
        // TODO remove this
        return Resource.findResource(instanceId);
    }

    public String getSortName() {
        // TODO remove
        return null;
    }

    public void setSortName(String sortName) {
        // TODO remove
    }

    public static Collection findSvcRes_orderName(Boolean fSystem) {
        // TODO remove
        return null;
    }

    public void setConfigValidationError(String error) {
        // TODO from ConfigResponseDB. remove?
    }

    public void setConfigUserManaged(boolean userManaged) {
        // TODO from ConfigResponseDB. remove?
    }

    public boolean isConfigUserManaged() {
        // TODO from ConfigResponseDB. remove?
        return true;
    }

}
