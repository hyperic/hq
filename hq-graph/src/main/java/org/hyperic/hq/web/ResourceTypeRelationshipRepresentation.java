package org.hyperic.hq.web;

import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.inventory.domain.ResourceTypeRelation;
import org.springframework.web.util.UriTemplate;

public class ResourceTypeRelationshipRepresentation extends ResourceTypeRepresentation {
	private String relationshipUri;
	private String relationshipName;
	
	public ResourceTypeRelationshipRepresentation(ResourceTypeRelation relationship, String baseUri) {
		this(relationship.getFrom().getId(), relationship.getTo(), relationship.getName(), baseUri);
	}
	
	public ResourceTypeRelationshipRepresentation(Long id, ResourceType resourceType, String relationshipName, String baseUri) {
		super(resourceType, "/resourcetypes");
		
		this.relationshipUri = new UriTemplate(baseUri).expand(resourceType.getId()).toASCIIString();
		this.relationshipName = relationshipName;
	}
	
	public String getRelationshipName() {
		return relationshipName;
	}

	public String getRelationshipUri() {
		return relationshipUri;
	}
}
