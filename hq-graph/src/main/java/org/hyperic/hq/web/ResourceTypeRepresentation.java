package org.hyperic.hq.web;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.plugin.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ResourceTypeRepresentation {
	private Long id;
	private String name;
	private Set<ResourceRepresentation> resources = new HashSet<ResourceRepresentation>();
	private UriTemplate uri;
	
	public ResourceTypeRepresentation() {}
	
	public ResourceTypeRepresentation(ResourceType resourceType, String baseUri) {
		this.id = resourceType.getId();
		this.name = resourceType.getName();
		
		Set<Resource> resources = resourceType.getResources();
		
		if (resources != null) {
			/*
			for (Resource r : resources) {
				this.resources.add(new ResourceRepresentation(r, "/resources"));
			}
			*/
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
	
	public Set<ResourceRepresentation> getResources() {
		return resources;
	}

	public void setResources(Set<ResourceRepresentation> resources) {
		this.resources = resources;
	}

	public String getUri() {
		return (uri != null) ? uri.expand(id).toASCIIString() : null;
	}
	
	public ResourceType toDomain() {
		ResourceType resourceType = new ResourceType();

		resourceType.setId(id);
		resourceType.setName(name);

		return resourceType;
	}
}