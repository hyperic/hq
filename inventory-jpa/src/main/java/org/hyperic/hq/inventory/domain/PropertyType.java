package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
public class PropertyType {
   
    private String defaultValue;

    //TODO validation not working in Neo4j
    //@NotNull
    private String description;

    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    private String name;

    private Boolean optional;

    @ManyToOne
    private ResourceType resourceType;

    
    private Boolean secret;
    
  
    private Boolean hidden;

    @Version
    @Column(name = "version")
    private Integer version;
    
    private Class<?> type;

    public PropertyType() {

    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Boolean isOptional() {
        //TODO proper way to do default value?
       if(this.optional == null) {
           return false;
       }
       return this.optional;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Boolean isSecret() {
        if(this.secret == null) {
            return false;
        }
        return this.secret;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public PropertyType merge() {
        PropertyType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void remove() {
        //TODO verify removed from resource type
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            PropertyType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setSecret(Boolean secret) {
        this.secret = secret;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public boolean isHidden() {
        if(this.hidden == null) {
            return false;
        }
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
    
    public boolean isIndexed() {
        //TODO add indexed?
        return false;
    }

    public void setIndexed(boolean indexed) {
        //TODO add indexed?
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("ResourceType: ").append(getResourceType()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        sb.append("Hidden: ").append(isHidden());
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PropertyType)) {
            return false;
        }
        return this.getId() == ((PropertyType) obj).getId();
    }
}
