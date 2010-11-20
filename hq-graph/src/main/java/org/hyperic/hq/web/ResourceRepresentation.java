package org.hyperic.hq.web;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ResourceRepresentation {
	private Long id;
	private String name;
	private ResourceTypeRepresentation resourceType;
	private String resourceTypeName;
	private Map<String, Object> properties = new HashMap<String, Object>();
	private UriTemplate uri;
	
	public ResourceRepresentation() {}
	
	public ResourceRepresentation(Resource resource, String baseUri) {
		this.id = resource.getId();
		this.name = resource.getName();
		
		if (resource.getType() != null) {
			this.resourceType = new ResourceTypeRepresentation(resource.getType(), "/resourcetypes");
			this.resourceTypeName = this.resourceType.getName();
		}
		
		if (!resource.getProperties().isEmpty()) {
			this.properties = resource.getProperties();
		}
		
		this.uri = new UriTemplate(baseUri + "/{id}");
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

	public String getResourceTypeName() {
		return resourceTypeName;
	}

	public void setResourceTypeName(String resourceTypeName) {
		this.resourceTypeName = resourceTypeName;
	}

	public ResourceTypeRepresentation getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceTypeRepresentation resourceType) {
		this.resourceType = resourceType;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getUri() {
		return (uri != null) ? uri.expand(id).toASCIIString() : null;
	}
	
	public Resource toDomain() {
		Resource resource;
		
		if (id == null) {
			resource = new Resource();
		} else {
			resource = Resource.findResource(id);
		}
		
		resource.setName(name);
		
		if (resourceTypeName != null) {
			resource.setType(ResourceType.findResourceTypeByName(resourceTypeName));
		} else {
			resource.setType(resourceType.toDomain());
		}
		
		for (Map.Entry<String, Object> p : properties.entrySet()) {
			resource.setProperty(p.getKey(), p.getValue());
		}
		
		return resource;
	}
}
