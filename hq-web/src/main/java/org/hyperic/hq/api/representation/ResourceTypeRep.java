package org.hyperic.hq.api.representation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hyperic.hq.api.LinkHelper;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.util.Assert;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeRep implements SimpleRepresentation, LinkedRepresentation {	
	private Integer id;
	private String name;
	private String description;
	private String pluginName;
	private List<OperationTypeRep> operationTypes;
	private List<PropertyTypeRep> propertyTypes;
	private Map<String, String> links = new HashMap<String, String>();
	
	public ResourceTypeRep() {}
	
	public ResourceTypeRep(ResourceType type) {
		Assert.notNull(type, "Resource type argument can not be null");
		
		// Transfer domain object to representation...
		setId(type.getId());
		
		name = type.getName();
		description = type.getDescription();
		
		if (type.getPlugin() != null) {
			pluginName = type.getPlugin().getName();
		}
		
		if (type.getOperationTypes() != null) {
			operationTypes = new ArrayList<OperationTypeRep>();
			
			for (OperationType operationType : type.getOperationTypes()) {
				operationTypes.add(new OperationTypeRep(operationType));
			}
		}
		
		if (type.getPropertyTypes() != null) {
			propertyTypes = new ArrayList<PropertyTypeRep>();
			
			for (PropertyType propertyType : type.getPropertyTypes()) {
				propertyTypes.add(new PropertyTypeRep(propertyType));
			}
		}
		
		// Setup links...
		links.put(RELATIONSHIPS_LABEL, LinkHelper.getCollectionUri(RESOURCE_TYPES_LABEL, id, RELATIONSHIPS_LABEL));
		links.put(RESOURCES_LABEL, LinkHelper.getCollectionUri(RESOURCE_TYPES_LABEL, id, RESOURCES_LABEL));
		links.put(OPERATION_TYPES_LABEL, LinkHelper.getCollectionUri(RESOURCE_TYPES_LABEL, id, OPERATION_TYPES_LABEL));
		links.put(PROPERTY_TYPES_LABEL, LinkHelper.getCollectionUri(RESOURCE_TYPES_LABEL, id, PROPERTY_TYPES_LABEL));
		links.put(CONFIG_TYPES_LABEL, LinkHelper.getCollectionUri(RESOURCE_TYPES_LABEL, id, CONFIG_TYPES_LABEL));
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
		
		links.put(SELF_LABEL, LinkHelper.getInstanceByIdUri(RESOURCE_TYPES_LABEL, id));
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

	public List<OperationTypeRep> getOperationTypes() {
		return operationTypes;
	}

	public void setOperationTypes(List<OperationTypeRep> operationTypes) {
		this.operationTypes = operationTypes;
	}

	public List<PropertyTypeRep> getPropertyTypes() {
		return propertyTypes;
	}

	public void setPropertyTypes(List<PropertyTypeRep> propertyTypes) {
		this.propertyTypes = propertyTypes;
	}

	public Map<String, String> getLinks() {
		return links;
	}
}