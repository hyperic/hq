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
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.product.Plugin;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.core.Direction;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceType {

    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    private String name;

    private String description;

    @OneToMany(cascade = CascadeType.ALL,mappedBy="resourceType")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<OperationType> operationTypes = new HashSet<OperationType>();

    @OneToMany(cascade = CascadeType.ALL,mappedBy="resourceType")
    private Set<PropertyType> propertyTypes = new HashSet<PropertyType>();
    
    @ManyToOne
    private Plugin plugin;

    @OneToMany(cascade = CascadeType.REMOVE,mappedBy="type")
    private Set<Resource> resources =  new HashSet<Resource>();

    @Version
    @Column(name = "version")
    private Integer version;
    
    @OneToMany(mappedBy="from")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<ResourceTypeRelationship> fromRelationships  = new HashSet<ResourceTypeRelationship>();
    
    @OneToMany(mappedBy="to")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<ResourceTypeRelationship> toRelationships  = new HashSet<ResourceTypeRelationship>();

    public ResourceType() {
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

    public Set<ResourceTypeRelationship> getRelationships(ResourceType entity, String name,
                                                            Direction direction) {
        if(direction == Direction.INCOMING) {
            return getRelationshipsFrom(entity,name);
        }
        if(direction == Direction.OUTGOING) {
            return getRelationshipsTo(entity,name);
        }
        Set<ResourceTypeRelationship> relationships =  getRelationshipsFrom(entity,name);
        relationships.addAll(getRelationshipsTo(entity, name));
        return relationships;
    }
    
    private Set<ResourceTypeRelationship> getRelationshipsTo(ResourceType entity, String name) {
        Set<ResourceTypeRelationship> relationships = new HashSet<ResourceTypeRelationship>();
        for(ResourceTypeRelationship relationship: fromRelationships) {
            if(relationship.getName().equals(name) && relationship.getFrom().equals(entity)) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }
    
    private Set<ResourceTypeRelationship> getRelationshipsFrom(ResourceType entity, String name) {
        Set<ResourceTypeRelationship> relationships = new HashSet<ResourceTypeRelationship>();
        for(ResourceTypeRelationship relationship: toRelationships) {
            if(relationship.getName().equals(name) && relationship.getTo().equals(entity)) {
                relationships.add(relationship);
            }
        } 
        return relationships;
    }

    public boolean isRelatedTo(ResourceType entity, String name) {
        //TODO other directions besides outgoing?
        for(ResourceTypeRelationship relationship: fromRelationships) {
            if(relationship.getName().equals(name) && relationship.getTo().equals(entity)) {
                return true;
            }
        } 
        return false;
    }

    
    @Transactional
    public ResourceTypeRelationship relateTo(ResourceType entity, String relationName) {
        ResourceTypeRelationship relationship = new ResourceTypeRelationship();
        relationship.setFrom(this);
        relationship.setFromId(this.getId());
        relationship.setTo(entity);
        relationship.setToId(entity.getId());
        relationship.setName(relationName);
        entityManager.persist(relationship);
        this.fromRelationships.add(relationship);
        entity.toRelationships.add(relationship);
        return relationship;
    }

    @Transactional
    public void removeRelationships(ResourceType entity, String name,
                                    Direction direction) {
        //TODO remove from collections?
        for (ResourceTypeRelationship relation : getRelationships(entity, name, direction)) {
            if (this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
            //TODO can't remove relationship by ID.  would it ever be detached?
            //else {
                //ResourceRelationship attached = this.entityManager.find(ResourceRelationship.class, relation.getId());
                //this.entityManager.remove(attached);
            //}
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

    public Set<ResourceTypeRelationship> getRelationships() {
        return getRelationships(null, null, Direction.BOTH);
    }

    public Set<ResourceTypeRelationship> getRelationshipsFrom(String relationName) {
        return getRelationships(null, relationName, Direction.OUTGOING);
    }

    public Set<ResourceTypeRelationship> getRelationshipsTo(String relationName) {
        return getRelationships(null, relationName, Direction.INCOMING);
    }

    public ResourceTypeRelationship getRelationshipTo(ResourceType entity, String relationName) {
        Set<ResourceTypeRelationship> relations = getRelationships(entity, relationName, null);
        ResourceTypeRelationship result = null;
        Iterator<ResourceTypeRelationship> i = relations.iterator();

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
    public void remove() {
        removeResources();
        removePropertyTypes();
        removeOperationTypes();
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
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        for(ResourceTypeRelationship fromRelationship: fromRelationships) {
            if(fromRelationship.getName().equals(relationName)) {
                resourceTypes.add(fromRelationship.getTo());
            }
        }
        return resourceTypes;
    }

    public Set<ResourceType> getResourceTypesTo(String relationName) {
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        for(ResourceTypeRelationship toRelationship: toRelationships) {
            if(toRelationship.getName().equals(relationName)) {
                resourceTypes.add(toRelationship.getFrom());
            }
        }
        return resourceTypes;
    }

    public ResourceType getResourceTypeFrom(String relationName) {
        Set<ResourceType> resourceTypes = getResourceTypesFrom(relationName);
        if (resourceTypes.isEmpty()) {
            return null;
        }
        // TODO validate only one
        return resourceTypes.iterator().next();
    }

    public ResourceType getResourceTypeTo(String relationName) {
        Set<ResourceType> resourceTypes = getResourceTypesTo(relationName);
        if (resourceTypes.isEmpty()) {
            return null;
        }
        // TODO validate only one
        return resourceTypes.iterator().next();
    }

    public void setPropertyTypes(Set<PropertyType> propertyTypes) {
        this.propertyTypes = propertyTypes;
    }
    
    public void addPropertyType(PropertyType propertyType) {
        propertyTypes.add(propertyType);
        propertyType.setResourceType(this);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ResourceType)) {
            return false;
        }
        return this.getId() == ((ResourceType) obj).getId();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceType[ ");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ").append("]");
        return sb.toString();
    }
}