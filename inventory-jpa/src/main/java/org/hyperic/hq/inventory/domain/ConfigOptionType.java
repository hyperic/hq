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

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar() See ConfigOptionTag for the
 * supported value types for Config. May need custom Converter to make some of
 * them graph properties
 * @author administrator
 * 
 */
@Entity
@Configurable
public class ConfigOptionType  {
    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    private String name;
    
   
   
    private String defaultValue;
    
   
    
    private String description;
        
    
  
    private Boolean optional;
        
   
    
    private Boolean hidden;
        
    
   
    private Boolean secret;

    @Version
    @Column(name = "version")
    private Integer version;
    
    @ManyToOne
    private ResourceType resourceType;

    public ConfigOptionType() {
    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public ConfigOptionType merge() {
        ConfigOptionType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void remove() {
        //TODO verify removed from ResourceType
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ConfigOptionType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
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
    
    

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public Boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean isSecret() {
        return secret;
    }

    public void setSecret(Boolean secret) {
        this.secret = secret;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("ResourceType: ").append(getResourceType()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Hidden: ").append(isHidden()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ConfigOptionType)) {
            return false;
        }
        return this.getId() == ((ConfigOptionType) obj).getId();
    }
}