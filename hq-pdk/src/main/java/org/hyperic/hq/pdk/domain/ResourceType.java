package org.hyperic.hq.pdk.domain;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceType {
	private Integer id;
	private String name;
	private String description;
	private String pluginName;
	private Set<PropertyType> propertyTypes;
	private Set<ConfigOptionType> configOptionTypes;
	private Set<OperationType> operationTypes;
	
	public ResourceType() {}
	
	public ResourceType(String name, String description, String pluginName, Set<PropertyType> propertyTypes, Set<OperationType> operationTypes, Set<ConfigOptionType> configOptionTypes) {
		this.name = name;
		this.description = description;
		this.pluginName = pluginName;
		this.propertyTypes = propertyTypes;
		this.operationTypes = operationTypes;
		this.configOptionTypes = configOptionTypes;
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
	
	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public Set<PropertyType> getPropertyTypes() {
		return propertyTypes;
	}
	
	public void setPropertyTypes(Set<PropertyType> propertyTypes) {
		this.propertyTypes = propertyTypes;
	}
	
	public Set<ConfigOptionType> getConfigOptionTypes() {
		return configOptionTypes;
	}
	
	public void setConfigOptionTypes(Set<ConfigOptionType> configOptionTypes) {
		this.configOptionTypes = configOptionTypes;
	}
	
	public Set<OperationType> getOperationTypes() {
		return operationTypes;
	}
	
	public void setOperationTypes(Set<OperationType> operationTypes) {
		this.operationTypes = operationTypes;
	}
}

