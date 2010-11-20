package org.hyperic.hq.web;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceRelation;
import org.springframework.web.util.UriTemplate;

public class ResourceRelationshipRepresentation extends ResourceRepresentation {
	private String relationshipUri;
	private String relationshipName;
	
	public ResourceRelationshipRepresentation(ResourceRelation relationship, String baseUri) {
		this(relationship.getFrom().getId(), relationship.getTo(), relationship.getName(), baseUri);
	}
	
	public ResourceRelationshipRepresentation(Long id, Resource resource, String relationshipName, String baseUri) {
		super(resource, "/resources");
		
		this.relationshipUri = new UriTemplate(baseUri).expand(resource.getId()).toASCIIString();
		this.relationshipName = relationshipName;
	}
	
	public String getRelationshipUri() {
		return relationshipUri;
	}
	
	public String getRelationshipName() {
		return relationshipName;
	}
}
