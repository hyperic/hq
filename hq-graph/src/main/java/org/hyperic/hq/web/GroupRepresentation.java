package org.hyperic.hq.web;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.web.util.UriTemplate;

public class GroupRepresentation {
	private Long id;
	private String name;
	private Set<ResourceRepresentation> members = new HashSet<ResourceRepresentation>();
	private UriTemplate uri;
	
	public GroupRepresentation() {}
	
	public GroupRepresentation(ResourceGroup group, String baseUri) {
		this.id = group.getId();
		this.name = group.getName();
		
		for (Resource r : group.getMembers()) {
			this.members.add(new ResourceRepresentation(r, "/resources"));
		}
		
		this.uri = new UriTemplate(baseUri);
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

	public Set<ResourceRepresentation> getMembers() {
		return members;
	}

	public void setMembers(Set<ResourceRepresentation> members) {
		this.members = members;
	}

	public String getUri() {
		return (uri != null) ? uri.expand(id).toASCIIString() : null;
	}
	
	public ResourceGroup toDomain() {
		ResourceGroup group;
		
		if (id == null) {
			group = new ResourceGroup();
		} else {
			group = ResourceGroup.findResourceGroup(id);
		}
		
		group.setName(name);

		return group;
	}
}
