package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;

/**
 * Represents a property that can be set against Resources of the
 * associated ResourceType
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity
public class PropertyType {
   
    private String defaultValue;

    @NotNull
    private String description;
    
    @GraphId
    private Integer id;

    @NotNull
    private String name;

    private boolean optional;

    private boolean secret;
    
    private boolean hidden;
       
    private boolean indexed;
    
    //TODO use type?  Had to in JPA impl
    private Class<?> type;

    public PropertyType() {

    }
    
    public PropertyType(String name,String description) {
        this.description = description;
        this.name = name;
    }
    
    public PropertyType(String name,Class<?> type) {
        this.name=name;
        this.type=type;
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

    public boolean isOptional() {
       return this.optional;
    }

    

    public boolean isSecret() {
        return this.secret;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
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

    public void setOptional(boolean optional) {
        this.optional = optional;
    }


    public void setSecret(boolean secret) {
        this.secret = secret;
    }
 
    public boolean isHidden() {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        sb.append("Hidden: ").append(isHidden()).append("]");
        return sb.toString();
    }
}
