package org.hyperic.hq.api.form;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hyperic.hq.api.representation.ConfigOptionTypeRep;
import org.hyperic.hq.api.representation.OperationTypeRep;
import org.hyperic.hq.api.representation.PropertyTypeRep;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeForm {
	private Integer id;
	private String name;
	private String description;
	private String pluginName;
	private Set<PropertyTypeRep> propertyTypes;
	private Set<ConfigOptionTypeRep> configOptionTypes;
	private Set<OperationTypeRep> operationTypes;
	
	
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

	public Set<PropertyTypeRep> getPropertyTypes() {
		return propertyTypes;
	}

	public void setPropertyTypes(Set<PropertyTypeRep> propertyTypes) {
		this.propertyTypes = propertyTypes;
	}

	public Set<ConfigOptionTypeRep> getConfigOptionTypes() {
		return configOptionTypes;
	}

	public void setConfigOptionTypes(Set<ConfigOptionTypeRep> configOptionTypes) {
		this.configOptionTypes = configOptionTypes;
	}

	public Set<OperationTypeRep> getOperationTypes() {
		return operationTypes;
	}

	public void setOperationTypes(Set<OperationTypeRep> operationTypes) {
		this.operationTypes = operationTypes;
	}
}