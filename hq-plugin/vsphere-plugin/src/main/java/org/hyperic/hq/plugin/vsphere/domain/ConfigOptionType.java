package org.hyperic.hq.plugin.vsphere.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigOptionType {
	private Integer id;
	private String name;
	private String defaultValue;
	private String description;
	private Boolean optional;
	private Boolean hidden;
	private Boolean secret;
	
	public ConfigOptionType() {}
	
	public ConfigOptionType(String name, String description, String defaultValue, Boolean optional, Boolean hidden, Boolean secret) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.description = description;
		this.optional = optional;
		this.hidden = hidden;
		this.secret = secret;
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

	public Boolean getOptional() {
		return optional;
	}

	public void setOptional(Boolean optional) {
		this.optional = optional;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getSecret() {
		return secret;
	}

	public void setSecret(Boolean secret) {
		this.secret = secret;
	}
}

