package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ListOfResourceTypeRepresentations {
	private List<ResourceTypeRepresentation> resourceTypes = new ArrayList<ResourceTypeRepresentation>();
	private UriTemplate uri;
	
	public ListOfResourceTypeRepresentations(List<ResourceType> resourceTypes, String baseUri) {
		if (resourceTypes != null) {
			for (ResourceType rt : resourceTypes) {
				this.resourceTypes.add(new ResourceTypeRepresentation(rt, "/resourcetypes"));
			}
		}
		
		this.uri = new UriTemplate(baseUri);
	}

	public List<ResourceTypeRepresentation> getResourceTypes() {
		return resourceTypes;
	}
	
	public String getUri() {
		return (uri != null) ? uri.expand().toASCIIString() : null;
	}
}