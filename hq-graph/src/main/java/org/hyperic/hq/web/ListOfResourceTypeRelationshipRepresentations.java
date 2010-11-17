package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.plugin.domain.ResourceType;
import org.springframework.web.util.UriTemplate;

public class ListOfResourceTypeRelationshipRepresentations {
	private List<ResourceTypeRelationshipRepresentation> resourceTypeRelations = new ArrayList<ResourceTypeRelationshipRepresentation>();
	private UriTemplate uri;
	
	public ListOfResourceTypeRelationshipRepresentations(Long id, List<ResourceType> resourceTypes, String baseUri) {
		if (resourceTypes != null) {
			for (ResourceType rt : resourceTypes) {
				this.resourceTypeRelations.add(new ResourceTypeRelationshipRepresentation(id, rt, baseUri + "/{toId}"));
			}
		}
		
		this.uri = new UriTemplate(baseUri);
	}

	public List<ResourceTypeRelationshipRepresentation> getResourceTypes() {
		return resourceTypeRelations;
	}
	
	public String getUri() {
		return (uri != null) ? uri.expand().toASCIIString() : null;
	}
}