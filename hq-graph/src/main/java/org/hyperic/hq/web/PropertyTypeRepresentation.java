package org.hyperic.hq.web;

import org.hyperic.hq.plugin.domain.PropertyType;

public class PropertyTypeRepresentation {
	private Long id;
	private String name;
	private String description;
	private String defaultValue;
	private Boolean optional;
	private Boolean secret;
	
	public PropertyTypeRepresentation() {}
	
	public PropertyTypeRepresentation(PropertyType propertyType) {
		this.id = propertyType.getId();
		this.name = propertyType.getName();
		this.description = propertyType.getDescription();
		this.defaultValue = propertyType.getDefaultValue();
		this.optional = propertyType.getOptional();
		this.secret = propertyType.getSecret();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Boolean getOptional() {
		return optional;
	}

	public void setOptional(Boolean optional) {
		this.optional = optional;
	}

	public Boolean getSecret() {
		return secret;
	}

	public void setSecret(Boolean secret) {
		this.secret = secret;
	}
}