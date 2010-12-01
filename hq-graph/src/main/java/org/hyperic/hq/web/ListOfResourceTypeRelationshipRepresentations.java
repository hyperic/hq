package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.inventory.domain.ResourceTypeRelation;
import org.springframework.web.util.UriTemplate;

public class ListOfResourceTypeRelationshipRepresentations {
	private List<ResourceTypeRelationshipRepresentation> resourceTypeRelations = new ArrayList<ResourceTypeRelationshipRepresentation>();
	private UriTemplate uri;
	
	public ListOfResourceTypeRelationshipRepresentations(Long id, List<ResourceTypeRelation> resourceTypeRelations, String baseUri) {
		if (resourceTypeRelations != null) {
			for (ResourceTypeRelation rtr : resourceTypeRelations) {
				this.resourceTypeRelations.add(new ResourceTypeRelationshipRepresentation(rtr, baseUri + "/{toId}"));
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