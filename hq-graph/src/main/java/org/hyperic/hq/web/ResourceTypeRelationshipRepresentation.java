package org.hyperic.hq.web;

import org.hyperic.hq.plugin.domain.ResourceType;
import org.hyperic.hq.plugin.domain.ResourceTypeRelation;
import org.springframework.web.util.UriTemplate;

public class ResourceTypeRelationshipRepresentation extends ResourceTypeRepresentation {
	private String relationshipUri;

	public ResourceTypeRelationshipRepresentation(ResourceTypeRelation relationship, String baseUri) {
		this(relationship.getFrom().getId(), relationship.getTo(), baseUri);
	}
	
	public ResourceTypeRelationshipRepresentation(Long id, ResourceType resourceType, String baseUri) {
		super(resourceType, "/resourcetypes");
		
		this.relationshipUri = new UriTemplate(baseUri).expand(id, resourceType.getId()).toASCIIString();
	}
	
	public String getRelationshipUri() {
		return relationshipUri;
	}
}
