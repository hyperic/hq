package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.InvalidRelationshipException;
import org.hyperic.hq.reference.ConfigType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.Direction;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Resource {
    
    @ManyToOne
    private Agent agent;

   
    private String description;
    
    @Autowired
    private transient ConversionService conversionService;

    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

  
    private String location;

   
    private String modifiedBy;

    @NotNull
    private String name;

    @ManyToOne
    private AuthzSubject owner;
 
    @ManyToOne
    private ResourceType type;

    @Version
    @Column(name = "version")
    private Integer version;
    
    @OneToMany(mappedBy="from")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<ResourceRelationship> fromRelationships  = new HashSet<ResourceRelationship>();
    
    @OneToMany(mappedBy="to")
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<ResourceRelationship> toRelationships= new HashSet<ResourceRelationship>();
    
    @OneToMany(cascade=CascadeType.REMOVE)
    private Set<ResourceProperty> properties= new HashSet<ResourceProperty>();
    
    @OneToMany(cascade=CascadeType.REMOVE)
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<Config> configs= new HashSet<Config>();

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
        for(Config config: configs) {
            //TODO Enum vs String for Config.type?
            if(type.toString().equals(config.getType())) {
                return config;
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
        Map<String, Object> props = new HashMap<String, Object>();
        for (ResourceProperty property: properties) {
            props.put(property.getName(), property.getValue());
        }
        return props;
    }
    
    public void removeProperties() {
        for(ResourceProperty property: properties) {
            property.remove();
        }
        properties.clear();
    }

    public Object getProperty(String key) {
        PropertyType propertyType = type.getPropertyType(key);
        if (propertyType == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        Object value = getProperties().get(key);
        
        if(value == null) {
            return conversionService.convert(propertyType.getDefaultValue(), propertyType.getType());
        }
        return conversionService.convert(value, propertyType.getType());
    }

    
    public Set<ResourceRelationship> getRelationships(Resource entity, String name, Direction direction) {
        if(direction == Direction.INCOMING) {
            return getRelationshipsFrom(entity,name);
        }
        if(direction == Direction.OUTGOING) {
            return getRelationshipsTo(entity,name);
        }
        Set<ResourceRelationship> relationships =  getRelationshipsFrom(entity,name);
        relationships.addAll(getRelationshipsTo(entity, name));
        return relationships;
    }
    
    private Set<ResourceRelationship> getRelationshipsTo(Resource entity, String name) {
        Set<ResourceRelationship> relationships = new HashSet<ResourceRelationship>();
        for(ResourceRelationship relationship: fromRelationships) {
            if(relationship.getName().equals(name) && relationship.getFrom().equals(entity)) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }
    
    private Set<ResourceRelationship> getRelationshipsFrom(Resource entity, String name) {
        Set<ResourceRelationship> relationships = new HashSet<ResourceRelationship>();
        for(ResourceRelationship relationship: toRelationships) {
            if(relationship.getName().equals(name) && relationship.getTo().equals(entity)) {
                relationships.add(relationship);
            }
        } 
        return relationships;
    }
     
    public boolean isRelatedTo(Resource resource, String relationName) {
       //TODO other directions besides outgoing?
        for(ResourceRelationship relationship: fromRelationships) {
            if(relationship.getName().equals(name) && relationship.getTo().equals(resource)) {
                return true;
            }
        } 
        return false;
    }

    @Transactional
    public ResourceRelationship relateTo(Resource resource, String relationName) {
        if (!(RelationshipTypes.CONTAINS.equals(relationName)) && !type.isRelatedTo(resource.getType(), relationName)) {
            throw new InvalidRelationshipException();
        }
        ResourceRelationship relationship = new ResourceRelationship();
        relationship.setFrom(this);
        relationship.setFromId(this.getId());
        relationship.setTo(resource);
        relationship.setToId(resource.getId());
        relationship.setName(relationName);
        this.entityManager.persist(relationship);
        this.fromRelationships.add(relationship);
        resource.toRelationships.add(relationship);
        return relationship;
    }
    
    @Transactional
    public void removeRelationships(Resource entity, String name, Direction direction) {
        //TODO remove from collections?
        for (ResourceRelationship relation : getRelationships(entity, name, direction)) {
            if (this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
            //TODO can't remove relationship by ID.  would it ever be detached?
            //TODO verify remove properties
            //else {
                //ResourceRelationship attached = this.entityManager.find(ResourceRelationship.class, relation.getId());
                //this.entityManager.remove(attached);
            //}
        }
    }

    public void removeRelationship(Resource resource, String relationName) {
        if (isRelatedTo(resource, relationName)) {
            removeRelationships(resource, relationName, Direction.BOTH);
        }
    }

    public void removeRelationships() {
        for(ResourceRelationship relation: this.toRelationships) {
            if (this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
        }
        for(ResourceRelationship relation: this.fromRelationships) {
            if (this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
        }
        this.toRelationships.clear();
        this.fromRelationships.clear();
    }

    public void removeRelationships(String relationName) {
        //TODO remove from collections?
        for(ResourceRelationship relation: this.toRelationships) {
            if (relation.getName().equals(relationName) && this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
        }
        for(ResourceRelationship relation: this.fromRelationships) {
            if (relation.getName().equals(relationName) && this.entityManager.contains(relation)) {
                this.entityManager.remove(relation);
            } 
        }
    }

    public Set<ResourceRelationship> getRelationships() {
        Set<ResourceRelationship> allRelations = new HashSet<ResourceRelationship>();
        allRelations.addAll(toRelationships);
        allRelations.addAll(fromRelationships);
        return allRelations;
    }

    public Set<ResourceRelationship> getRelationshipsFrom(String relationName) {
        Set<ResourceRelationship> relations = new HashSet<ResourceRelationship>();
        for(ResourceRelationship relation: this.fromRelationships) {
            if(relation.getName().equals(relationName)) {
                relations.add(relation);
            }
        }
        return relations;
    }

    public Set<ResourceRelationship> getRelationshipsTo(String relationName) {
        Set<ResourceRelationship> relations = new HashSet<ResourceRelationship>();
        for(ResourceRelationship relation: this.toRelationships) {
            if(relation.getName().equals(relationName)) {
                relations.add(relation);
            }
        }
        return relations;
    }

    public Set<Resource> getResourcesFrom(String relationName) {
        Set<Resource> resources = new HashSet<Resource>();
        for(ResourceRelationship fromRelationship: fromRelationships) {
            if(fromRelationship.getName().equals(relationName)) {
                resources.add(fromRelationship.getTo());
            }
        }
        return resources;
    }
    
    public Set<Resource> getResourcesTo(String relationName) {
        Set<Resource> resources = new HashSet<Resource>();
        for(ResourceRelationship toRelationship: toRelationships) {
            if(toRelationship.getName().equals(relationName)) {
                resources.add(toRelationship.getFrom());
            }
        }
        return resources;
    }
    
    public Set<Resource> getChildren(boolean recursive) {
        Set<Resource> children =getResourcesFrom(RelationshipTypes.CONTAINS);
        if(!recursive) {
            return children;
        }
        addChildren(this,children);
        return children;
    }
    
    private void addChildren(Resource parent, Set<Resource> children) {
        Set<Resource> firstChildren = parent.getChildren(false);
        for(Resource firstChild: firstChildren) {
            addChildren(firstChild,children);
        }
        children.addAll(firstChildren);
    }
    
    public Set<Integer> getChildrenIds(boolean recursive) {
        Set<Integer> children = new HashSet<Integer>();
        for(Resource child: getChildren(false)) {
            children.add(child.getId());
        }
        if(!(recursive)) {
            return children;
        }
        addChildrenIds(this,children);
        return children;
    }
    
    private void addChildrenIds(Resource parent, Set<Integer> children) {
        Set<Resource> firstChildren = parent.getChildren(false);
        for(Resource firstChild: firstChildren) {
            addChildrenIds(firstChild,children);
            children.add(firstChild.getId());
        }
    }
     
    public boolean hasChild(Resource resource,boolean recursive) {
        if(getChildren(recursive).contains(resource)) {
            return true;
        }
        return false;
    }
    
    public ResourceRelationship getRelationshipTo(Resource resource, String relationName) {
        //TODO more than one?
       Set<ResourceRelationship> relationships = getRelationshipsTo(resource, relationName);
       if(relationships.isEmpty()) {
           return null;
       }
       return relationships.iterator().next();
    }


    public Resource getResourceFrom(String relationName) {
        Set<Resource> resources = getResourcesFrom(relationName);
        if (resources.isEmpty()) {
            return null;
        }
        // TODO enforce only one?
        return resources.iterator().next();
    }

    public Resource getResourceTo(String relationName) {
        Set<Resource> resources = getResourcesTo(relationName);
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
        //TODO check cascade removes properties and config
        removeRelationships();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Resource attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    private void setConfig(Config config, ConfigType type) {
        config.setType(type.toString());
        config.merge();
        this.configs.add(config);
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

    public Object setProperty(String key, Object value) {
        if (type.getPropertyType(key) == null) {
            throw new IllegalArgumentException("Property " + key +
                                               " is not defined for resource of type " +
                                               type.getName());
        }
        if (value == null) {
            // TODO log a warning?
            return null;
        }
        // TODO check other stuff? Should def check optional param and maybe
        // disregard nulls, below throws Exception
        // with null values
        for(ResourceProperty property: properties) {
            if(property.getName().equals(key)) {
                Object oldValue = property.getValue();
                property.setValue(value.toString());
                property.merge();
                return oldValue;
            }
        }
        ResourceProperty property = new ResourceProperty();
        property.setName(key);
        property.setValue(value.toString());
        entityManager.persist(property);
        properties.add(property);
        return null;
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
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Resource)) {
            return false;
        }
        return this.getId() == ((Resource) obj).getId();
    }
}