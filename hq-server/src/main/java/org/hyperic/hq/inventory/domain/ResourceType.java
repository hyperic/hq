package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.reference.RelationshipDirection;
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
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.datastore.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@NodeEntity(partial = true)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"underlyingState", "stateAccessors"})
public class ResourceType implements IdentityAware, RelationshipAware<ResourceType> {

    @PersistenceContext
    transient EntityManager entityManager;

    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.INCOMING, elementClass = Resource.class)
    @OneToMany
    @Transient
    private Set<Resource> resources;

    @javax.annotation.Resource
    transient FinderFactory finderFactory;

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

    @RelatedTo(type = "HAS_OPERATIONS", direction = Direction.OUTGOING, elementClass = OperationType.class)
    @OneToMany
    @Transient
    private Set<OperationType> operationTypes;

    @RelatedTo(type = "HAS_PROPERTIES", direction = Direction.OUTGOING, elementClass = PropertyType.class)
    @OneToMany
    @Transient
    private Set<PropertyType> propertyTypes;

    @RelatedTo(type = "HAS_CONFIG_TYPE", direction = Direction.OUTGOING, elementClass = ConfigType.class)
    @OneToMany
    @Transient
    private Set<ConfigType> configTypes;

    @Transient
    @ManyToOne
    @RelatedTo(type = "DEFINED_BY", direction = Direction.OUTGOING, elementClass = Plugin.class)
    private Plugin plugin;

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
    public Set<Relationship<ResourceType>> getRelationships(ResourceType entity, String name, RelationshipDirection direction) {
    	Set<Relationship<ResourceType>> relations = new HashSet<Relationship<ResourceType>>();
    	Iterable<org.neo4j.graphdb.Relationship> relationships;
    	org.neo4j.graphdb.Direction neo4jDirection = null;
    	
    	switch (direction) {
    		case BOTH_WAYS:
    			neo4jDirection = org.neo4j.graphdb.Direction.BOTH;
    			break;
    		case INCOMING:
    			neo4jDirection = org.neo4j.graphdb.Direction.INCOMING;
    			break;
    		case OUTGOING:
    			neo4jDirection = org.neo4j.graphdb.Direction.OUTGOING;
    			break;
    	}

    	if (name != null) {
    		if (neo4jDirection != null) {
    			relationships = getUnderlyingState().getRelationships(DynamicRelationshipType.withName(name), neo4jDirection);
    		} else {
    			relationships = getUnderlyingState().getRelationships(DynamicRelationshipType.withName(name));
    		}
    	} else {
    		if (neo4jDirection != null) {
    			relationships = getUnderlyingState().getRelationships(neo4jDirection);
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
            			relations.add(graphDatabaseContext.createEntityFromState(relationship, Relationship.class));
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
	            }, 
	            ReturnableEvaluator.ALL_BUT_START_NODE,
	            DynamicRelationshipType.withName(name), 
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
    public void removeRelationships(ResourceType entity, String name, RelationshipDirection direction) {
        // TODO getRelationships only does one direction
        for (Relationship<ResourceType> relation : getRelationships(entity, name, direction)) {
            relation.getUnderlyingState().delete();
        }
	}

    public void removeRelationship(ResourceType entity, String relationName) {
        if (isRelatedTo(entity, relationName)) {
        	removeRelationships(entity, relationName, RelationshipDirection.ALL);
        }
    }

    public void removeRelationships() {
    	removeRelationships(null, null, RelationshipDirection.ALL);
    }

    public void removeRelationships(String relationName) {
    	removeRelationships(null, relationName, RelationshipDirection.ALL);
    }

	public Set<Relationship<ResourceType>> getRelationships() {
        return getRelationships(null, null, RelationshipDirection.ALL);
    }

    public Set<Relationship<ResourceType>> getRelationshipsFrom(String relationName) {
        return getRelationships(null, relationName, RelationshipDirection.OUTGOING);
    }

    public Set<Relationship<ResourceType>> getRelationshipsTo(String relationName) {
        return getRelationships(null, relationName, RelationshipDirection.INCOMING);
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
        if (this.entityManager == null)
            this.entityManager = entityManager();
        ResourceType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.persist(this);
        getId();
    }

    @Transactional
    public void remove() {
        removeResources();
        removePropertyTypes();
        removeOperationTypes();
        removeConfigTypes();
        if (this.entityManager == null)
            this.entityManager = entityManager();
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
        for (ConfigType configType : configTypes) {
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
        // TODO validate only one
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.OUTGOING)
            .iterator().next();
    }

    public ResourceType getResourceTypeTo(String relationName) {
        // TODO validate only one
        return getRelatedResourceTypes(relationName, org.neo4j.graphdb.Direction.INCOMING)
            .iterator().next();
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

    public static int countResourceTypes() {
        return entityManager().createQuery("select count(o) from ResourceType o", Integer.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new ResourceType().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<ResourceType> findAllResourceTypes() {
        List<ResourceType> types = entityManager().createQuery("select o from ResourceType o", ResourceType.class)
            .getResultList();
        for(ResourceType type: types) {
            type.getId();
        }
        return types;
    }

    public static ResourceType findResourceType(Integer id) {
    	return findById(id);
    }
    
    public static ResourceType findById(Integer id) {
        if (id == null)
            return null;
        ResourceType resourceType = entityManager().find(ResourceType.class, id);
        if(resourceType !=null) {
            resourceType.getId();
        }
        return resourceType;
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

    public static List<ResourceType> find(Integer firstResult, Integer maxResults) {
        List<ResourceType> types = entityManager().createQuery("select o from ResourceType o", ResourceType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
        for(ResourceType type: types) {
            type.getId();
        }
        return types;
    }
    
    public void setPropertyTypes(Set<PropertyType> propertyTypes) {
        this.propertyTypes = propertyTypes;
    }

    // TODO other config types and setters
    public Set<ConfigType> getMeasurementConfigTypes() {
        Set<ConfigType> configTypes = new HashSet<ConfigType>();
        Iterable<org.neo4j.graphdb.Relationship> relationships = this.getUnderlyingState().getRelationships(
            DynamicRelationshipType.withName("HAS_CONFIG_TYPE"),
            org.neo4j.graphdb.Direction.OUTGOING);
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            if ("Measurement".equals(relationship.getProperty("configType"))) {
                configTypes.add(graphDatabaseContext.createEntityFromState(
                    relationship.getOtherNode(getUnderlyingState()), ConfigType.class));
            }
        }
        return configTypes;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public static ResourceType findRootResourceType() {
    	// TODO Should this be hardcoded?
    	return findById(1);
    }

    public static Set<ResourceType> findByPlugin(String plugin) {
        Set<ResourceType> pluginTypes = new HashSet<ResourceType>();
        //TODO can we do a JPA-style query that is quicker here?
        List<ResourceType> types = findAllResourceTypes();
        for(ResourceType type: types) {
            if(type.getPlugin() != null && type.getPlugin().getName().equals(plugin)) {
                pluginTypes.add(type);
            }
        }
        return pluginTypes;
    }
    
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}