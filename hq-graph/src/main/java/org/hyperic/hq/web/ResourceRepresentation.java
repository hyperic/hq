package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.inventory.domain.ResourceTypeRelation;
import org.springframework.web.util.UriTemplate;

public class ResourceRepresentation {
	private Long id;
	private String name;
	private String type;
	private String uri;
	private Map<String, Object> properties = new HashMap<String, Object>();
	private List<FormRepresentation> forms = new ArrayList<FormRepresentation>();
	private List<ResourceTypeRepresentation> relatedResourceTypes = new ArrayList<ResourceTypeRepresentation>();
	
	public ResourceRepresentation() {}
	
	public ResourceRepresentation(Resource resource, String baseUri) {
		this.id = resource.getId();
		this.name = resource.getName();
		this.uri = new UriTemplate(baseUri + "/{id}").expand(resource.getId()).toASCIIString();
		
		if (!resource.getProperties().isEmpty()) {
			this.properties = resource.getProperties();
		}		

		FormRepresentation form = new FormRepresentation("put", this.uri);
		
		form.addInput("text", Boolean.TRUE, "name", "");
		
		if (resource.getType() != null) {
			this.type = resource.getType().getName();
			
			if (resource.getType().getPropertyTypes() != null) {
				FramesetRepresentation frameset = form.addFrameset("properties");
				
				for (PropertyType propertyType : resource.getType().getPropertyTypes()) {
					String type = "text";
					Boolean required = Boolean.FALSE;
					frameset.addInput(type, required, propertyType.getName(), propertyType.getDefaultValue());
				}
			}
			
			for (ResourceTypeRelation rtr : resource.getType().getRelationships()) {
				this.relatedResourceTypes.add(new ResourceTypeRepresentation(rtr.getTo(), "/resourcetypes"));
			}
		}
		
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public List<FormRepresentation> getForms() {
		return forms;
	}

	public List<ResourceTypeRepresentation> getRelatedResourceTypes() {
		return relatedResourceTypes;
	}

	public String getUri() {
		return uri;
	}
	
	public Resource toDomain() {
		Resource resource;
		
		if (id == null) {
			resource = new Resource();
		} else {
			resource = Resource.findResource(id);
			
			for (Map.Entry<String, Object> p : properties.entrySet()) {
				resource.setProperty(p.getKey(), p.getValue());
			}
		}
		
		resource.setName(name);
		
		if (type != null) {
			resource.setType(ResourceType.findResourceTypeByName(type));
		}
		
		return resource;
	}
}
