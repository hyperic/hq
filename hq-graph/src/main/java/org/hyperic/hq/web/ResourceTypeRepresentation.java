package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ResourceTypeRepresentation {
	private Long id;
	private String name;
	private List<PropertyTypeRepresentation> propertyTypes = new ArrayList<PropertyTypeRepresentation>();
	private List<FormRepresentation> forms = new ArrayList<FormRepresentation>();
	private String uri;
	
	public ResourceTypeRepresentation() {}
	
	public ResourceTypeRepresentation(ResourceType resourceType, String baseUri) {
		this.id = resourceType.getId();
		this.name = resourceType.getName();
		this.uri = new UriTemplate(baseUri + "/{id}").expand(this.id).toASCIIString();
		
		for (PropertyType pt : resourceType.getPropertyTypes()) {
			this.propertyTypes.add(new PropertyTypeRepresentation(pt));
		}
		
		FormRepresentation form = new FormRepresentation("put", this.uri);
		
		form.addInput("text", Boolean.TRUE, "name", "");
		
		FramesetRepresentation frameset = form.addFrameset("propertyTypes");
		
		frameset.addInput("hidden", Boolean.TRUE, "id", "");
		frameset.addInput("text", Boolean.TRUE, "name", "");
		frameset.addInput("text", Boolean.FALSE, "description", "");
		frameset.addInput("text", Boolean.FALSE, "defaultValue", "");
		frameset.addInput("checkbox", Boolean.TRUE, "optional", "");
		frameset.addInput("checkbox", Boolean.TRUE, "secret", "");
		
		forms.add(form);
		
		form = new FormRepresentation("delete", this.uri);
		
		forms.add(form);
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

	public List<PropertyTypeRepresentation> getPropertyTypes() {
		return propertyTypes;
	}

	public void setPropertyTypes(List<PropertyTypeRepresentation> propertyTypes) {
		this.propertyTypes = propertyTypes;
	}

	public List<FormRepresentation> getForms() {
		return forms;
	}

	public String getUri() {
		return uri;
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