package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.springframework.web.util.UriTemplate;

public class ListOfResourceRelationshipRepresentations {
	private List<ResourceRelationshipRepresentation> resourceRelations = new ArrayList<ResourceRelationshipRepresentation>();
	private UriTemplate uri;
	
	public ListOfResourceRelationshipRepresentations(Long id, Set<ResourceRelation> resourceRelations, String baseUri) {
		if (resourceRelations != null) {
			for (ResourceRelation rr : resourceRelations) {
				this.resourceRelations.add(new ResourceRelationshipRepresentation(rr, baseUri + "/{toId}"));
			}
		}
		
		this.uri = new UriTemplate(baseUri);
	}

	public List<ResourceRelationshipRepresentation> getResources() {
		return resourceRelations;
	}
	
	public String getUri() {
		return (uri != null) ? uri.expand().toASCIIString() : null;
	}
}