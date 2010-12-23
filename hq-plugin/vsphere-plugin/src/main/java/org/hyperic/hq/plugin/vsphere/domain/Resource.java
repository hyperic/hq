package org.hyperic.hq.plugin.vsphere.domain;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
	private Integer id;
	private String name;
	private String description;
	private String location;
	private String modifiedBy;
	private Map<String, Object> properties = new HashMap<String, Object>();
	private Map<String, Object> configs = new HashMap<String, Object>();
	private Agent agent;
	private ResourceType type;

	public Resource() {}
	
	public Resource(Integer id, String name, String description, String location, String modifiedBy, Map<String , Object> properties, Map<String, Object> configs, Agent agent, ResourceType type) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.location = location;
		this.modifiedBy = modifiedBy;
		this.properties = properties;
		this.configs = configs;
		this.agent = agent;
		this.type = type;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
	public Map<String, Object> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, Object> configs) {
		this.configs = configs;
	}
	
	public Agent getAgent() {
		return agent;
	}

	public void setAgent(Agent agent) {
		this.agent = agent;
	}

	public ResourceType getType() {
		return type;
	}

	public void setType(ResourceType type) {
		this.type = type;
	}
}