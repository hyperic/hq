package org.hyperic.hq.api.representation;

import org.hyperic.hq.inventory.domain.PropertyType;

public class PropertyTypeRep {
    private Integer id;
    private String name;
    private Object defaultValue;
    private String description;
	private Boolean hidden;
	private Boolean secret;

    public PropertyTypeRep() {}
	
	public PropertyTypeRep(PropertyType propertyType) {
		id = propertyType.getId();
		name = propertyType.getName();
		defaultValue = propertyType.getDefaultValue();
		description = propertyType.getDescription();
		hidden = propertyType.isHidden();
		secret = propertyType.isSecret();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
}

