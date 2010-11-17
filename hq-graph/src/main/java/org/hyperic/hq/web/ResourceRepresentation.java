package org.hyperic.hq.web;

import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.web.util.UriTemplate;

public class ResourceRepresentation {
	private Long id;
	private String name;
	private ResourceTypeRepresentation resourceType;
	private UriTemplate uri;
	
	public ResourceRepresentation() {}
	
	public ResourceRepresentation(Resource resource, String baseUri) {
		this.id = resource.getId();
		this.name = resource.getName();
		this.resourceType = new ResourceTypeRepresentation(resource.getType(), "/resourcetypes");
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

	public ResourceTypeRepresentation getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceTypeRepresentation resourceType) {
		this.resourceType = resourceType;
	}

	public String getUri() {
		return (uri != null) ? uri.expand(id).toASCIIString() : null;
	}
	
	public Resource toDomain() {
		Resource resource = new Resource();
		
		resource.setId(id);
		resource.setName(name);
		resource.setType(resourceType.toDomain());
		
		return resource;
	}
}
