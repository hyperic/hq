package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.List;

public class PropertyType implements IdentityAware, PersistenceAware<PropertyType> {
    private String defaultValue;
    private String description;
    private Integer id;
    private String name;
    private Boolean optional;
    private ResourceType resourceType;
    private Boolean secret;
    private Integer version;

    public PropertyType() {
    }
    
    public void flush() {
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

    public Boolean getOptional() {
        return this.optional;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Boolean getSecret() {
        return this.secret;
    }

    public Integer getVersion() {
        return this.version;
    }

    public PropertyType merge() {
    	return this;
    }

    public void persist() {
    }

    public void remove() {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("ResourceType: ").append(getResourceType()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(getOptional()).append(", ");
        sb.append("Secret: ").append(getSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        return sb.toString();
    }

    public static int count() {
    	return 0;
    }
    
    public static List<PropertyType> findAllPropertyTypes() {
    	return new ArrayList<PropertyType>();
    }

    public static PropertyType findPropertyType(Integer id) {
    	return new PropertyType();
    }

    public static List<PropertyType> find(Integer firstResult, Integer maxResults) {
    	return new ArrayList<PropertyType>();
    }
}