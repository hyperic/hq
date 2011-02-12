package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.hyperic.hq.reference.RelationshipTypes;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar() See ConfigOptionTag for the
 * supported value types for Config. May need custom Converter to make some of
 * them graph properties
 * @author administrator
 * 
 */
@Configurable
@NodeEntity
public class ConfigOptionType {
    
    @GraphId
    private Integer id;

    @NotNull
    private String name;
    
    private String defaultValue;
    
    private String description;
        
    private boolean optional;
        
    private boolean hidden;
   
    private boolean secret;
    
    @RelatedTo(type = RelationshipTypes.HAS_CONFIG_OPT_TYPE, direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    public ConfigOptionType() {
    }
    
    public ConfigOptionType(String name) {
        this.name=name;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
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
        sb.append("ResourceType: ").append(getResourceType()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Hidden: ").append(isHidden()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        return sb.toString();
    }
}
