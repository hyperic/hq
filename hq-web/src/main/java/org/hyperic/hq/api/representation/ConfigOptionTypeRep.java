package org.hyperic.hq.api.representation;

import org.hyperic.hq.inventory.domain.ConfigOptionType;

public class ConfigOptionTypeRep {
    private Integer id;
    private String name;
    private String defaultValue;
    private String description;
	private Boolean hidden;
	private Boolean secret;

	public ConfigOptionTypeRep() {}
	
	public ConfigOptionTypeRep(ConfigOptionType configType) {
		id = configType.getId();
		name = configType.getName();
		defaultValue = (String)configType.getDefaultValue();
		description = configType.getDescription();
		hidden = configType.isHidden();
		secret = configType.isSecret();
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

