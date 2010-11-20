package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.plugin.domain.PropertyType;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ResourceTypeRepresentation {
	private Long id;
	private String name;
	private List<Map<String, Object>> propertyTypes = new ArrayList<Map<String, Object>>();
	private UriTemplate uri;
	
	public ResourceTypeRepresentation() {}
	
	public ResourceTypeRepresentation(ResourceType resourceType, String baseUri) {
		this.id = resourceType.getId();
		this.name = resourceType.getName();
		this.uri = new UriTemplate(baseUri + "/{id}");
		
		for (PropertyType pt : resourceType.getPropertyTypes()) {
			Map<String, Object> item = new HashMap<String, Object>();
			
			item.put("id", pt.getId());
			item.put("name", pt.getName());
			item.put("description", pt.getDescription());
			item.put("defaultValue", pt.getDefaultValue());
			item.put("optional", pt.getOptional());
			item.put("secret", pt.getSecret());
			item.put("name", pt.getName());

			this.propertyTypes.add(item);
		}
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

	public List<Map<String, Object>> getPropertyTypes() {
		return propertyTypes;
	}

	public void setPropertyTypes(List<Map<String, Object>> propertyTypes) {
		this.propertyTypes = propertyTypes;
	}

	public String getUri() {
		return (uri != null) ? uri.expand(id).toASCIIString() : null;
	}
	
	public ResourceType toDomain() {
		ResourceType resourceType;
		
		if (id == null) {
			resourceType = new ResourceType();
		} else {
			resourceType = ResourceType.findResourceType(id);
		}
		
		resourceType.setName(name);

		return resourceType;
	}
}